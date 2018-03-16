/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import io.digitallibrary.bookapi.BookApiProperties._
import io.digitallibrary.bookapi.TestData.{LanguageCodeEnglish, LanguageCodeNorwegian}
import io.digitallibrary.bookapi.model.api.{Facet, SearchResult}
import io.digitallibrary.bookapi.model.domain.{Category, Paging, PublishingStatus}
import io.digitallibrary.bookapi.{TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

class FeedServiceTest extends UnitSuite with TestEnvironment {

  override val feedService = new FeedService

  val defaultFeedLocalization = FeedLocalization(
    rootTitle = "Root title",
    navTitle = "Nav title",
    categoriesTitle = "Categories",
    categoriesDescription = "Here are the descriptions",
    categoryTitle = _ => "Some category",
    levelTitle = level => s"Level $level",
    levelDescription = "Level description"
  )

  when(feedLocalizationService.localizationFor(any[LanguageTag])).thenReturn(defaultFeedLocalization)

  test("that calculateFeeds returns correct urls for one language, one category and one level") {
    val languages = Seq(LanguageTag(LanguageCodeEnglish))
    val levels = Map(
      Category(None, None, "cat1") -> Set("1")
    )

    when(translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(languages)
    when(translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(anyObject(), any[LanguageTag])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    calculatedFeedUrls.size should be (5)

    val expectedFeedUrls = Seq(OpdsRootDefaultLanguageUrl, OpdsNavUrl, OpdsRootDefaultLanguageUrl, OpdsCategoryAndLevelUrl).map(url =>
      s"${url.replace(OpdsLevelParam, "1")
        .replace(OpdsCategoryParam, "cat1")
        .replace(OpdsLanguageParam, LanguageCodeEnglish)}")

    calculatedFeedUrls.map(_.url).sorted should equal (Seq("/eng/category/cat1/level/1.xml", "/eng/category/cat1/root.xml", "/eng/nav.xml", "/eng/root.xml", "/root.xml").sorted)
  }

  test("that calculateFeedUrls calculates correct of feeds for multiple languages and levels") {
    val languages = Seq(LanguageTag(LanguageCodeEnglish), LanguageTag(LanguageCodeNorwegian))
    val norwegianCategoriesAndLevels = Map(
      Category(None, None, "cat1") -> Set("1", "2"),
      Category(None, None, "cat2") -> Set("decodable")
    )
    val englishCategoriesAndLevels = Map(
      Category(None, None, "cat1") -> Set("1", "2")
    )

    when(translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(languages)
    when(translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, LanguageTag(LanguageCodeNorwegian))).thenReturn(norwegianCategoriesAndLevels)
    when(translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, LanguageTag(LanguageCodeEnglish))).thenReturn(englishCategoriesAndLevels)

    val calculatedFeedUrls = feedService.calculateFeeds
    val expectedFeeds =
      """
        |/root.xml
        |/eng/nav.xml
        |/nob/nav.xml
        |/eng/root.xml
        |/nob/root.xml
        |/nob/category/cat1/root.xml
        |/nob/category/cat2/root.xml
        |/nob/category/cat1/level/1.xml
        |/nob/category/cat1/level/2.xml
        |/nob/category/cat2/level/decodable.xml
        |/eng/category/cat1/root.xml
        |/eng/category/cat1/level/1.xml
        |/eng/category/cat1/level/2.xml
      """
      .stripMargin.trim.split("\n").toSet

    calculatedFeedUrls.map(f => f.url).toSet should equal(expectedFeeds)
  }

  test("that levelPath returns expected path for language and level") {
    feedService.levelPath(LanguageTag("amh"), "cat1", "1") should equal ("/amh/category/cat1/level/1.xml")
  }

  test("that justArrivedPath returns expected path for language") {
    feedService.rootPath(LanguageTag("amh")) should equal ("/amh/root.xml")
  }

  test("that facetsForLanguage returns facets for languages, with languages alphabetically sorted") {
    when(readService.listAvailablePublishedLanguagesAsLanguageTags).thenReturn(Seq("eng", "hin", "ben", "eng-latn-gb").map(LanguageTag(_)))
    feedService.facetsForLanguages(LanguageTag("eng")) should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/ben/root.xml", LanguageTag("ben").localDisplayName.get, "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/root.xml", "English", "Languages", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/eng-latn-gb/root.xml", "English (Latin, United Kingdom)", "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/hin/root.xml", LanguageTag("hin").localDisplayName.get, "Languages", isActive = false))
    )
  }

  test("that facetsForReadingLevels returns facets for reading levels, with reading levels numerically sorted and new arrivals at the top") {
    val language = LanguageTag("eng")
    when(readService.listAvailablePublishedLevelsForLanguage(Some(language))).thenReturn(Seq("4", "1", "3", "2"))
    feedService.facetsForReadingLevels(language, "cat1", Some("3")) should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/eng/root.xml", "New arrivals", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/category/cat1/level/1.xml", "Level 1", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/category/cat1/level/2.xml", "Level 2", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/category/cat1/level/3.xml", "Level 3", "Selection", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/eng/category/cat1/level/4.xml", "Level 4", "Selection", isActive = false)
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
