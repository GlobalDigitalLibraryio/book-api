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
import no.gdl.bookapi.BookApiProperties.OpdsPath
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.domain.Sort
import no.gdl.bookapi.service.{ConverterService, ReadService}
import org.scalatra.NotFound

trait OPDSController {
  this: ReadService with ConverterService =>
  val opdsController: OPDSController
  val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
  val dtf: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  class OPDSController extends GdlController {

    val page = 1
    val pageSize = 10000 //TODO: Create partial opds feed entries, to solve paging

    before() {
      contentType = formats("xml")
    }

    get("/:lang/root.xml") {
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

    get("/:lang/new.xml") {
      val lang = language("lang")
      val books = readService.withLanguage(
        language = language("lang"),
        pageSize = pageSize,
        page = page,
        sort = Sort.ByArrivalDateDesc
      ).results

      if (books.nonEmpty){
        val feedUrl = s"/$lang/new.xml"
        val feedUpdated = books.head.dateArrived

        showAcquisitionFeed(
          feedUrl,
          Messages("new_entries_feed_title")(Lang(lang)),
          feedUpdated,
          books
        )
      } else {
        contentType = "text/plain"
        NotFound(body = "No books available for language.")
      }
    }

    get("/:lang/featured.xml") {
      val lang = language("lang")
      val feedUrl = s"/$lang/featured.xml"

      val editorsPick = readService.editorsPickForLanguage(lang)

      if (editorsPick.nonEmpty) {
        showAcquisitionFeed(
          feedUrl,
          Messages("featured_feed_title")(Lang(lang)),
          editorsPick.get.dateChanged,
          editorsPick.get.books
        )
      } else {
        contentType = "text/plain"
        NotFound(body = "No books available for language.")
      }
    }

    get("/:lang/level:lev.xml") {
      val lang = language("lang")
      val level = params("lev")

      val books = readService.withLanguageAndLevel(
        language = lang,
        readingLevel = Some(level),
        pageSize = pageSize,
        page = page,
        sort = Sort.ByTitleAsc
      ).results

      if(books.nonEmpty){
        implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

        val feedUrl = s"/$lang/level$level.xml"
        val feedTitle = s"${Messages("level_feed_title")(Lang(lang))} $level"
        val feedUpdated = books.maxBy(_.dateArrived).dateArrived
        showAcquisitionFeed(
          feedUrl,
          feedTitle,
          feedUpdated,
          books
        )
      } else {
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

    private def showAcquisitionFeed(feedUrl: String, feedTitle: String, feedUpdated: LocalDate, books: Seq[api.Book]) = {
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog">
        <id>{getFeedId(feedUrl)}</id>
        <title>{feedTitle}</title>
        <updated>{feedUpdated.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</updated>
        <link href={feedUrl} rel="self"/>
        {books.map(x =>
        <entry>
          <id>urn:uuid:{x.uuid}</id>
          <title>{x.title}</title>
          {x.contributors.map(contrib => <author><name>{contrib.name}</name></author>)}
          <updated>{sdf.format(new Date())}</updated>
          <summary>{x.description}</summary>
          {if(x.coverPhoto.isDefined) {
            <link href={x.coverPhoto.get.large} type="image/jpeg" rel="http://opds-spec.org/image"/>
            <link href={x.coverPhoto.get.small} type="image/png" rel="http://opds-spec.org/image/thumbnail"/>
          }}
          <link href={x.downloads.epub} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={x.downloads.pdf} type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
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
