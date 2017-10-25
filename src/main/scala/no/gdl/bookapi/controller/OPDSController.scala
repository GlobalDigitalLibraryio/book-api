/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import java.text.SimpleDateFormat
import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.servlet.http.HttpServletRequest

import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.{Feed, FeedEntry}
import no.gdl.bookapi.service.{ConverterService, FeedService, ReadService}
import org.scalatra.NotFound

trait OPDSController {
  this: ReadService with ConverterService with FeedService =>
  val opdsController: OPDSController
  val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
  val dtf: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  class OPDSController extends GdlController {
    before() {
      contentType = formats("xml")
    }

    get(BookApiProperties.OpdsNavUrl.url) {
      navigationFeed {
        feedService.feedsForNavigation(language("lang"))
      }
    }

    get(BookApiProperties.OpdsRootUrl.url) {
      acquisitionFeed() {
        feedService.fullListOfFeedEntries(language("lang"))
      }
    }

    get(BookApiProperties.OpdsNewUrl.url) {
      acquisitionFeed() {
        feedService.newEntriesFor(language("lang"))
      }
    }

    get(BookApiProperties.OpdsFeaturedUrl.url) {
      val updated = readService.editorsPickForLanguage(language("lang")).map(_.dateChanged)

      acquisitionFeed(updated) {
        feedService.editorsPickForLanguage(language("lang"))
      }
    }

    get(BookApiProperties.OpdsLevelUrl.url) {
      acquisitionFeed() {
        feedService.feedEntriesForLanguageAndLevel(language("lang"), params("lev"))
      }
    }

    private def navigationFeed(getFeeds: => Seq[Feed])(implicit request: HttpServletRequest) = {
      val lang = language("lang")
      val self = feedService.feedForUrl(request.getRequestURI, lang) {Seq()}
      self match {
        case Some(s) => renderNavigationRoot(s, getFeeds)
        case None =>
          contentType = "text/plain"
          NotFound(body = s"No books available for language $lang.")
      }
    }

    private def renderNavigationRoot(self: Feed, feeds: Seq[Feed]) = {
      <feed xmlns="http://www.w3.org/2005/Atom">
        <id>{self.feedDefinition.uuid}</id>
        <link rel="self"
              href={self.feedDefinition.url}
              type="application/atom+xml;profile=opds-catalog;kind=navigation"/>
        <link rel="start"
              href={self.feedDefinition.url}
              type="application/atom+xml;profile=opds-catalog;kind=navigation"/>
        <title>{self.title}</title>
        <updated>{self.updated.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</updated>
        {feeds.map(feed =>
          <entry>
            <title>{feed.title}</title>
            <link rel={feed.rel.getOrElse("subsection")}
                  href={feed.feedDefinition.url}
                  type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
            <updated>{feed.updated.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</updated>
            <id>{feed.feedDefinition.uuid}</id>
            {if (feed.description.isDefined) {
              <content type="text">{feed.description.get}</content>
            }}
          </entry>
        )}
      </feed>
    }

    private def acquisitionFeed(feedUpdated: Option[LocalDate] = None)(getBooks: => Seq[FeedEntry])(implicit request: HttpServletRequest) = {
      val lang = language("lang")
      feedService.feedForUrl(request.getRequestURI, lang, feedUpdated)(getBooks) match {
        case Some(x) => renderAcquisitionFeed(x)
        case None =>
          contentType = "text/plain"
          NotFound(body = s"No books available for language $lang.")
      }
    }

    private def renderAcquisitionFeed(feed: Feed) = {
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog">
        <id>{feed.feedDefinition.uuid}</id>
        <title>{feed.title}</title>
        <updated>{feed.updated.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</updated>
        <link href={feed.feedDefinition.url} rel="self"/>
        {feed.content.map(feedEntry =>
          <entry>
            <id>urn:uuid:{feedEntry.book.uuid}</id>
            <title>{feedEntry.book.title}</title>
            {feedEntry.book.contributors.map(contrib =>
              <author>
                <name>{contrib.name}</name>
              </author>
            )}
            <updated>{sdf.format(new Date())}</updated>
            <summary>{feedEntry.book.description}</summary>
            {if (feedEntry.book.coverPhoto.isDefined) {
              <link href={feedEntry.book.coverPhoto.get.large} type="image/jpeg" rel="http://opds-spec.org/image"/>
              <link href={feedEntry.book.coverPhoto.get.small} type="image/png" rel="http://opds-spec.org/image/thumbnail"/>
            }}
            <link href={feedEntry.book.downloads.epub} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
            <link href={feedEntry.book.downloads.pdf} type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
            {feedEntry.categories.sortBy(_.sortOrder).map(category =>
              <link href={category.url} rel="collection" title={category.title}/>
            )}
          </entry>
        )}
      </feed>
    }
  }
}
