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
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.OpdsLanguageParam
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.domain.{Paging, PublishingStatus, Sort}
import io.digitallibrary.bookapi.repository.{BookRepository, FeedRepository, TranslationRepository}
import io.digitallibrary.bookapi.service.search.SearchService
import io.digitallibrary.language.model.LanguageTag

trait FeedService {
  this: FeedRepository with TranslationRepository with BookRepository with ReadService with ConverterService with SearchService with FeedLocalizationService =>
  val feedService: FeedService

  sealed trait PagingStatus
  case class OnlyOnePage(currentPaging: Paging) extends PagingStatus
  case class MoreAhead(currentPaging: Paging, lastPage: Int) extends PagingStatus
  case class MoreBefore(currentPaging: Paging) extends PagingStatus
  case class MoreInBothDirections(currentPaging: Paging, lastPage: Int) extends PagingStatus

  sealed trait FeedType
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
      readService.listAvailablePublishedCategoriesForLanguage(currentLanguage).keys.toList.sortWith(categorySort).map(category => Facet(
        href = s"${BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsCategoryUrl
          .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
          .replace(BookApiProperties.OpdsCategoryParam, category.name)}",
        title = localization.categoryTitle(category.name),
        group = "Category",
        isActive = currentCategory.contains(category.name)
      )
      )
    }

    def categorySort(c1: domain.Category, c2: domain.Category): Boolean = {
      if (c1.name == "library_books") {
        true
      } else {
        c1.name.compareTo(c2.name) < 0
      }
    }

    def levelOrder(level: String): Int = {
      level match {
        case "decodable" => 0
        case "read-aloud" => 100
        case "root" => 101
        case _ => level.toInt
      }
    }

    def facetsForReadingLevels(currentLanguage: LanguageTag, currentCategory: String, currentReadingLevel: Option[String]): Seq[Facet] = {
      val localization = feedLocalizationService.localizationFor(currentLanguage)
      val group = "Selection"
      readService.listAvailablePublishedLevelsForLanguage(Some(currentLanguage), Some(currentCategory))
        .sortBy(levelOrder).map(readingLevel =>
        Facet(
          href = s"${
            BookApiProperties.CloudFrontOpds
          }${
            BookApiProperties.OpdsCategoryAndLevelUrl
              .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
              .replace(BookApiProperties.OpdsCategoryParam, currentCategory)
              .replace(BookApiProperties.OpdsLevelParam, readingLevel)
          }",
          title = localization.levelTitle(readingLevel),
          group = group,
          isActive = currentReadingLevel.contains(readingLevel))
      ) ++
      Seq(Facet(
        href = s"${
          BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsCategoryUrl
          .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
          .replace(BookApiProperties.OpdsCategoryParam, currentCategory)
        }",
        title = s"New arrivals",
        group = group,
        isActive = currentReadingLevel.isEmpty))
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

      (searchResultToPagingStatus(searchResult, paging), searchResultsToFeedEntries(searchResult.results, language))
    }

    def searchResultsToFeedEntries(bookHits: Seq[BookHit], language: LanguageTag): Seq[FeedEntry] = {
       for {
        bookHit <- bookHits
        book <- fromHit(bookHit, feedLocalizationService.localizationFor(language))
      } yield api.FeedEntry(book)
    }

    def fromHit(bookHit: BookHit, feedLocalization: FeedLocalization): Option[Book] = {
      for {
        translation <- unFlaggedTranslationsRepository.forBookIdAndLanguage(bookHit.id, LanguageTag(bookHit.language.code))
        book <- bookRepository.withId(bookHit.id)
        apiBook = converterService.toApiBook(translation, unFlaggedTranslationsRepository.languagesFor(bookHit.id), book)
        apiBookWithLocalizedReadingLevel = apiBook.copy(readingLevel = apiBook.readingLevel.map(feedLocalization.levelTitle))
      } yield apiBookWithLocalizedReadingLevel
    }

    def entriesForLanguageAndCategory(language: LanguageTag, category: String, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {
      val searchResult = searchService.searchWithCategoryAndLevel(
        languageTag = language,
        category = Some(category),
        readingLevel = None,
        source = None,
        paging = paging,
        sort = Sort.ByTitleAsc)

      (searchResultToPagingStatus(searchResult, paging), searchResultsToFeedEntries(searchResult.results, language))
    }

    def entriesForLanguageCategoryAndLevel(language: LanguageTag, category: String, level: String, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {
      val searchResult = searchService.searchWithCategoryAndLevel(
        languageTag = language,
        category = Some(category),
        readingLevel = Some(level),
        source = None,
        paging = paging,
        sort = Sort.ByTitleAsc)

      (searchResultToPagingStatus(searchResult, paging), searchResultsToFeedEntries(searchResult.results, language))
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

    def generateFeeds(): Seq[api.FeedDefinition] = {
      val feeds = calculateFeeds.map(createOrUpdateFeed)
      feeds.map(feed => api.FeedDefinition(feed.id.get, feed.revision.get, feed.url, feed.uuid))
    }

    def calculateFeeds: Seq[domain.Feed] = {
      val feeds = for {
        language <- unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)
        localization = feedLocalizationService.localizationFor(language)
        (category: domain.Category, levels: Set[String]) <- unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
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
