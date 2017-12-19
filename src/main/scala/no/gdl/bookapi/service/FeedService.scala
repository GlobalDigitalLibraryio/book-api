/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import java.time.LocalDate
import java.util.UUID

import com.osinka.i18n.{Lang, Messages}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.BookApiProperties.{OpdsLanguageParam, OpdsLevelParam}
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.{FeedCategory, FeedEntry}
import no.gdl.bookapi.model.domain.Sort
import no.gdl.bookapi.repository.{EditorsPickRepository, FeedRepository, TranslationRepository}

trait FeedService {
  this: FeedRepository with TranslationRepository with EditorsPickRepository with ReadService with ConverterService =>
  val feedService: FeedService

  class FeedService extends LazyLogging {
    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    val page = 1
    val pageSize = 10000 //TODO in #94: Create partial opds feed entries, to solve paging

    def feedForUrl(url: String, language: LanguageTag, feedUpdated: Option[LocalDate], titleArgs: Seq[String], books: => Seq[FeedEntry]): Option[api.Feed] = {
      val updated = feedUpdated match {
        case Some(x) => x
        case None => books.sortBy(_.book.dateArrived).reverse.headOption.map(_.book.dateArrived).getOrElse(LocalDate.now())
      }

      feedRepository.forUrl(url.replace(BookApiProperties.OpdsPath,"")).map(feedDefinition => {
        api.Feed(
          api.FeedDefinition(
            feedDefinition.id.get,
            feedDefinition.revision.get,
            s"${BookApiProperties.CloudFrontOpds}${feedDefinition.url}",
            feedDefinition.uuid),
          Messages(feedDefinition.titleKey, titleArgs:_*)(Lang(language.toString)),
          feedDefinition.descriptionKey.map(Messages(_)(Lang(language.toString))),
          Some("self"),
          updated,
          books)
      })
    }

    def feedsForNavigation(language: LanguageTag): Seq[api.Feed] = {
      implicit val lang: Lang = Lang(language.toString)

      val justArrivedUpdated = translationRepository.latestArrivalDateFor(language)
      val justArrived = feedRepository.forUrl(justArrivedPath(language)).map(definition =>
        api.Feed(
          api.FeedDefinition(
            definition.id.get,
            definition.revision.get,
            s"${BookApiProperties.CloudFrontOpds}${definition.url}",
            definition.uuid),
          Messages(definition.titleKey),
          definition.descriptionKey.map(Messages(_)),
          Some("http://opds-spec.org/sort/new"),
          justArrivedUpdated, Seq()))


      val featured = feedRepository.forUrl(featuredPath(language)).map(definition =>
        api.Feed(
          api.FeedDefinition(
            definition.id.get,
            definition.revision.get,
            s"${BookApiProperties.CloudFrontOpds}${definition.url}",
            definition.uuid),
          Messages(definition.titleKey),
          definition.descriptionKey.map(Messages(_)),
          Some("http://opds-spec.org/featured"),
          editorsPickLastUpdated(language).getOrElse(LocalDate.now()),
          Seq()))

      val levels: Seq[api.Feed] = readService.listAvailableLevelsForLanguage(Some(language))
        .flatMap(level => {
          val url = levelPath(language, level)
          val levelUpdated = translationRepository.latestArrivalDateFor(language, level)

          feedRepository.forUrl(url).map(definition =>
            api.Feed(
              api.FeedDefinition(
                definition.id.get,
                definition.revision.get,
                s"${BookApiProperties.CloudFrontOpds}${definition.url}",
                definition.uuid),
              Messages(definition.titleKey, level),
              definition.descriptionKey.map(Messages(_)),
              None,
              levelUpdated, Seq()))
        })

      Seq(featured, justArrived).flatten ++ levels
    }

    def allEntries(language: LanguageTag): Seq[FeedEntry] = {
      val featuredBooks = editorsPicks(language).map(addFeaturedCategory(_, language))
      val justArrived = newEntries(language).map(addJustArrivedCategory(_, language))

      val allBooks = readService.withLanguage(
        language = language,
        pageSize = pageSize,
        page = page,
        sort = Sort.ByArrivalDateDesc
      ).results.map(book => FeedEntry(book)).map(addLevelCategory(_, language)).sortBy(_.book.readingLevel)

      featuredBooks ++ justArrived ++ allBooks
    }

    def newEntries(lang: LanguageTag): Seq[FeedEntry] = readService.withLanguage(
      lang, BookApiProperties.OpdsJustArrivedLimit, 1, Sort.ByArrivalDateDesc).results.map(FeedEntry(_))

    def editorsPickLastUpdated(language: LanguageTag): Option[LocalDate] = editorsPickRepository.lastUpdatedEditorsPick(language)
    def editorsPicks(lang: LanguageTag): Seq[FeedEntry] = readService.editorsPickForLanguage(lang)
      .map(_.books).getOrElse(Seq()).map(FeedEntry(_))

    def entriesForLanguageAndLevel(language: LanguageTag, level: String): Seq[FeedEntry] = {
      readService.withLanguageAndLevel(
        language = language,
        readingLevel = Some(level),
        pageSize = pageSize,
        page = page,
        sort = Sort.ByTitleAsc
      ).results.map(book => api.FeedEntry(book))
    }

    def addFeaturedCategory(feedEntry: FeedEntry, language: LanguageTag): FeedEntry = {
      val title = Messages("featured_feed_title")(Lang(language.toString))
      val url = s"${BookApiProperties.CloudFrontOpds}${featuredPath(language)}"
      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(url, title, sortOrder = 1))
    }

    def addJustArrivedCategory(feedEntry: FeedEntry, language: LanguageTag): FeedEntry = {
      val title = Messages("new_entries_feed_title")(Lang(language.toString))
      val url = s"${BookApiProperties.CloudFrontOpds}${justArrivedPath(language)}"
      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(url, title, sortOrder = 2))
    }

    def addLevelCategory(feedEntry: FeedEntry, language: LanguageTag): FeedEntry = {
      val level = feedEntry.book.readingLevel.getOrElse(BookApiProperties.DefaultReadingLevel)
      val readingLevelCategoryTitle = Messages("level_feed_title", level)(Lang(language.toString))
      val readingLevelUrl = s"${BookApiProperties.CloudFrontOpds}${levelPath(language, level)}"
      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(readingLevelUrl, readingLevelCategoryTitle, sortOrder = level.toInt + 2))
    }

    def featuredPath(language: LanguageTag): String = s"${BookApiProperties.OpdsFeaturedUrl.url}"
      .replace(BookApiProperties.OpdsLanguageParam, language.toString)

    def justArrivedPath(language: LanguageTag): String = s"${BookApiProperties.OpdsNewUrl.url}"
      .replace(BookApiProperties.OpdsLanguageParam, language.toString)

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
