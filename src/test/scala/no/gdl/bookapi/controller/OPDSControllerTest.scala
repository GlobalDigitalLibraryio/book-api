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
        <link href="some-url?page-size=100&amp;page=2" rel="next"/>

        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/ben/new.xml" title="Bengali" opds:facetGroup="Languages" opds:activeFacet="false"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/eng/new.xml" title="English" opds:facetGroup="Languages" opds:activeFacet="true"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/hin/new.xml" title="Hindu" opds:facetGroup="Languages" opds:activeFacet="false"/>

        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/eng/new.xml" title="New arrivals" opds:facetGroup="Selection" opds:activeFacet="false"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/eng/level1.xml" title="Level 1" opds:facetGroup="Selection" opds:activeFacet="false"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/eng/level2.xml" title="Level 2" opds:facetGroup="Selection" opds:activeFacet="true"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/eng/level3.xml" title="Level 3" opds:facetGroup="Selection" opds:activeFacet="false"/>

        <entry>
          <id>urn:uuid:{entry1.book.uuid}</id>
          <title>{entry1.book.title}</title>
          <author>
            <name>{entry1.book.contributors.head.name}</name>
          </author>
          <dc:license>{entry1.book.license.description.get}</dc:license>
          <dc:publisher>{entry1.book.publisher.name}</dc:publisher>
          <updated>{entry1.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
          <dc:created>{entry1.book.dateCreated.get.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</dc:created>
          <published>{entry1.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</published>
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
          <dc:license>{entry2.book.license.description.get}</dc:license>
          <dc:publisher>{entry2.book.publisher.name}</dc:publisher>
          <updated>{entry2.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</updated>
          <dc:created>{entry2.book.dateCreated.get.atStartOfDay(ZoneId.systemDefault()).format(dtf)}</dc:created>
          <published>{entry2.book.dateArrived.atStartOfDay(ZoneId.systemDefault()).format(formatter)}</published>
          <lrmi:educationalAlignment alignmentType="readingLevel" targetName={entry2.book.readingLevel.get}/>
          <summary>{entry2.book.description}</summary>
          <link href={entry2.book.downloads.epub} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={entry2.book.downloads.pdf} type="application/pdf" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={entry2.categories.head.url} rel="collection" title={entry2.categories.head.title}/>
      </entry>
      </feed>

    val generated = controller.render(feed, HasMore(currentPage = 1, currentPageSize = 100))
    val toCheck = generated.mkString.replaceAll("\\s", "")
    val expected = expectedXml.mkString.replaceAll("\\s", "")

    toCheck should equal (expected)
  }

  test("that next link is present and pointing to the next page if there is more content to show") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controller.render(feed, HasMore(currentPage = 2, currentPageSize = 100))
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=3\" rel=\"next\"/>") should be (true)
  }

  test("that next link is not present when there is no more content to show") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controller.render(feed, NothingMore)
    generated.mkString.contains("<link href=\"some-url?page-size") should be (false)
  }


}
