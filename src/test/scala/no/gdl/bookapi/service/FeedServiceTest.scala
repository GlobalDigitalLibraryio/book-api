/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import no.gdl.bookapi.BookApiProperties.{OpdsLanguageParam, OpdsLevelParam, OpdsPath}
import no.gdl.bookapi.TestData.{LanguageCodeAmharic, LanguageCodeEnglish, LanguageCodeNorwegian}
import no.gdl.bookapi.model.api.FeedEntry
import no.gdl.bookapi.{BookApiProperties, TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

class FeedServiceTest extends UnitSuite with TestEnvironment {

  override val feedService = new FeedService

  test("that calculateFeedUuids returns correct urls for one language and one level") {
    val languages = Seq(LanguageCodeEnglish)
    val levels = Seq("1")

    when(translationRepository.allAvailableLanguages()).thenReturn(languages)
    when(translationRepository.allAvailableLevels(any[Option[String]])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    calculatedFeedUrls.size should be (5)

    val expectedFeedUrls = BookApiProperties.OpdsFeeds.map(feed =>
      s"$OpdsPath${feed.url.replace(OpdsLevelParam, "1").replace(OpdsLanguageParam, LanguageCodeEnglish)}")

    expectedFeedUrls.sorted should equal (calculatedFeedUrls.map(_.url).sorted)
  }

  test("that calculateFeedUrls calculates correct number of feeds for multiple languages and levels") {
    val languages = Seq(LanguageCodeEnglish, LanguageCodeNorwegian, LanguageCodeAmharic)
    val levels = Seq("1", "2", "3", "4", "5")

    val expectedNumberOfFeeds = (4 * languages.size) + (languages.size * levels.size)

    when(translationRepository.allAvailableLanguages()).thenReturn(languages)
    when(translationRepository.allAvailableLevels(any[Option[String]])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    calculatedFeedUrls.size should be (expectedNumberOfFeeds)
  }

  test("that featuredPath returns expected path for language"){
    feedService.featuredPath("amh") should equal ("/book-api/opds/amh/featured.xml")
  }

  test("that levelPath returns expected path for language and level") {
    feedService.levelPath("amh", "1") should equal ("/book-api/opds/amh/level1.xml")
  }

  test("that justArrivedPath returns expected path for language") {
    feedService.justArrivedPath("amh") should equal ("/book-api/opds/amh/new.xml")
  }

  test("that addFeaturedCategory adds category with correct url") {
    val feedEntry = FeedEntry(TestData.Api.DefaultBook, Seq())
    val withCategory = feedService.addFeaturedCategory(feedEntry, "amh")

    withCategory.categories.size should be (1)
    withCategory.categories.head.url should equal (s"${BookApiProperties.Domain}/book-api/opds/amh/featured.xml")
  }

  test("that addJustArrivedCategory adds category with correct url") {
    val feedEntry = FeedEntry(TestData.Api.DefaultBook, Seq())
    val withCategory = feedService.addJustArrivedCategory(feedEntry, "amh")

    withCategory.categories.size should be (1)
    withCategory.categories.head.url should equal (s"${BookApiProperties.Domain}/book-api/opds/amh/new.xml")
  }

  test("that addLevelCategory adds category with correct url") {
    val feedEntry = FeedEntry(TestData.Api.DefaultBook, Seq())
    val withCategory = feedService.addLevelCategory(feedEntry, "amh")

    withCategory.categories.size should be (1)
    withCategory.categories.head.url should equal (s"${BookApiProperties.Domain}/book-api/opds/amh/level${feedEntry.book.readingLevel.get}.xml")
  }
}
