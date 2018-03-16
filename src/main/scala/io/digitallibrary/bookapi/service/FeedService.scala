/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.OpdsLanguageParam
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.domain.{Paging, PublishingStatus, Sort}
import io.digitallibrary.bookapi.repository.{BookRepository, FeedRepository, TranslationRepository}
import io.digitallibrary.bookapi.service.search.SearchService

import scala.util.Try

trait FeedService {
  this: FeedRepository with TranslationRepository with BookRepository with ReadService with ConverterService with SearchService with FeedLocalizationService =>
  val feedService: FeedService

  sealed trait PagingStatus
  case class OnlyOnePage(currentPaging: Paging) extends PagingStatus
  case class MoreAhead(currentPaging: Paging, lastPage: Int) extends PagingStatus
  case class MoreBefore(currentPaging: Paging) extends PagingStatus
  case class MoreInBothDirections(currentPaging: Paging, lastPage: Int) extends PagingStatus

  sealed trait FeedType
  case object NavigationFeed extends FeedType
  case class RootFeed(language: LanguageTag) extends FeedType
  case class CategoryFeed(language: LanguageTag, category: String) extends FeedType
  case class LevelFeed(language: LanguageTag, category: String, level: String) extends FeedType


  class FeedService extends LazyLogging {
    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    def feedForUrl(url: String, feedType: FeedType, books: => Seq[FeedEntry]): Option[api.Feed] = {

      val facets = feedType match {
        case RootFeed(language) =>
          facetsForLanguages(language) ++
            facetsForCategories(language, currentCategory = None)
        case CategoryFeed(language, category) =>
          facetsForLanguages(language) ++
            facetsForCategories(language, Some(category)) ++
            facetsForReadingLevels(currentLanguage = language, currentCategory = category, None)
        case LevelFeed(language, category, level) =>
          facetsForLanguages(language) ++
            facetsForCategories(language, Some(category)) ++
            facetsForReadingLevels(currentLanguage = language, currentCategory = category, Some(level))
        case NavigationFeed => Seq.empty
      }

      feedRepository.forUrl(url.replace(BookApiProperties.OpdsPath,"")).map(feedDefinition => {
        api.Feed(
          feedDefinition = api.FeedDefinition(
            feedDefinition.id.get,
            feedDefinition.revision.get,
            s"${BookApiProperties.CloudFrontOpds}${feedDefinition.url}",
            feedDefinition.uuid),
          title = feedDefinition.title,
          description = feedDefinition.description,
          rel = Some("self"),
          updated = feedDefinition.updated,
          content = books,
          facets = facets)
      })
    }

    def facetsForLanguages(currentLanguage: LanguageTag): Seq[Facet] = {
      readService.listAvailablePublishedLanguagesAsLanguageTags.sortBy(_.toString).map(lang => Facet(
        href = s"${
          BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsRootUrl
          .replace(BookApiProperties.OpdsLanguageParam, lang.toString)}",
        title = s"${lang.localDisplayName.getOrElse(lang.displayName)}",
        group = "Languages",
        isActive = lang == currentLanguage))
    }

