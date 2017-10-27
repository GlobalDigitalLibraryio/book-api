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
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  class OPDSController extends GdlController {
    before() {
      contentType = formats("xml")
    }

    get(BookApiProperties.OpdsNavUrl.url) {
      val navFeeds = feedService.feedsForNavigation(language("lang"))
      val navUpdated = navFeeds.map(_.updated).sorted.reverse.headOption
      navigationFeed(feedUpdated = navUpdated, feeds = navFeeds)
    }

    get(BookApiProperties.OpdsRootUrl.url) {
      acquisitionFeed(books = feedService.allEntries(language("lang")))
    }

    get(BookApiProperties.OpdsNewUrl.url) {
      acquisitionFeed(books = feedService.newEntries(language("lang")))
    }

    get(BookApiProperties.OpdsFeaturedUrl.url) {
      val lang = language("lang")
      acquisitionFeed(
        feedUpdated = feedService.editorsPickLastUpdated(lang),
        books = feedService.editorsPicks(lang))
    }

    get(BookApiProperties.OpdsLevelUrl.url) {
      acquisitionFeed(
        titleArgs = Seq(params("lev")),
        books = feedService.entriesForLanguageAndLevel(language("lang"), params("lev")))
    }

    private def navigationFeed(feedUpdated: Option[LocalDate], feeds: => Seq[Feed])(implicit request: HttpServletRequest) = {
      val lang = language("lang")
      val selfOpt = feedService.feedForUrl(request.getRequestURI, lang, feedUpdated, Seq(), Seq())
      selfOpt match {
        case None =>
          contentType = "text/plain"
          NotFound(body = s"No books available for language $lang.")

        case Some(self) => {
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
      }
    }

    private def acquisitionFeed(feedUpdated: Option[LocalDate] = None, titleArgs: Seq[String] = Seq(), books: => Seq[FeedEntry])(implicit request: HttpServletRequest) = {
      val lang = language("lang")
      feedService.feedForUrl(request.getRequestURI, lang, feedUpdated, titleArgs, books) match {
        case None =>
          contentType = "text/plain"
          NotFound(body = s"No books available for language $lang.")

        case Some(feed) => {
          <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog" xmlns:lrmi="http://purl.org/dcx/lrmi-terms/">
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
                <lrmi:educationalAlignment alignmentType="readingLevel" targetName={feedEntry.book.readingLevel.getOrElse("1")} />
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
  }
}
