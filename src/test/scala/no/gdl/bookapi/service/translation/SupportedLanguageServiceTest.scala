/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import no.gdl.bookapi.integration.crowdin.CrowdinClient
import no.gdl.bookapi.model.api.Language
import no.gdl.bookapi.model.crowdin.SupportedLanguage
import no.gdl.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito.{times, verify, when}

import scala.util.Success

class SupportedLanguageServiceTest extends UnitSuite with TestEnvironment {

  override val supportedLanguageService = new SupportedLanguageService

  test("that crowdinClient is only called once to load cache") {
    val oneLanguage = SupportedLanguage("English", "eng", "eng", "en", "eng", "en-GB", "androidCode", "osxCode", "osxLocale")
    val crowdinClientMock = mock[CrowdinClient]

    when(crowdinClientBuilder.withGenericAccess).thenReturn(crowdinClientMock)
    when(crowdinClientMock.getSupportedLanguages).thenReturn(Success(Seq(oneLanguage)))

    supportedLanguageService.getSupportedLanguages.size should be (1)
    supportedLanguageService.getSupportedLanguages.size should be (1)
    supportedLanguageService.getSupportedLanguages.size should be (1)
    supportedLanguageService.getSupportedLanguages.size should be (1)

    verify(crowdinClientMock, times(1)).getSupportedLanguages
  }

  test("that languages with invalid codes are filtered out") {
    val crowdinClientMock = mock[CrowdinClient]
    when(crowdinClientBuilder.withGenericAccess).thenReturn(crowdinClientMock)

    val validLanguage = SupportedLanguage("English", "eng", "eng", "en", "eng", "en-GB", "androidCode", "osxCode", "osxLocale")
    val invalidLanguage = SupportedLanguage("Invalid", "invalid", "invalid", "invalid", "invalid", "invalid", "invalid", "invalid", "invalid")
    when(crowdinClientMock.getSupportedLanguages).thenReturn(Success(Seq(validLanguage, invalidLanguage)))

    new SupportedLanguageService().getSupportedLanguages should be (Seq(Language("eng", "English")))
  }

}
