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
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.BookApiProperties.{OpdsLanguageParam, OpdsLevelParam}
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.{FeedCategory, FeedEntry}
import no.gdl.bookapi.model.domain.Sort
import no.gdl.bookapi.repository.{FeedRepository, TranslationRepository}

trait FeedService {
  this: FeedRepository with TranslationRepository with ReadService with ConverterService =>
  val feedService: FeedService

  class FeedService extends LazyLogging {

    val page = 1
    val pageSize = 10000 //TODO: Create partial opds feed entries, to solve paging

    def feedsForNavigation(language: String): Seq[api.Feed] = {
      implicit val lang: Lang = Lang(language)

      val justArrived = feedRepository.forUrl(justArrivedUrl(language)).map(definition =>
        api.Feed(
          api.FeedDefinition(
            definition.id.get,
            definition.revision.get,
            s"${BookApiProperties.Domain}${definition.url}",
            definition.uuid),
          Messages(definition.titleKey),
          definition.descriptionKey.map(Messages(_)),
          Some("http://opds-spec.org/sort/new"),
          LocalDate.now(), Seq()))

      val featured = feedRepository.forUrl(featuredUrl(language)).map(definition =>
        api.Feed(
          api.FeedDefinition(
            definition.id.get,
            definition.revision.get,
            s"${BookApiProperties.Domain}${definition.url}",
            definition.uuid),
          Messages(definition.titleKey),
          definition.descriptionKey.map(Messages(_)),
          Some("http://opds-spec.org/featured"),
          LocalDate.now(), Seq()))

      val levels: Seq[api.Feed] = readService.listAvailableLevelsForLanguage(Some(language))
        .flatMap(level => {
          val url = levelUrl(language, level)
          feedRepository.forUrl(url).map(definition =>
            api.Feed(
              api.FeedDefinition(
                definition.id.get,
                definition.revision.get,
                s"${BookApiProperties.Domain}${definition.url}",
                definition.uuid),
              Messages(definition.titleKey, level),
              definition.descriptionKey.map(Messages(_)),
              None,
              LocalDate.now(), Seq()))
        })

      Seq(featured, justArrived).flatten ++ levels
    }

    def fullListOfFeedEntries(language: String): Seq[FeedEntry] = {
      val featuredBooks = editorsPickForLanguage(language).map(addFeaturedCategory(_, language))

      val justArrived = newEntriesFor(language).map(addJustArrivedCategory(_, language))

      val allBooks = readService.withLanguage(
        language = language,
        pageSize = pageSize,
        page = page,
        sort = Sort.ByArrivalDateDesc
      ).results.map(book => FeedEntry(book)).map(addLevelCategory(_, language)).sortBy(_.book.readingLevel)


      featuredBooks ++ justArrived ++ allBooks
    }

    def newEntriesFor(lang: String): Seq[FeedEntry] = {
      readService.withLanguage(lang, BookApiProperties.OpdsJustArrivedLimit, 1, Sort.ByArrivalDateDesc).results.map(FeedEntry(_))
    }

    def editorsPickForLanguage(lang: String): Seq[FeedEntry] = {
      readService.editorsPickForLanguage(lang).map(_.books).getOrElse(Seq()).map(FeedEntry(_))
    }

    def feedEntriesForLanguageAndLevel(language: String, level: String): Seq[FeedEntry] = {
      readService.withLanguageAndLevel(
        language = language,
        readingLevel = Some(level),
        pageSize = pageSize,
        page = page,
        sort = Sort.ByTitleAsc
      ).results.map(book => api.FeedEntry(book))
    }

    def feedForUrl(url: String, language: String)(getBooks: => Seq[FeedEntry]): Option[api.Feed] = {
      feedRepository.forUrl(url).map(feedDefinition => {
        api.Feed(
          api.FeedDefinition(
            feedDefinition.id.get,
            feedDefinition.revision.get,
            feedDefinition.url,
            feedDefinition.uuid),
          Messages(feedDefinition.titleKey)(Lang(language)),
          feedDefinition.descriptionKey.map(Messages(_)(Lang(language))),
          Some("self"),
          LocalDate.now(), // TODO: Get correct updated url.
          getBooks)
      })
    }

    def addFeaturedCategory(feedEntry: FeedEntry, language: String): FeedEntry = {
      val title = Messages("featured_feed_title")(Lang(language))
      val url = s"${BookApiProperties.Domain}${featuredUrl(language)}"

      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(url, title, sortOrder = 1))
    }

    def featuredUrl(language: String): String = s"${BookApiProperties.OpdsPath}${BookApiProperties.OpdsFeaturedUrl.url}"
      .replace(BookApiProperties.OpdsLanguageParam, language)

    def addJustArrivedCategory(feedEntry: FeedEntry, language: String): FeedEntry = {
      val title = Messages("new_entries_feed_title")(Lang(language))
      val url = s"${BookApiProperties.Domain}${justArrivedUrl(language)}"

      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(url, title, sortOrder = 2))
    }

    def justArrivedUrl(language: String): String = s"${BookApiProperties.OpdsPath}${BookApiProperties.OpdsNewUrl.url}"
      .replace(BookApiProperties.OpdsLanguageParam, language)

    def addLevelCategory(feedEntry: FeedEntry, language: String): FeedEntry = {
      val level = feedEntry.book.readingLevel.getOrElse(BookApiProperties.DefaultReadingLevel)
      val readingLevelCategoryTitle = s"${Messages("level_feed_title", level)(Lang(language))}"
      val readingLevelUrl = s"${BookApiProperties.Domain}${levelUrl(language, level)}"

      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(readingLevelUrl, readingLevelCategoryTitle, sortOrder = level.toInt + 2))
    }

    def levelUrl(language: String, level: String): String = s"${BookApiProperties.OpdsPath}${BookApiProperties.OpdsLevelUrl.url}"
      .replace(BookApiProperties.OpdsLanguageParam, language)
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
        val url = s"${BookApiProperties.OpdsPath}${feedDefinition.url}"

        val containsLanguage = url.contains(OpdsLanguageParam)
        val containsLevel = url.contains(OpdsLevelParam)

        val urls = (containsLanguage, containsLevel) match {
          case (true, true) => languageAndLevel.map(langLevel => url.replace(OpdsLanguageParam, langLevel._1).replace(OpdsLevelParam, langLevel._2))
          case (true, _) => languages.map(lang => url.replace(OpdsLanguageParam, lang))
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
