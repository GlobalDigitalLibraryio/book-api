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

    def newEntriesFor(lang: String): Seq[FeedEntry] = {
      readService.withLanguage(lang, BookApiProperties.OpdsJustArrivedLimit, 1, Sort.ByArrivalDateDesc).results.map(FeedEntry(_))
    }

    def editorsPickForLanguage(lang: String): Seq[FeedEntry] = {
      readService.editorsPickForLanguage(lang).map(_.books).getOrElse(Seq()).map(FeedEntry(_))
    }

    def feedEntriesForLanguage(language: String): Seq[FeedEntry] = {
      val allBooks = readService.withLanguage(
        language = language,
        pageSize = pageSize,
        page = page,
        sort = Sort.ByArrivalDateDesc
      ).results.map(book => addLevelCategory(FeedEntry(book), language))

      val justArrived = allBooks.take(BookApiProperties.OpdsJustArrivedLimit).map(addJustArrivedCategory(_, language))
      val editorsPick = readService.editorsPickForLanguage(language).map(_.books).getOrElse(Seq())
      val featuredBooks = allBooks.filter(entry => editorsPick.contains(entry.book)).map(addEditorsPickCategory(_, language))

      val rest = allBooks
        .filterNot(entry => justArrived.map(_.book).contains(entry.book))
        .filterNot(entry => featuredBooks.map(_.book).contains(entry.book))

      featuredBooks ++ justArrived ++ rest
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

    def feedForUrl(url: String, language: String, feedTitle: String)(getBooks: => Seq[FeedEntry]): Option[api.Feed] = {
      feedRepository.forUrl(url).map(feedDefinition => {
        api.Feed(
          api.FeedDefinition(
            feedDefinition.id.get,
            feedDefinition.revision.get,
            feedDefinition.url,
            feedDefinition.uuid),
          feedTitle,
          LocalDate.now(),
          getBooks)
      })
    }

    def addEditorsPickCategory(feedEntry: FeedEntry, language: String): FeedEntry = {
      val title = Messages("featured_feed_title")(Lang(language))
      val url = s"${BookApiProperties.Domain}${BookApiProperties.OpdsPath}${BookApiProperties.OpdsFeaturedUrl}"
        .replace(BookApiProperties.OpdsLanguageParam, language)

      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(url, title, sortOrder = 1))
    }

    def addJustArrivedCategory(feedEntry: FeedEntry, language: String): FeedEntry = {
      val title = Messages("new_entries_feed_title")(Lang(language))
      val url = s"${BookApiProperties.Domain}${BookApiProperties.OpdsPath}${BookApiProperties.OpdsNewUrl}"
        .replace(BookApiProperties.OpdsLanguageParam, language)

      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(url, title, sortOrder = 2))
    }

    def addLevelCategory(feedEntry: FeedEntry, language: String): FeedEntry = {
      val level = feedEntry.book.readingLevel.getOrElse(BookApiProperties.DefaultReadingLevel)

      val readingLevelCategoryTitle = s"${Messages("level_feed_title")(Lang(language))} $level"

      val readingLevelUrl = s"${BookApiProperties.Domain}${BookApiProperties.OpdsPath}${BookApiProperties.OpdsLevelUrl}"
        .replace(BookApiProperties.OpdsLanguageParam, language)
        .replace(BookApiProperties.OpdsLevelParam, level)

      feedEntry.copy(
        categories = feedEntry.categories :+ FeedCategory(readingLevelUrl, readingLevelCategoryTitle, sortOrder = level.toInt + 2))

    }

    def generateFeeds(): Seq[api.FeedDefinition] = {
      val existing = feedRepository.all()
      val toCreate = calculateFeedUrls.filterNot(existing.map(_.url).toSet)

      val all = existing ++ toCreate.map(createFeed)
      all.map(feed => api.FeedDefinition(feed.id.get, feed.revision.get, feed.url, feed.uuid))
    }

    def calculateFeedUrls: Seq[String] = {
      val languages = translationRepository.allAvailableLanguages()
      val languageAndLevel = languages.flatMap(lang => {
        translationRepository.allAvailableLevels(Some(lang)).map(level => (lang, level))
      })

      BookApiProperties.OpdsFeeds.flatMap { feedTemplate =>
        val url = s"${BookApiProperties.OpdsPath}$feedTemplate"

        val containsLanguage = url.contains(OpdsLanguageParam)
        val containsLevel = url.contains(OpdsLevelParam)

        (containsLanguage, containsLevel) match {
          case (true, true) => languageAndLevel.map(langLevel => url.replace(OpdsLanguageParam, langLevel._1).replace(OpdsLevelParam, langLevel._2))
          case (true, _) => languages.map(lang => url.replace(OpdsLanguageParam, lang))
          case (_, _) => Seq(url)
        }
      }
    }

    def createFeed(feedUrl: String): domain.Feed = {
      feedRepository.add(domain.Feed(None, None, feedUrl, UUID.randomUUID().toString))
    }
  }

}
