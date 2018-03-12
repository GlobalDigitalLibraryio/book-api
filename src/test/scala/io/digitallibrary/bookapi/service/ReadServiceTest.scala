/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import io.digitallibrary.bookapi.model.domain.PublishingStatus
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.Mockito._

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService = new ReadService

  test("that listAvailableLanguages returns languages sorted by name") {
    when(translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(Seq(LanguageTag(TestData.LanguageCodeEnglish), LanguageTag(TestData.LanguageCodeAmharic)))
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeAmharic))).thenReturn(TestData.Api.amharic)
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeEnglish))).thenReturn(TestData.Api.english)

    readService.listAvailablePublishedLanguages should equal(Seq(TestData.Api.amharic, TestData.Api.english))
  }

  test("that listAvailablePublishedCategoriesForLanguage creates a map from categories to reading levels") {
    when(translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, LanguageTag(TestData.LanguageCodeEnglish)))
      .thenReturn(Seq(("category1", "level1"), ("category1", "level3"), ("category2", "level1")))
    readService.listAvailablePublishedCategoriesForLanguage(LanguageTag(TestData.LanguageCodeEnglish)) should equal(Map("category2" -> List("level1"), "category1" -> List("level1", "level3")))
  }
}
