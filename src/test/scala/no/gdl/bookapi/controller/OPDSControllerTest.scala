/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import no.gdl.bookapi.model.api.{Feed, FeedEntry}
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}


class OPDSControllerTest extends UnitSuite with TestEnvironment {
  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  lazy val controller = new OPDSController

  test("that rendering of navigation feeds includes all feeds with correct data") {
    val self: Feed = TestData.Api.DefaultFeed
    val feed1 = TestData.Api.DefaultFeed.copy(title = "Feed 1")
    val feed2 = TestData.Api.DefaultFeed.copy(title = "Feed 2", rel = None)

    val expectedXml =
      <feed xmlns="http://www.w3.org/2005/Atom">
        <id>{self.feedDefinition.uuid}</id>
        <link rel="self" href={self.feedDefinition.url} type="application/atom+xml;profile=opds-catalog;kind=navigation"/>
        <link rel="start" href={self.feedDefinition.url} type="application/atom+xml;profile=opds-catalog;kind=navigation"/>
        <title>{self.title}</title>
        <updated>{self.updated.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
        <entry>
          <title>{feed1.title}</title>
          <link rel={feed1.rel.get} href={feed1.feedDefinition.url} type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
          <updated>{feed1.updated.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
          <id>{feed1.feedDefinition.uuid}</id>
          <content type="text">{feed1.description.get}</content>
        </entry>
        <entry>
          <title>{feed2.title}</title>
          <link rel="subsection" href={feed2.feedDefinition.url} type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
          <updated>{feed2.updated.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
          <id>{feed2.feedDefinition.uuid}</id>
          <content type="text">{feed2.description.get}</content>
        </entry>
      </feed>

    val generated = controller.render(self, Seq(feed1, feed2))
    val toCheck = generated.mkString.replaceAll("\\s", "")
    val expected = expectedXml.mkString.replaceAll("\\s", "")

    toCheck should equal (expected)
  }

  test("that rendering of acquisition feeds includes all books with correct data") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))

    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))

    val expectedXml =
      <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog" xmlns:lrmi="http://purl.org/dcx/lrmi-terms/">
        <id>{feed.feedDefinition.uuid}</id>
        <title>{feed.title}</title>
        <updated>{feed.updated.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
        <link href={feed.feedDefinition.url} rel="self"/>
        <entry>
          <id>urn:uuid:{entry1.book.uuid}</id>
          <title>{entry1.book.title}</title>
          <author>
            <name>{entry1.book.contributors.head.name}</name>
          </author>
          <updated>{entry1.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
          <dc:publisher>{entry1.book.publisher.name}</dc:publisher>
          <lrmi:educationalAlignment alignmentType="readingLevel" targetName={entry1.book.readingLevel.get}/>
          <summary>{entry1.book.description}</summary>
          <link href={entry1.book.downloads.epub} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={entry1.book.downloads.pdf} type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
        </entry>
        <entry>
          <id>urn:uuid:{entry2.book.uuid}</id>
          <title>{entry2.book.title}</title>
          <author>
            <name>{entry2.book.contributors.head.name}</name>
          </author>
          <updated>{entry2.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
          <dc:publisher>{entry2.book.publisher.name}</dc:publisher>
          <lrmi:educationalAlignment alignmentType="readingLevel" targetName={entry2.book.readingLevel.get}/>
          <summary>{entry2.book.description}</summary>
          <link href={entry2.book.downloads.epub} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={entry2.book.downloads.pdf} type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={entry2.categories.head.url} rel="collection" title={entry2.categories.head.title}/>
      </entry>
      </feed>

    val generated = controller.render(feed)
    val toCheck = generated.mkString.replaceAll("\\s", "")
    val expected = expectedXml.mkString.replaceAll("\\s", "")

    toCheck should equal (expected)
  }

}
