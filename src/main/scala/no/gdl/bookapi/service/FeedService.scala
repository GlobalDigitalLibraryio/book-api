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
import no.gdl.bookapi.BookApiProperties.{OpdsLanguageParam, OpdsLevelParam}
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.{Facet, FeedCategory, FeedEntry, SearchResult}
import no.gdl.bookapi.model.domain.{PublishingStatus, Paging, Sort}
import no.gdl.bookapi.repository.{FeedRepository, TranslationRepository}

import scala.util.Try

trait FeedService {
  this: FeedRepository with TranslationRepository with ReadService with ConverterService with FeedLocalizationService =>
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
      val localization = feedLocalizationService.localizationFor(language)

      // TODO Do something else here? Do we have to read this from the database?
      feedRepository.forUrl(url.replace(BookApiProperties.OpdsPath,"")).map(feedDefinition => {
        val levelRegex = ".*/level(\\d+).xml".r
        val (title, description) = feedDefinition.titleKey match {
          case "opds_root_title" => (localization.rootTitle, None)
          case "opds_nav_title" => (localization.navTitle, None)
          case "level_feed_title" => url match {
            case levelRegex(level) => (localization.levelTitle(level), Some(localization.levelDescription))
            case _ => ("TODO", Some(localization.levelDescription))
          }
        }

        api.Feed(
          feedDefinition = api.FeedDefinition(
            feedDefinition.id.get,
            feedDefinition.revision.get,
            s"${BookApiProperties.CloudFrontOpds}${feedDefinition.url}",
            feedDefinition.uuid),
          title = title,
          description = description,
          rel = Some("self"),
          updated = updated,
          content = books,
          facets = facets)
      })
    }

    def facetsForLanguages(currentLanguage: LanguageTag): Seq[Facet] = {
      readService.listAvailableLanguagesAsLanguageTags.sortBy(_.toString).map(lang => Facet(
        href = s"${
          BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsRootUrl.url
          .replace(BookApiProperties.OpdsLanguageParam, lang.toString)}",
        title = s"${lang.displayName}",
        group = "Languages",
        isActive = lang == currentLanguage))
    }

    def facetsForSelections(currentLanguage: LanguageTag, url: String): Seq[Facet] = {
      val localization = feedLocalizationService.localizationFor(currentLanguage)
      val group = "Selection"
      (Facet(
        href = s"${
          BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsRootUrl.url
          .replace(BookApiProperties.OpdsLanguageParam, currentLanguage.toString)}",
        title = s"New arrivals",
        group = group,
        isActive = url.endsWith("root.xml"))
        +:
        readService.listAvailableLevelsForLanguage(Some(currentLanguage))
          .sortBy(level => Try(level.toInt).getOrElse(0)).map(readingLevel =>
          Facet(
            href = s"${
              BookApiProperties.CloudFrontOpds}${BookApiProperties.OpdsLevelUrl.url
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
            url = s"${BookApiProperties.CloudFrontOpds}${definition.url}",
            uuid = definition.uuid),
          title = localization.navTitle,
          description = None,
          rel = Some("http://opds-spec.org/sort/new"),
          updated = justArrivedUpdated,
          content = Seq.empty,
          facets = Seq.empty))


      val levels: Seq[api.Feed] = readService.listAvailableLevelsForLanguage(Some(language))
        .flatMap(level => {
          val url = levelPath(language, level)
          val levelUpdated = translationRepository.latestArrivalDateFor(language, level)

          feedRepository.forUrl(url).map(definition =>
            api.Feed(
              api.FeedDefinition(
                id = definition.id.get,
                revision = definition.revision.get,
                url = s"${BookApiProperties.CloudFrontOpds}${definition.url}",
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
      val searchResult =
        readService.withLanguageAndStatus(
        language = languageTag,
        status = PublishingStatus.PUBLISHED,
        pageSize = paging.pageSize,
        page = paging.page,
        sort = Sort.ByArrivalDateDesc)

      (searchResultToPagingStatus(searchResult, paging), searchResult.results.map(book => api.FeedEntry(book)))
    }

    def newEntries(lang: LanguageTag): Seq[FeedEntry] = {
      val searchResult = readService.withLanguage(
        lang, BookApiProperties.OpdsJustArrivedLimit, 1, Sort.ByArrivalDateDesc)
      searchResult.results.map(FeedEntry(_))
    }

    def entriesForLanguageAndLevel(language: LanguageTag, level: String, paging: Paging): (PagingStatus, Seq[FeedEntry]) = {
      val searchResult =
        readService.withLanguageAndLevelAndStatus(
        language = language,
        readingLevel = Some(level),
        pageSize = paging.pageSize,
        page = paging.page,
        sort = Sort.ByTitleAsc,
        status = PublishingStatus.PUBLISHED
      )
      (searchResultToPagingStatus(searchResult, paging), searchResult.results.map(book => api.FeedEntry(book)))
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
    def rootPath(language: LanguageTag): String = s"${BookApiProperties.OpdsRootUrl.url}"
      .replace(BookApiProperties.OpdsLanguageParam, language.toString)

    // TODO Issue#200: Remove when not used anymore
    def levelPath(language: LanguageTag, level: String): String = s"${BookApiProperties.OpdsLevelUrl.url}"
      .replace(BookApiProperties.OpdsLanguageParam, language.toString)
      .replace(BookApiProperties.OpdsLevelParam, level)

    def generateFeeds(): Seq[api.FeedDefinition] = {
      val existing = feedRepository.all()
      val toCreate = calculateFeeds.filterNot(x => existing.map(_.url).toSet.contains(x.url))

      val all = existing ++ toCreate.map(createFeed)
      all.map(feed => api.FeedDefinition(feed.id.get, feed.revision.get, feed.url, feed.uuid))
    }

    def calculateFeeds: Seq[domain.Feed] = {
      val languages = translationRepository.allAvailableLanguages()
      val languageAndLevel = languages.flatMap(lang => {
        translationRepository.allAvailableLevels(Some(lang)).map(level => (lang, level))
      })

      BookApiProperties.OpdsFeeds.flatMap { feedDefinition =>
        val url = feedDefinition.url

        val containsLanguage = url.contains(OpdsLanguageParam)
        val containsLevel = url.contains(OpdsLevelParam)

        val urls = (containsLanguage, containsLevel) match {
          case (true, true) => languageAndLevel.map(langLevel => url.replace(OpdsLanguageParam, langLevel._1.toString).replace(OpdsLevelParam, langLevel._2))
          case (true, _) => languages.map(lang => url.replace(OpdsLanguageParam, lang.toString))
          case (_, _) => Seq(url)
        }

        urls.map(url => domain.Feed(None, None, url, UUID.randomUUID().toString, feedDefinition.titleKey, feedDefinition.descriptionKey))
      }
    }

    def createFeed(feed: domain.Feed): domain.Feed = {
      feedRepository.add(feed)
    }
  }
}