    def facetsForCategories(currentLanguage: LanguageTag, currentCategory: Option[String]): Seq[Facet] = {
      val localization = feedLocalizationService.localizationFor(currentLanguage)
      readService.listAvailablePublishedCategoriesForLanguage(currentLanguage).keys.toList.map(category => Facet(
        href = s"${BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsCategoryUrl
          .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
          .replace(BookApiProperties.OpdsCategoryParam, category.name)}",
        title = localization.categoryTitle(category.name),
        group = "Category",
        isActive = currentCategory.contains(category.name)
      )
      )
    }

    def facetsForReadingLevels(currentLanguage: LanguageTag, currentCategory: String, currentReadingLevel: Option[String]): Seq[Facet] = {
      val localization = feedLocalizationService.localizationFor(currentLanguage)
      val group = "Selection"
      (Facet(
        href = s"${
          BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsCategoryUrl
          .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
          .replace(BookApiProperties.OpdsCategoryParam, currentCategory)
        }",
        title = s"New arrivals",
        group = group,
        isActive = currentReadingLevel.isEmpty)
        +:
        readService.listAvailablePublishedLevelsForLanguage(Some(currentLanguage))
          .sortBy(level => Try(level.toInt).getOrElse(0)).map(readingLevel =>
          Facet(
            href = s"${
              BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsCategoryAndLevelUrl
              .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
              .replace(BookApiProperties.OpdsCategoryParam, currentCategory)
              .replace(BookApiProperties.OpdsLevelParam, readingLevel)}",
            title = localization.levelTitle(readingLevel),
            group = group,
            isActive = currentReadingLevel.contains(readingLevel))
        ))
    }

    // TODO Issue#200: Remove when not used anymore
    def feedsForNavigation(language: LanguageTag): Seq[api.Feed] = {
      val localization = feedLocalizationService.localizationFor(language)

      val justArrived = feedRepository.forUrl(rootPath(language)).map(definition =>
        api.Feed(
          api.FeedDefinition(
            id = definition.id.get,
            revision = definition.revision.get,
            url = s"${BookApiProperties.CloudFrontOpds}${definition.url}?page-size=100",
            uuid = definition.uuid),
          title = localization.navTitle,
          description = None,
          rel = Some("http://opds-spec.org/sort/new"),
          updated = definition.updated,
          content = Seq.empty,
          facets = Seq.empty))

      val categoriesAndLevels = for {
        (category: domain.Category, levels: Set[String]) <- translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
        level <- levels
        url = levelPath(language, category.name, level)
      } yield feedRepository.forUrl(url).map(definition =>
          api.Feed(
            api.FeedDefinition(
              id = definition.id.get,
              revision = definition.revision.get,
              url = s"${BookApiProperties.CloudFrontOpds}${definition.url}?page-size=100",
              uuid = definition.uuid),
            title = localization.levelTitle(level),
            description = Some(localization.levelDescription),
            rel = None,
            updated = definition.updated,
            content = Seq.empty,
            facets = Seq.empty))

      Seq(justArrived).flatten ++ categoriesAndLevels.flatten
    }

    def allEntries(language: LanguageTag, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {

      val searchResult = {
        searchService.searchWithCategoryAndLevel(
          languageTag = language,
          category = None,
          readingLevel = None,
          source = None,
          paging = paging,
          sort = Sort.ByArrivalDateDesc)
      }

      (searchResultToPagingStatus(searchResult, paging), searchResult.results.map(book => api.FeedEntry(fromHit(book))))
    }

    def fromHit(bookHit: BookHit): Book = {
      val book = for {
        translation <- translationRepository.forBookIdAndLanguage(bookHit.id, LanguageTag(bookHit.language.code))
        book <- bookRepository.withId(bookHit.id)
      } yield converterService.toApiBook(translation, translationRepository.languagesFor(bookHit.id), book)

      book.get
    }

    def entriesForLanguageAndCategory(language: LanguageTag, category: String, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {
      val searchResult = searchService.searchWithCategoryAndLevel(
        languageTag = language,
        category = Some(category),
        readingLevel = None,
        source = None,
        paging = paging,
        sort = Sort.ByTitleAsc)

      (searchResultToPagingStatus(searchResult, paging), searchResult.results.map(book => api.FeedEntry(fromHit(book))))
    }

    def entriesForLanguageCategoryAndLevel(language: LanguageTag, category: String, level: String, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {
      val searchResult = searchService.searchWithCategoryAndLevel(
        languageTag = language,
        category = Some(category),
        readingLevel = Some(level),
        source = None,
        paging = paging,
        sort = Sort.ByTitleAsc)

      (searchResultToPagingStatus(searchResult, paging), searchResult.results.map(book => api.FeedEntry(fromHit(book))))
    }

    def searchResultToPagingStatus(searchResult: SearchResult, paging: Paging): PagingStatus = {
      if (searchResult.totalCount > paging.pageSize) {
        val lastPage = Math.round(Math.ceil(searchResult.totalCount.toFloat / paging.pageSize)).toInt
        if (paging.page == lastPage) {
          MoreBefore(currentPaging = paging)
        } else if (paging.page > 1) {
          MoreInBothDirections(currentPaging = paging, lastPage = lastPage)
        } else {
          MoreAhead(currentPaging = paging, lastPage = lastPage)
        }
      } else {
        OnlyOnePage(currentPaging = paging)
      }
    }

    // TODO Issue#200: Remove when not used anymore
    def rootPath(language: LanguageTag): String = s"${BookApiProperties.OpdsRootUrl}"
      .replace(BookApiProperties.OpdsLanguageParam, language.toString)

    // TODO Issue#200: Remove when not used anymore
    def levelPath(language: LanguageTag, category: String, level: String): String = s"${BookApiProperties.OpdsCategoryAndLevelUrl}"
      .replace(BookApiProperties.OpdsLanguageParam, language.toString)
      .replace(BookApiProperties.OpdsCategoryParam, category)
      .replace(BookApiProperties.OpdsLevelParam, level)

    def generateFeeds(): Seq[api.FeedDefinition] = {
      val feeds = calculateFeeds.map(createOrUpdateFeed)
      feeds.map(feed => api.FeedDefinition(feed.id.get, feed.revision.get, feed.url, feed.uuid))
    }

    def calculateFeeds: Seq[domain.Feed] = {
      val feeds = for {
        language <- translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)
        localization = feedLocalizationService.localizationFor(language)
        (category: domain.Category, levels: Set[String]) <- translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
        categoryFeed = domain.Feed(
          id = None,
          revision = None,
          url = BookApiProperties.OpdsCategoryUrl
            .replace(OpdsLanguageParam, language.toString)
            .replace(BookApiProperties.OpdsCategoryParam, category.name),
          uuid = UUID.randomUUID().toString,
          title = localization.categoriesTitle,
          description = Some(localization.categoriesDescription),
          updated = ZonedDateTime.now()
        )
        level <- levels
        feed <- Seq(
          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsRootUrl.replace(OpdsLanguageParam, language.toString),
            uuid = UUID.randomUUID().toString,
            title = localization.rootTitle,
            description = None,
            updated = ZonedDateTime.now()
          ),

          categoryFeed,

          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsNavUrl.replace(OpdsLanguageParam, language.toString),
            uuid = UUID.randomUUID().toString,
            title = localization.navTitle,
            description = None,
            updated = ZonedDateTime.now()
          ),

          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsCategoryAndLevelUrl
              .replace(OpdsLanguageParam, language.toString)
              .replace(BookApiProperties.OpdsCategoryParam, category.name)
              .replace(BookApiProperties.OpdsLevelParam, level),
            uuid = UUID.randomUUID().toString,
            title = localization.levelTitle(level),
            description = Some(localization.levelDescription),
            updated = ZonedDateTime.now()
          ))

      } yield feed
      val defaultRootFeed =
        domain.Feed(
          id = None,
          revision = None,
          url = BookApiProperties.OpdsRootDefaultLanguageUrl,
          uuid = UUID.randomUUID().toString,
          title = feedLocalizationService.localizationFor(LanguageTag("eng")).rootTitle,
          description = None,
          updated = ZonedDateTime.now()
        )
      defaultRootFeed +: feeds
    }

    def createOrUpdateFeed(feed: domain.Feed): domain.Feed = {
      feedRepository.addOrUpdate(feed)
    }
  }
}
