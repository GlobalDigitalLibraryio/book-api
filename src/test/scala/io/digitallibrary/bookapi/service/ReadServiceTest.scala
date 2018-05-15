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
    when(unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(Seq(LanguageTag(TestData.LanguageCodeEnglish), LanguageTag(TestData.LanguageCodeAmharic)))
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeAmharic))).thenReturn(TestData.Api.amharic)
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeEnglish))).thenReturn(TestData.Api.english)

    readService.listAvailablePublishedLanguages should equal(Seq(TestData.Api.amharic, TestData.Api.english))
  }

}
