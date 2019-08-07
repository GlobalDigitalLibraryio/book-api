/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}

import javax.servlet.http.HttpServletRequest
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.model.api.{Feed, FeedV2, FeedEntryV2}
import io.digitallibrary.bookapi.model.domain.{ContributorType, Paging}
import io.digitallibrary.bookapi.service._
import org.scalatra.NotFound

import scala.util.Try
import scala.xml.Elem

trait OPDSController {
  this: ReadServiceV2 with ConverterService with FeedService =>
  val opdsController: OPDSController
  val dtf: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val defaultLanguage = LanguageTag(BookApiProperties.DefaultLanguage)

  val defaultPageSize = 15
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

    get(BookApiProperties.OpdsRootDefaultLanguageUrl) {
      val (pagingStatus, books) = feedService.allEntries(defaultLanguage, extractPageAndPageSize())
      acquisitionFeed(books, RootFeed(defaultLanguage), pagingStatus)
    }

    get(BookApiProperties.OpdsRootUrl) {
      val lang = LanguageTag(params("lang"))
      val (pagingStatus, books) = feedService.allEntries(lang, extractPageAndPageSize())
      acquisitionFeed(books, RootFeed(lang), pagingStatus)
    }

    get(BookApiProperties.OpdsCategoryUrl) {
      val lang = LanguageTag(params("lang"))
      val category = params("category")
      val (pagingStatus, books) = feedService.entriesForLanguageAndCategory(lang, category, extractPageAndPageSize())
      acquisitionFeed(books, CategoryFeed(lang, category), pagingStatus)
    }

    get(BookApiProperties.OpdsCategoryAndLevelUrl) {
      val lang = LanguageTag(params("lang"))
      val category = params("category")
      val level = params("lev")
      val (pagingStatus, books) = feedService.entriesForLanguageCategoryAndLevel(language = lang, category = category, level = level, paging = extractPageAndPageSize())
      acquisitionFeed(books, LevelFeed(lang, category, level), pagingStatus)
    }

    private def acquisitionFeed(books: => Seq[FeedEntryV2], feedType: FeedType, pagingStatus: PagingStatus)(implicit request: HttpServletRequest) = {
      val lang = Try(LanguageTag(params("lang"))).getOrElse(defaultLanguage)
      feedService.feedForUrl(request.getRequestURI, feedType, books) match {
        case Some(feed) => render(feed, pagingStatus)
        case None =>
          contentType = "text/plain"
          NotFound(body = s"No books available for language $lang.")
      }
    }

    private[controller] def render(feed: FeedV2, pagingStatus: PagingStatus): Elem = {
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog" xmlns:lrmi="http://purl.org/dcx/lrmi-terms/">
        <id>{feed.feedDefinition.uuid}</id>
        <title>{feed.title}</title>
        <updated>{feed.updated.format(dtf)}</updated>
        <link href={feed.feedDefinition.url} rel="self"/>
        {pagingStatus match {
        case MoreAhead(Paging(currentPage, currentPageSize), lastPage) =>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=1"} rel="first"/>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=${currentPage + 1}"} rel="next"/>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=$lastPage"} rel="last"/>
        case MoreBefore(Paging(currentPage, currentPageSize)) =>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=1"} rel="first"/>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=${currentPage - 1}"} rel="previous"/>
        case MoreInBothDirections(Paging(currentPage, currentPageSize), lastPage) =>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=1"} rel="first"/>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=${currentPage - 1}"} rel="previous"/>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=${currentPage + 1}"} rel="next"/>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=$lastPage"} rel="last"/>
        case OnlyOnePage(Paging(_, currentPageSize)) =>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=1"} rel="first"/>
                <link href={s"${feed.feedDefinition.url}?page-size=$currentPageSize&page=1"} rel="last"/>
          }
        }
        {feed.facets.map(facet =>
          <link rel="http://opds-spec.org/facet" href={facet.href} title={facet.title} opds:facetGroup={facet.group} opds:activeFacet={facet.isActive.toString}/>)
        }

        {feed.content.map(feedEntry =>
          <entry>
            <id>urn:uuid:{feedEntry.book.uuid}</id>
            <title>{feedEntry.book.title}</title>
            {feedEntry.book.contributors.map(contrib =>
              ContributorType.valueOf(contrib.`type`).toOption match {
                case Some(ContributorType.Author) =>
                  <author>
                    <name>{contrib.name}</name>
                  </author>
                case _ =>
                  <contributor type={contrib.`type`}>
                    <name>{contrib.name}</name>
                  </contributor>
              }
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
            {if (feedEntry.book.coverImage.isDefined) {
              <link href={feedEntry.book.coverImage.get.url} type="image/jpeg" rel="http://opds-spec.org/image"/>
              <link href={feedEntry.book.coverImage.get.url + "?width=200"} type="image/png" rel="http://opds-spec.org/image/thumbnail"/>
            }}
            {if(feedEntry.book.downloads.epub.isDefined)
              <link href={feedEntry.book.downloads.epub.get} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
            }
            {if(feedEntry.book.downloads.pdf.isDefined)
              <link href={feedEntry.book.downloads.pdf.get} type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
            }
            {feedEntry.categories.sortBy(_.sortOrder).map(category =>
              <link href={category.url} rel="collection" title={category.title}/>
            )}
          </entry>
        )}
      </feed>
    }
  }
}
