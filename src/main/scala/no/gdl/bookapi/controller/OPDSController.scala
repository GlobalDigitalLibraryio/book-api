/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}
import javax.servlet.http.HttpServletRequest

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.{Feed, FeedEntry}
import no.gdl.bookapi.model.domain.Paging
import no.gdl.bookapi.service.{ConverterService, FeedService, ReadService}
import org.scalatra.NotFound

import scala.xml.Elem

trait OPDSController {
  this: ReadService with ConverterService with FeedService =>
  val opdsController: OPDSController
  val dtf: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val defaultPageSize = 10
  val maxPageSize = 100

  class OPDSController extends GdlController {
    before() {
      contentType = formats("xml")
    }

    def extractPageAndPageSize(): Paging = {
      Paging(
        page = intOrDefault("page", 1).max(1),
        pageSize = intOrDefault("page-size", defaultPageSize).min(maxPageSize).max(1)
      )
    }

    get(BookApiProperties.OpdsNavUrl.url) {
      val navFeeds = feedService.feedsForNavigation(LanguageTag(params("lang")))
      val navLastUpdated = navFeeds.map(_.updated).sorted.reverse.headOption
      navigationFeed(feedUpdated = navLastUpdated, feeds = navFeeds)
    }

    get(BookApiProperties.OpdsRootUrl.url) {
      val paging = extractPageAndPageSize()
      acquisitionFeed(books = feedService.allEntries(LanguageTag(params("lang")), paging), paging = paging)
    }

    get(BookApiProperties.OpdsNewUrl.url) {
      val paging = extractPageAndPageSize()
      acquisitionFeed(books = feedService.newEntries(LanguageTag(params("lang"))), paging = paging)
    }

    get(BookApiProperties.OpdsLevelUrl.url) {
      val paging = extractPageAndPageSize()
      acquisitionFeed(
        titleArgs = Seq(params("lev")),
        books = feedService.entriesForLanguageAndLevel(LanguageTag(params("lang")), params("lev"), paging),
        paging = paging)
    }

    private def navigationFeed(feedUpdated: Option[LocalDate], feeds: => Seq[Feed])(implicit request: HttpServletRequest) = {
      val lang = LanguageTag(params("lang"))
      val selfOpt = feedService.feedForUrl(request.getRequestURI, lang, feedUpdated, Seq(), Seq())
      selfOpt match {
        case Some(self) => render(self, feeds)
        case None =>
          contentType = "text/plain"
          NotFound(body = s"No books available for language $lang.")
      }
    }

    private def acquisitionFeed(feedUpdated: Option[LocalDate] = None, titleArgs: Seq[String] = Seq(), books: => Seq[FeedEntry], paging: Paging)(implicit request: HttpServletRequest) = {
      val lang = LanguageTag(params("lang"))
      feedService.feedForUrl(request.getRequestURI, lang, feedUpdated, titleArgs, books) match {
        case Some(feed) => render(feed, paging)
        case None =>
          contentType = "text/plain"
          NotFound(body = s"No books available for language $lang.")
      }
    }

    private[controller] def render(self: Feed, feeds: Seq[Feed]): Elem = {
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

    private[controller] def render(feed: Feed, paging: Paging): Elem = {
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog" xmlns:lrmi="http://purl.org/dcx/lrmi-terms/">
        <id>{feed.feedDefinition.uuid}</id>
        <title>{feed.title}</title>
        <updated>{feed.updated.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</updated>
        <link href={feed.feedDefinition.url} rel="self"/>
        <link href={s"${feed.feedDefinition.url}?page-size=${paging.pageSize}&page=${paging.page + 1}"} rel="next"/>
        {feed.facets.map(facet =>
          <link rel="http://opds-spec.org/facet" href={facet.href} title={facet.title} opds:facetGroup={facet.group} opds:activeFacet={facet.isActive.toString}/>)
        }
        {feed.content.map(feedEntry =>
          <entry>
            <id>urn:uuid:{feedEntry.book.uuid}</id>
            <title>{feedEntry.book.title}</title>
            {feedEntry.book.contributors.map(contrib =>
              <author>
                <name>{contrib.name}</name>
              </author>
            )}
            <dc:license>{feedEntry.book.license.description.getOrElse(feedEntry.book.license.name)}</dc:license>
            <dc:publisher>{feedEntry.book.publisher.name}</dc:publisher>
            <updated>{feedEntry.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</updated>
            {if(feedEntry.book.dateCreated.isDefined) {
              <dc:created>{feedEntry.book.dateCreated.get.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</dc:created>
            }}
            <published>{feedEntry.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</published>
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
