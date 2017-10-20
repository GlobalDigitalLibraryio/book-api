/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}
import java.util.{Date, UUID}

import com.osinka.i18n.{Lang, Messages}
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.BookApiProperties.OpdsPath
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.{Feed, FeedEntry}
import no.gdl.bookapi.model.domain.Sort
import no.gdl.bookapi.service.{ConverterService, FeedService, ReadService}
import org.scalatra.NotFound

trait OPDSController {
  this: ReadService with ConverterService with FeedService =>
  val opdsController: OPDSController
  val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
  val dtf: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  class OPDSController extends GdlController {

    val page = 1
    val pageSize = 10000 //TODO: Create partial opds feed entries, to solve paging

    before() {
      contentType = formats("xml")
    }


    get(BookApiProperties.OpdsNavUrl) {
      val lang = language("lang")
      val feedName = s"/$lang/root.xml"

      val levelsForLanguage = readService.listAvailableLevelsForLanguage(Some(lang))

      if(levelsForLanguage.nonEmpty) {
        showNavigationRoot(getFeedId(feedName), language("lang"), levelsForLanguage)
      } else {
        contentType = "text/plain"
        NotFound(body = "No books available for language.")
      }
    }

    get(BookApiProperties.OpdsRootUrl) {
      val lang = language("lang")
      val feedTitle = Messages("default_feed_title")(Lang(lang))

      val feed = feedService.feedForUrl(request.getRequestURI, lang, feedTitle) {
        feedService.feedEntriesForLanguage(lang)
      }

      feed match {
        case Some(x) => showAcquisitionFeed(x)
        case None =>
          contentType = "text/plain"
          NotFound(body = "No books available for language.")
      }
    }

    get(BookApiProperties.OpdsNewUrl) {
      val lang = language("lang")
      val feedTitle = Messages("new_entries_feed_title")(Lang(lang))

      val feed = feedService.feedForUrl(request.getRequestURI, lang, feedTitle) {
        feedService.newEntriesFor(lang)
      }

      feed match {
        case Some(x) => showAcquisitionFeed(x)
        case None =>
          contentType = "text/plain"
          NotFound(body = "No books available for language.")
      }
    }

    get(BookApiProperties.OpdsFeaturedUrl) {
      val lang = language("lang")
      val feedTitle = Messages("featured_feed_title")(Lang(lang))

      val feed = feedService.feedForUrl(request.getRequestURI, lang, feedTitle) {
        feedService.editorsPickForLanguage(lang)
      }

      feed match {
        case Some(x) => showAcquisitionFeed(x)
        case None =>
          contentType = "text/plain"
          NotFound(body = "No books available for language.")
      }
    }

    get(BookApiProperties.OpdsLevelUrl) {
      val lang = language("lang")
      val level = params("lev")
      val feedTitle = s"${Messages("level_feed_title")(Lang(lang))} $level"

      val feed = feedService.feedForUrl(request.getRequestURI, lang, feedTitle) {
        feedService.feedEntriesForLanguageAndLevel(lang, level)
      }

      feed match {
        case Some(x) => showAcquisitionFeed(x)
        case None =>
          contentType = "text/plain"
          NotFound(body = "No books available for language.")
      }
    }

    private def showNavigationRoot(feedUrl: String, language: String, levels: Seq[String]) = {
      implicit val messageLang: Lang = Lang(language)
      val levelMessagePrefix: String = Messages("level_feed_title")

      <feed xmlns="http://www.w3.org/2005/Atom">
        <id>{getFeedId(feedUrl)}</id>
        <link rel="self"
              href={feedUrl}
              type="application/atom+xml;profile=opds-catalog;kind=navigation"/>
        <link rel="start"
              href={feedUrl}
              type="application/atom+xml;profile=opds-catalog;kind=navigation"/>
        <title>{Messages("opds_root_title")}</title>
        <updated>2010-01-10T10:03:10Z</updated>

        <entry>
          <title>{Messages("new_entries_feed_title")}</title>
          <link rel="http://opds-spec.org/sort/new"
                href={s"$OpdsPath/$language/new.xml"}
                type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
          <updated>2010-01-10T10:01:01Z</updated>
          <id>{getFeedId(s"/$language/new.xml")}</id>
          <content type="text">{Messages("new_entries_feed_description")}</content>
        </entry>

        <entry>
          <title>{Messages("featured_feed_title")}</title>
          <link rel="http://opds-spec.org/featured"
                href={s"$OpdsPath/$language/featured.xml"}
                type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
          <updated>2010-01-10T10:01:01Z</updated>
          <id>{getFeedId(s"/$language/featured.xml")}</id>
          <content type="text">{Messages("featured_feed_description")}</content>
        </entry>

        {levels.map(level =>
          <entry>
            <title>{s"$levelMessagePrefix $level"}</title>
            <link href={s"$OpdsPath/$language/level$level.xml"}
                  type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
            <updated>2010-01-10T10:01:01Z</updated>
            <id>{getFeedId(s"/$language/level$level.xml")}</id>
            <content type="text">{Messages("level_feed_description")}</content>
          </entry>
        )}
      </feed>
    }

    private def showAcquisitionFeed(feed: Feed) = {
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog">
        <id>{feed.feedDefinition.uuid}</id>
        <title>{feed.title}</title>
        <updated>{feed.updated.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</updated>
        <link href={feed.feedDefinition.url} rel="self"/>
        {feed.content.map(feedEntry =>
        <entry>
          <id>urn:uuid:{feedEntry.book.uuid}</id>
          <title>{feedEntry.book.title}</title>
          {feedEntry.book.contributors.map(contrib => <author><name>{contrib.name}</name></author>)}
          <updated>{sdf.format(new Date())}</updated>
          <summary>{feedEntry.book.description}</summary>
          {if(feedEntry.book.coverPhoto.isDefined) {
            <link href={feedEntry.book.coverPhoto.get.large} type="image/jpeg" rel="http://opds-spec.org/image"/>
            <link href={feedEntry.book.coverPhoto.get.small} type="image/png" rel="http://opds-spec.org/image/thumbnail"/>
          }}
          <link href={feedEntry.book.downloads.epub} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={feedEntry.book.downloads.pdf} type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
          {feedEntry.categories.sortBy(_.sortOrder).reverse.map(category =>
            <link href={category.url} rel="collection" title={category.title}/>
          )}
        </entry>
      )}
      </feed>
    }


    private def getFeedId(feedUrl: String) = {
      // TODO: Implement lookup of uuid for feed, and generate for new feeds
      s"urn:uuid:${UUID.randomUUID().toString}"
    }

  }
}
