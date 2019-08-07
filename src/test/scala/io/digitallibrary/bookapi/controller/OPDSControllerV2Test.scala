package io.digitallibrary.bookapi.controller

import java.time.ZoneId
import java.time.format.DateTimeFormatter

import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.model.api.{FeedEntry, FeedEntryV2}
import io.digitallibrary.bookapi.model.domain.Paging
import io.digitallibrary.language.model.LanguageTag
import org.mockito.Mockito.verify
import org.scalatra.test.scalatest.ScalatraFunSuite

class OPDSControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
  lazy val controllerV2 = new OPDSControllerV2

  addServlet(controllerV2, "/*")

  test("v2: that rendering of acquisition feeds includes all books with correct data") {
    val entry1: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry
    val entry2: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))

    val feed = TestData.ApiV2.DefaultFeed.copy(content = Seq(entry1, entry2))

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

        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/v1/ben/root.xml" title="Bengali" opds:facetGroup="Languages" opds:activeFacet="false"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/v1/eng/root.xml" title="English" opds:facetGroup="Languages" opds:activeFacet="true"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/v1/hin/root.xml" title="Hindu" opds:facetGroup="Languages" opds:activeFacet="false"/>

        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/v1/eng/root.xml" title="New arrivals" opds:facetGroup="Selection" opds:activeFacet="false"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/v1/eng/level1.xml" title="Level 1" opds:facetGroup="Selection" opds:activeFacet="false"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/v1/eng/level2.xml" title="Level 2" opds:facetGroup="Selection" opds:activeFacet="true"/>
        <link rel="http://opds-spec.org/facet" href="https://opds.test.digitallibrary.io/v1/eng/level3.xml" title="Level 3" opds:facetGroup="Selection" opds:activeFacet="false"/>

        <entry>
          <id>urn:uuid:{entry1.book.uuid}</id>
          <title>{entry1.book.title}</title>
          <author>
            <name>Author Authorson</name>
          </author>
          <author>
            <name>Co Author</name>
          </author>
          <contributor type="Photographer">
            <name>PhotoGrapher</name>
          </contributor>
          <contributor type="Illustrator">
            <name>IlluStrator</name>
          </contributor>
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
            <name>Author Authorson</name>
          </author>
          <author>
            <name>Co Author</name>
          </author>
          <contributor type="Photographer">
            <name>PhotoGrapher</name>
          </contributor>
          <contributor type="Illustrator">
            <name>IlluStrator</name>
          </contributor>
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

    val generated = controllerV2.render(feed, MoreInBothDirectionsV2(Paging(page = 3, pageSize = 10), lastPage = 10))
    val toCheck = generated.mkString.replaceAll("\\s", "")
    val expected = expectedXml.mkString.replaceAll("\\s", "")

    toCheck should equal (expected)
  }

  test("v2: that only first, next and last links are present and correct if there is more content ahead") {
    val entry1: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry
    val entry2: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry.copy(categories = Seq(TestData.ApiV2.DefaultFeedCategory))
    val feed = TestData.ApiV2.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controllerV2.render(feed, MoreAheadV2(Paging(page = 2, pageSize = 100), lastPage = 11))
    generated.mkString.contains("rel=\"previous\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=3\" rel=\"next\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=11\" rel=\"last\"/>") should be (true)
  }

  test("v2: that only first and previous links are present and correct if there is more content before") {
    val entry1: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry
    val entry2: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.ApiV2.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controllerV2.render(feed, MoreBeforeV2(Paging(page = 3, pageSize = 100)))
    generated.mkString.contains("rel=\"next\"") should be (false)
    generated.mkString.contains("rel=\"last\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=2\" rel=\"previous\"/>") should be (true)
  }

  test("v2: that first, previous, next and last links are present and correct if there is more content in both directions") {
    val entry1: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry
    val entry2: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.ApiV2.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controllerV2.render(feed, MoreInBothDirectionsV2(Paging(page = 3, pageSize = 100), lastPage = 15))
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=2\" rel=\"previous\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=4\" rel=\"next\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=100&amp;page=15\" rel=\"last\"/>") should be (true)
  }

  test("v2: that only previous and next links are present when there is only 1 page to show") {
    val entry1: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry
    val entry2: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.ApiV2.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controllerV2.render(feed, OnlyOnePageV2(Paging(page = 1, pageSize = 10)))
    generated.mkString.contains("rel=\"previous\"") should be (false)
    generated.mkString.contains("rel=\"next\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"last\"/>") should be (true)
  }

  test("v2: that incorrect page number for single page is replaced by page=1") {
    val entry1: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry
    val entry2: FeedEntryV2 = TestData.ApiV2.DefaultFeedEntry.copy(categories = Seq(TestData.Api.DefaultFeedCategory))
    val feed = TestData.ApiV2.DefaultFeed.copy(content = Seq(entry1, entry2))
    val generated = controllerV2.render(feed, OnlyOnePageV2(Paging(page = 2, pageSize = 10)))
    generated.mkString.contains("rel=\"previous\"") should be (false)
    generated.mkString.contains("rel=\"next\"") should be (false)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"first\"/>") should be (true)
    generated.mkString.contains("<link href=\"some-url?page-size=10&amp;page=1\" rel=\"last\"/>") should be (true)
  }

  test("v2: that /root.xml defaults to English root feed with default paging") {
    get("/v2/root.xml") {
      verify(feedServiceV2).allEntries(LanguageTag("eng"), Paging(page = 1, pageSize = 15))
    }
  }

}

