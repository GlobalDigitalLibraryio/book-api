/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import java.time.LocalDate
import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.BookApiProperties.OpdsLanguageParam
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api._
import no.gdl.bookapi.model.domain.{Paging, PublishingStatus, Sort}
import no.gdl.bookapi.repository.{BookRepository, FeedRepository, TranslationRepository}
import no.gdl.bookapi.service.search.SearchService

import scala.util.Try

trait FeedService {
  this: FeedRepository with TranslationRepository with BookRepository with ReadService with ConverterService with SearchService with FeedLocalizationService =>
  val feedService: FeedService

  sealed trait PagingStatus
  case class OnlyOnePage(currentPaging: Paging) extends PagingStatus
  case class MoreAhead(currentPaging: Paging, lastPage: Int) extends PagingStatus
  case class MoreBefore(currentPaging: Paging) extends PagingStatus
  case class MoreInBothDirections(currentPaging: Paging, lastPage: Int) extends PagingStatus

  class FeedService extends LazyLogging {
    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    def feedForUrl(url: String, language: LanguageTag, feedUpdated: Option[LocalDate], titleArgs: Seq[String], books: => Seq[FeedEntry]): Option[api.Feed] = {
      val updated = feedUpdated match {
        case Some(x) => x
        case None => books.sortBy(_.book.dateArrived).reverse.headOption.map(_.book.dateArrived).getOrElse(LocalDate.now())
      }

      val facets = facetsForLanguages(language) ++ facetsForSelections(language, url)

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
          updated = updated,
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

    def facetsForSelections(currentLanguage: LanguageTag, url: String): Seq[Facet] = {
      val localization = feedLocalizationService.localizationFor(currentLanguage)
      val group = "Selection"
      (Facet(
        href = s"${
          BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsRootUrl
          .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)}",
        title = s"New arrivals",
        group = group,
        isActive = url.endsWith("root.xml"))
        +:
        readService.listAvailablePublishedLevelsForLanguage(Some(currentLanguage))
          .sortBy(level => Try(level.toInt).getOrElse(0)).map(readingLevel =>
          Facet(
            href = s"${
              BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsLevelUrl
              .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)
              .replace(BookApiProperties.OpdsLevelParam, readingLevel)}",
            title = localization.levelTitle(readingLevel),
            group = group,
            isActive = url.endsWith(s"level$readingLevel.xml"))
        ))
    }

    // TODO Issue#200: Remove when not used anymore
    def feedsForNavigation(language: LanguageTag): Seq[api.Feed] = {
      val localization = feedLocalizationService.localizationFor(language)

      val justArrivedUpdated = translationRepository.latestArrivalDateFor(language)
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
          updated = justArrivedUpdated,
          content = Seq.empty,
          facets = Seq.empty))


      val levels: Seq[api.Feed] = readService.listAvailablePublishedLevelsForLanguage(Some(language))
        .flatMap(level => {
          val url = levelPath(language, level)
          val levelUpdated = translationRepository.latestArrivalDateFor(language, level)

          feedRepository.forUrl(url).map(definition =>
            api.Feed(
              api.FeedDefinition(
                id = definition.id.get,
                revision = definition.revision.get,
                url = s"${BookApiProperties.CloudFrontOpds}${definition.url}?page-size=100",
                uuid = definition.uuid),
              title = localization.levelTitle(level),
              description = Some(localization.levelDescription),
              rel = None,
              updated = levelUpdated,
              content = Seq.empty,
              facets = Seq.empty))
        })

      Seq(justArrived).flatten ++ levels
    }

    def allEntries(language: LanguageTag, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {

      val searchResult = {
        searchService.searchWithLevel(languageTag = language, None, paging = paging, sort = Sort.ByArrivalDateDesc)
      }

      (searchResultToPagingStatus(searchResult, paging), searchResult.results.map(book => api.FeedEntry(fromHit(book))))
    }

    def fromHit(bookHit: BookHit): Book = {
      val availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(bookHit.id)
      val translation = translationRepository.forBookIdAndLanguage(bookHit.id, LanguageTag(bookHit.language.code))
      val book = bookRepository.withId(bookHit.id)
      converterService.toApiBook(translation, availableLanguages, book).get
    }

    def entriesForLanguageAndLevel(language: LanguageTag, level: String, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {
      val searchResult = searchService.searchWithLevel(languageTag = language, readingLevel = Some(level), paging = paging, sort = Sort.ByTitleAsc)
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
    def levelPath(language: LanguageTag, level: String): String = s"${BookApiProperties.OpdsLevelUrl}"
      .replace(BookApiProperties.OpdsLanguageParam, language.toString)
      .replace(BookApiProperties.OpdsLevelParam, level)

    def generateFeeds(): Seq[api.FeedDefinition] = {
      val feeds = calculateFeeds.map(createOrUpdateFeed)
      feeds.map(feed => api.FeedDefinition(feed.id.get, feed.revision.get, feed.url, feed.uuid))
    }

    def calculateFeeds: Seq[domain.Feed] = {
      val feeds = for {
        language <- translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)
        level <- translationRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, Some(language))
        localization = feedLocalizationService.localizationFor(language)
        feed <- Seq(
          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsRootUrl.replace(OpdsLanguageParam, language.toString),
            uuid = UUID.randomUUID().toString,
            title = localization.rootTitle,
            description = None),

          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsNavUrl.replace(OpdsLanguageParam, language.toString),
            uuid = UUID.randomUUID().toString,
            title = localization.navTitle,
            description = None),

          domain.Feed(
            id = None,
            revision = None,
            url = BookApiProperties.OpdsLevelUrl
              .replace(OpdsLanguageParam, language.toString)
              .replace(BookApiProperties.OpdsLevelParam, level),
            uuid = UUID.randomUUID().toString,
            title = localization.levelTitle(level),
            description = Some(localization.levelDescription)
          ))

      } yield feed
      val defaultRootFeed =
        domain.Feed(
          id = None,
          revision = None,
          url = BookApiProperties.OpdsRootDefaultLanguageUrl,
          uuid = UUID.randomUUID().toString,
          title = feedLocalizationService.localizationFor(LanguageTag("eng")).rootTitle,
          description = None)
      defaultRootFeed +: feeds
    }

    def createOrUpdateFeed(feed: domain.Feed): domain.Feed = {
      feedRepository.addOrUpdate(feed)
    }
  }
}
