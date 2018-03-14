/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import io.digitallibrary.bookapi.model.api.{Feed, FeedEntry}
import io.digitallibrary.bookapi.model.domain.Paging
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.Mockito.verify
import org.scalatra.test.scalatest.ScalatraFunSuite


class OPDSControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  lazy val controller = new OPDSController

  addServlet(controller, "/*")

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
        <updated>{self.updated.format(formatter)}</updated>
        <entry>
          <title>{feed1.title}</title>
          <link rel={feed1.rel.get} href={feed1.feedDefinition.url} type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
          <updated>{feed1.updated.format(formatter)}</updated>
          <id>{feed1.feedDefinition.uuid}</id>
          <content type="text">{feed1.description.get}</content>
        </entry>
        <entry>
          <title>{feed2.title}</title>
          <link rel="subsection" href={feed2.feedDefinition.url} type="application/atom+xml;profile=opds-catalog;kind=acquisition"/>
          <updated>{feed2.updated.format(formatter)}</updated>
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
        <updated>{feed.updated.format(formatter)}</updated>
        <link href={feed.feedDefinition.url} rel="self"/>
        <link href="some-url?page-size=10&amp;page=1" rel="first"/>
        <link href="some-url?page-size=10&amp;page=2" rel="previous"/>
        <link href="some-url?page-size=10&amp;page=4" rel="next"/>
        <link href="some-url?page-size=10&amp;page=10" rel="last"/>

        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/ben/root.xml" title="Bengali" opds:facetGroup="Languages" opds:activeFacet="false"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/eng/root.xml" title="English" opds:facetGroup="Languages" opds:activeFacet="true"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/hin/root.xml" title="Hindu" opds:facetGroup="Languages" opds:activeFacet="false"/>

        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/eng/root.xml" title="New arrivals" opds:facetGroup="Selection" opds:activeFacet="false"/>
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
          <link href={entry1.book.downloads.epub.get} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
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
          <link href={entry2.book.downloads.epub.get} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
          <link href={entry2.categories.head.url} rel="collection" title={entry2.categories.head.title}/>
      </entry>
      </feed>

    val generated = controller.render(feed, MoreInBothDirections(Paging(page = 3, pageSize = 10), lastPage = 10))
    val toCheck = generated.mkString.replaceAll("\\s", "")
    val expected = expectedXml.mkString.replaceAll("\\s", "")

    toCheck should equal (expected)
  }

  test("that only first, next and last links are present and correct if there is more content ahead") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controller.render(feed, MoreAhead(Paging(page = 2, pageSize = 100), lastPage = 11))
    generated.mkString.contains("rel=\"previous\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=3\" rel=\"next\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=11\" rel=\"last\"/>") should be (true)
  }

  test("that only first and previous links are present and correct if there is more content before") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controller.render(feed, MoreBefore(Paging(page = 3, pageSize = 100)))
    generated.mkString.contains("rel=\"next\"") should be (false)
    generated.mkString.contains("rel=\"last\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=2\" rel=\"previous\"/>") should be (true)
  }

  test("that first, previous, next and last links are present and correct if there is more content in both directions") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controller.render(feed, MoreInBothDirections(Paging(page = 3, pageSize = 100), lastPage = 15))
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=2\" rel=\"previous\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=4\" rel=\"next\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=15\" rel=\"last\"/>") should be (true)
  }

  test("that only previous and next links are present when there is only 1 page to show") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controller.render(feed, OnlyOnePage(Paging(page = 1, pageSize = 10)))
    generated.mkString.contains("rel=\"previous\"") should be (false)
    generated.mkString.contains("rel=\"next\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"last\"/>") should be (true)
  }

  test("that incorrect page number for single page is replaced by page=1") {
    val entry1: FeedEntry = TestData.Api.DefaultFeedEntry
    val entry2: FeedEntry = TestData.Api.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.Api.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controller.render(feed, OnlyOnePage(Paging(page = 2, pageSize = 10)))
    generated.mkString.contains("rel=\"previous\"") should be (false)
    generated.mkString.contains("rel=\"next\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"last\"/>") should be (true)
  }

  test("that /root.xml defaults to English root feed with default paging") {
    get("/root.xml") {
      verify(feedService).allEntries(LanguageTag("eng"), Paging(page = 1, pageSize = 15))
    }
  }

}
