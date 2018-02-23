/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.{PublishingStatus, SearchResult, Sort}
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService = new ReadService

  test("that listAvailableLanguages returns languages sorted by name") {
    when(translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(Seq(LanguageTag(TestData.LanguageCodeEnglish), LanguageTag(TestData.LanguageCodeAmharic)))
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeAmharic))).thenReturn(TestData.Api.amharic)
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeEnglish))).thenReturn(TestData.Api.english)

    readService.listAvailablePublishedLanguages should equal(Seq(TestData.Api.amharic, TestData.Api.english))
  }
}
