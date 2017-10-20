/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import no.gdl.bookapi.BookApiProperties.{OpdsLanguageParam, OpdsLevelParam, OpdsPath}
import no.gdl.bookapi.TestData.{LanguageCodeAmharic, LanguageCodeEnglish, LanguageCodeNorwegian}
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

    val calculatedFeedUrls = feedService.calculateFeedUrls
    calculatedFeedUrls.size should be (5)

    val expectedFeeds = BookApiProperties.OpdsFeeds.map(feed =>
      s"${OpdsPath}${feed.replace(OpdsLevelParam, "1").replace(OpdsLanguageParam, LanguageCodeEnglish)}")

    expectedFeeds.sorted should equal (calculatedFeedUrls.sorted)
  }

  test("that calculateFeedUrls calculates correct number of feeds for multiple languages and levels") {
    val languages = Seq(LanguageCodeEnglish, LanguageCodeNorwegian, LanguageCodeAmharic)
    val levels = Seq("1", "2", "3", "4", "5")

    val expectedNumberOfFeeds = (4 * languages.size) + (languages.size * levels.size)

    when(translationRepository.allAvailableLanguages()).thenReturn(languages)
    when(translationRepository.allAvailableLevels(any[Option[String]])(any[DBSession])).thenReturn(levels)

    val calculatedFeedUrls = feedService.calculateFeedUrls
    calculatedFeedUrls.size should be (expectedNumberOfFeeds)
  }



}
