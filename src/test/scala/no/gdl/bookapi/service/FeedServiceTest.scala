/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties._
import no.gdl.bookapi.TestData.{LanguageCodeAmharic, LanguageCodeEnglish, LanguageCodeNorwegian}
import no.gdl.bookapi.model.api.{Facet, SearchResult}
import no.gdl.bookapi.model.domain.Paging
import no.gdl.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

class FeedServiceTest extends UnitSuite with TestEnvironment {

  override val feedService = new FeedService

  val defaultFeedLocalization = FeedLocalization(
    rootTitle = "Root title",
    navTitle = "Nav title",
    levelTitle = level => s"Level $level",
    levelDescription = "Level description"
  )

  when(feedLocalizationService.localizationFor(any[LanguageTag])).thenReturn(defaultFeedLocalization)

  test("that calculateFeeds returns correct urls for one language and one level") {
    val languages = Seq(LanguageTag(LanguageCodeEnglish))
    val levels = Seq("1")

    when(translationRepository.allAvailableLanguages()).thenReturn(languages)
    when(translationRepository.allAvailableLevels(any[Option[LanguageTag]])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    calculatedFeedUrls.size should be (3)

    val expectedFeedUrls = Seq(OpdsNavUrl, OpdsRootUrl, OpdsLevelUrl).map(url =>
      s"${url.replace(OpdsLevelParam, "1").replace(OpdsLanguageParam, LanguageCodeEnglish)}")

    expectedFeedUrls.sorted should equal (calculatedFeedUrls.map(_.url).sorted)
  }

  test("that calculateFeedUrls calculates correct of feeds for multiple languages and levels") {
    val languages = Seq(LanguageTag(LanguageCodeEnglish), LanguageTag(LanguageCodeNorwegian), LanguageTag(LanguageCodeAmharic))
    val levels = Seq("1", "2", "3")

    when(translationRepository.allAvailableLanguages()).thenReturn(languages)
    when(translationRepository.allAvailableLevels(any[Option[LanguageTag]])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    val expectedFeeds =
      """
        |/eng/nav.xml
        |/nob/nav.xml
        |/amh/nav.xml
        |/eng/root.xml
        |/nob/root.xml
        |/amh/root.xml
        |/eng/level1.xml
        |/eng/level2.xml
        |/eng/level3.xml
        |/nob/level1.xml
        |/nob/level2.xml
        |/nob/level3.xml
        |/amh/level1.xml
        |/amh/level2.xml
        |/amh/level3.xml
      """
      .stripMargin.trim.split("\n").toSet

    calculatedFeedUrls.map(f => f.url).toSet should equal(expectedFeeds)
  }

  test("that levelPath returns expected path for language and level") {
    feedService.levelPath(LanguageTag("amh"), "1") should equal ("/amh/level1.xml")
  }

  test("that justArrivedPath returns expected path for language") {
    feedService.rootPath(LanguageTag("amh")) should equal ("/amh/root.xml")
  }

  test("that facetsForLanguage returns facets for languages, with languages alphabetically sorted") {
    when(readService.listAvailableLanguagesAsLanguageTags).thenReturn(Seq("eng", "hin", "ben", "eng-latn-gb").map(LanguageTag(_)))
    feedService.facetsForLanguages(LanguageTag("eng")) should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/ben/root.xml", "Bengali", "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/root.xml", "English", "Languages", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/eng-latn-gb/root.xml", "English (Latin, United Kingdom)", "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/hin/root.xml", "Hindi", "Languages", isActive = false))
    )
  }

  test("that facetsForSelections returns facets for reading levels, with reading levels numerically sorted and new arrivals at the top") {
    val language = LanguageTag("eng")
    when(readService.listAvailableLevelsForLanguage(Some(language))).thenReturn(Seq("4", "1", "3", "2"))
    feedService.facetsForSelections(language, "http://local.digitallibrary.io/book-api/opds/eng/level3.xml") should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/eng/root.xml", "New arrivals", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level1.xml", "Level 1", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level2.xml", "Level 2", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level3.xml", "Level 3", "Selection", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/level4.xml", "Level 4", "Selection", isActive = false)
    ))
  }

  test("that searchResultToPagingStatus returns correct status when there's more content ahead") {
    val searchResult = mock[SearchResult]
    when(searchResult.totalCount).thenReturn(130)
    feedService.searchResultToPagingStatus(searchResult, Paging(1, 25)) should equal (MoreAhead(Paging(1, 25), lastPage = 6))
  }

  test("that searchResultToPagingStatus returns correct status when there's more content before") {
    val searchResult = mock[SearchResult]
    when(searchResult.totalCount).thenReturn(50)
    feedService.searchResultToPagingStatus(searchResult, Paging(2, 25)) should equal (MoreBefore(Paging(2, 25)))
  }

  test("that searchResultToPagingStatus returns correct status when there's more content in both directions") {
    val searchResult = mock[SearchResult]
    when(searchResult.totalCount).thenReturn(120)
    feedService.searchResultToPagingStatus(searchResult, Paging(3, 25)) should equal (MoreInBothDirections(Paging(3, 25), lastPage = 5))
  }

  test("that searchResultToPagingStatus returns correct status when there's only one page of content") {
    val searchResult = mock[SearchResult]
    when(searchResult.totalCount).thenReturn(23)
    feedService.searchResultToPagingStatus(searchResult, Paging(1, 25)) should equal (OnlyOnePage(Paging(1, 25)))
  }

}
