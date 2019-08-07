package io.digitallibrary.bookapi.service

import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.model.domain.PublishingStatus
import io.digitallibrary.language.model.LanguageTag
import org.mockito.Mockito.when

class ReadServiceV2Test extends UnitSuite with TestEnvironment {
  override val readServiceV2 = new ReadServiceV2

  test("v2: that listAvailableLanguages returns languages sorted by name") {
    when(unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)).thenReturn(Seq(LanguageTag(TestData.LanguageCodeEnglish), LanguageTag(TestData.LanguageCodeAmharic)))
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeAmharic))).thenReturn(TestData.Api.amharic)
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeEnglish))).thenReturn(TestData.Api.english)

    readServiceV2.listAvailablePublishedLanguages should equal(Seq(TestData.Api.amharic, TestData.Api.english))
  }
}