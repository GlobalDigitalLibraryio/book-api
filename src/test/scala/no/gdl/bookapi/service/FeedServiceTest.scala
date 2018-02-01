/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties.{OpdsLanguageParam, OpdsLevelParam}
import no.gdl.bookapi.TestData.{LanguageCodeAmharic, LanguageCodeEnglish, LanguageCodeNorwegian}
import no.gdl.bookapi.model.api.{Facet, FeedEntry}
import no.gdl.bookapi.{BookApiProperties, TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

class FeedServiceTest extends UnitSuite with TestEnvironment {

  override val feedService = new FeedService

  test("that calculateFeeds returns correct urls for one language and one level") {
    val languages = Seq(LanguageTag(LanguageCodeEnglish))
    val levels = Seq("1")

    when(translationRepository.allAvailableLanguages()).thenReturn(languages)
    when(translationRepository.allAvailableLevels(any[Option[LanguageTag]])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    calculatedFeedUrls.size should be (5)

    val expectedFeedUrls = BookApiProperties.OpdsFeeds.map(feed =>
      s"${feed.url.replace(OpdsLevelParam, "1").replace(OpdsLanguageParam, LanguageCodeEnglish)}")

    expectedFeedUrls.sorted should equal (calculatedFeedUrls.map(_.url).sorted)
  }

  test("that calculateFeedUrls calculates correct number of feeds for multiple languages and levels") {
    val languages = Seq(LanguageTag(LanguageCodeEnglish), LanguageTag(LanguageCodeNorwegian), LanguageTag(LanguageCodeAmharic))
    val levels = Seq("1", "2", "3", "4", "5")

    val expectedNumberOfFeeds = (4 * languages.size) + (languages.size * levels.size)

    when(translationRepository.allAvailableLanguages()).thenReturn(languages)
    when(translationRepository.allAvailableLevels(any[Option[LanguageTag]])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    calculatedFeedUrls.size should be (expectedNumberOfFeeds)
  }

  test("that featuredPath returns expected path for language"){
    feedService.featuredPath(LanguageTag("amh")) should equal ("/amh/featured.xml")
  }

  test("that levelPath returns expected path for language and level") {
    feedService.levelPath(LanguageTag("amh"), "1") should equal ("/amh/level1.xml")
  }

  test("that justArrivedPath returns expected path for language") {
    feedService.justArrivedPath(LanguageTag("amh")) should equal ("/amh/new.xml")
  }

  test("that addFeaturedCategory adds category with correct url") {
    val feedEntry = FeedEntry(TestData.Api.DefaultBook, Seq())
    val withCategory = feedService.addFeaturedCategory(feedEntry, LanguageTag("amh"))

    withCategory.categories.size should be (1)
    withCategory.categories.head.url should equal (s"${BookApiProperties.CloudFrontOpds}/amh/featured.xml")
  }

  test("that addJustArrivedCategory adds category with correct url") {
    val feedEntry = FeedEntry(TestData.Api.DefaultBook, Seq())
    val withCategory = feedService.addJustArrivedCategory(feedEntry, LanguageTag("amh"))

    withCategory.categories.size should be (1)
    withCategory.categories.head.url should equal (s"${BookApiProperties.CloudFrontOpds}/amh/new.xml")
  }

  test("that addLevelCategory adds category with correct url") {
    val feedEntry = FeedEntry(TestData.Api.DefaultBook, Seq())
    val withCategory = feedService.addLevelCategory(feedEntry, LanguageTag("amh"))

    withCategory.categories.size should be (1)
    withCategory.categories.head.url should equal (s"${BookApiProperties.CloudFrontOpds}/amh/level${feedEntry.book.readingLevel.get}.xml")
  }

  test("that facetsForLanguage returns facets for languages, with languages alphabetically sorted") {
    when(readService.listAvailableLanguagesAsLanguageTags).thenReturn(Seq("eng", "hin", "ben", "eng-latn-gb").map(LanguageTag(_)))
    feedService.facetsForLanguages(LanguageTag("eng")) should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/ben/new.xml", "Bengali", "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/new.xml", "English", "Languages", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/eng-latn-gb/new.xml", "English (Latin, United Kingdom)", "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/hin/new.xml", "Hindi", "Languages", isActive = false))
    )
  }

  test("that facetsForSelections returns facets for reading levels, with reading levels numerically sorted and new arrivals at the top") {
    val language = LanguageTag("eng")
    when(readService.listAvailableLevelsForLanguage(Some(language))).thenReturn(Seq("4", "1", "3", "2"))
    feedService.facetsForSelections(language, "http://local.digitallibrary.io/book-api/opds/eng/level3.xml") should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/eng/new.xml", "New arrivals", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level1.xml", "Level 1", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level2.xml", "Level 2", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level3.xml", "Level 3", "Selection", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level4.xml", "Level 4", "Selection", isActive = false)
    ))
  }
}
