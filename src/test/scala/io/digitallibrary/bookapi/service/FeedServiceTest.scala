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

    when(unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(languages)
    when(unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(anyObject(), any[LanguageTag])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeeds
    calculatedFeedUrls.size should be (4)

    val expectedFeedUrls = Seq(OpdsRootDefaultLanguageUrl, OpdsRootDefaultLanguageUrl, OpdsCategoryAndLevelUrl).map(url =>
      s"${url.replace(OpdsLevelParam, "1")
        .replace(OpdsCategoryParam, "cat1")
        .replace(OpdsLanguageParam, LanguageCodeEnglish)}")

    calculatedFeedUrls.map(_.url).sorted should equal (Seq("/v1/en/category/cat1/level/1.xml", "/v1/en/category/cat1/root.xml", "/v1/en/root.xml", "/v1/root.xml").sorted)
  }

  test("that calculateFeedUrls calculates correct of feeds for multiple languages and levels") {
    val languages = Seq(LanguageTag(LanguageCodeEnglish), LanguageTag(LanguageCodeNorwegian))
    val norwegianCategoriesAndLevels = Map(
      Category(None, None, "library_books") -> Set("1", "2"),
      Category(None, None, "classroom_books") -> Set("decodable")
    )
    val englishCategoriesAndLevels = Map(
      Category(None, None, "library_books") -> Set("1", "2")
    )

    when(unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(languages)
    when(unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, LanguageTag(LanguageCodeNorwegian))).thenReturn(norwegianCategoriesAndLevels)
    when(unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, LanguageTag(LanguageCodeEnglish))).thenReturn(englishCategoriesAndLevels)

    val calculatedFeedUrls = feedService.calculateFeeds
    val expectedFeeds =
      """
        |/v1/root.xml
        |/v1/en/root.xml
        |/v1/nb/root.xml
        |/v1/nb/category/library_books/root.xml
        |/v1/nb/category/classroom_books/root.xml
        |/v1/nb/category/library_books/level/1.xml
        |/v1/nb/category/library_books/level/2.xml
        |/v1/nb/category/classroom_books/level/decodable.xml
        |/v1/en/category/library_books/root.xml
        |/v1/en/category/library_books/level/1.xml
        |/v1/en/category/library_books/level/2.xml
      """
      .stripMargin.trim.split("\n").toSet

    calculatedFeedUrls.map(f => f.url).toSet should equal(expectedFeeds)
  }

  test("that facetsForLanguage returns facets for languages, with languages alphabetically sorted") {
    when(readService.listAvailablePublishedLanguagesAsLanguageTags).thenReturn(Seq("eng", "hin", "ben", "eng-latn-gb").map(LanguageTag(_)))
    feedService.facetsForLanguages(LanguageTag("eng")) should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/v1/bn/root.xml", LanguageTag("ben").localDisplayName.get, "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/root.xml", "English", "Languages", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en-latn-gb/root.xml", "English (Latin, United Kingdom)", "Languages", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/hi/root.xml", LanguageTag("hin").localDisplayName.get, "Languages", isActive = false))
    )
  }

  test("that facetsForReadingLevels returns facets for reading levels, with reading levels numerically sorted and new arrivals at the bottom") {
    val language = LanguageTag("eng")
    when(readService.listAvailablePublishedLevelsForLanguage(Some(language), Some("cat1"))).thenReturn(Seq("4", "1", "3", "2"))
    feedService.facetsForReadingLevels(language, "cat1", Some("3")) should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/category/cat1/level/1.xml", "Level 1", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/category/cat1/level/2.xml", "Level 2", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/category/cat1/level/3.xml", "Level 3", "Selection", isActive = true),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/category/cat1/level/4.xml", "Level 4", "Selection", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/category/cat1/root.xml", "New arrivals", "Selection", isActive = false)
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

  test("that facetsForCategories are sorted so that library books comes first") {
    val language = LanguageTag("eng")
    val categoriesAndLevels = Map(
      Category(None, None, "classroom_books") -> Set("decodable"),
      Category(None, None, "library_books") -> Set("1", "2")
    )
    when(readService.listAvailablePublishedCategoriesForLanguage(language)).thenReturn(categoriesAndLevels)
    feedService.facetsForCategories(language, currentCategory = None) should equal (Seq(
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/category/library_books/root.xml", "Some category", "Category", isActive = false),
      Facet("http://local.digitallibrary.io/book-api/opds/v1/en/category/classroom_books/root.xml", "Some category", "Category", isActive = false)
    ))
  }

  test("that facetsForReadingLevels are sorted with decodable first, then numeric levels ascending, then read-aloud and finally new arrivals") {
    val language = LanguageTag("eng")
    val levels = Seq("1", "2", "3", "4", "read-aloud", "decodable")
    when(readService.listAvailablePublishedLevelsForLanguage(Some(language), Some("library_books"))).thenReturn(levels)
    feedService.facetsForReadingLevels(language, currentCategory = "library_books", currentReadingLevel = None).map(_.href.split('/').toList.last) should equal (Seq(
      "decodable.xml",
      "1.xml",
      "2.xml",
      "3.xml",
      "4.xml",
      "read-aloud.xml",
      "root.xml"
    ))
  }

}
