/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.crowdin.CrowdinClient
import no.gdl.bookapi.model.api._
import no.gdl.bookapi.model.crowdin.CrowdinFile
import no.gdl.bookapi.model.domain.{InTranslation, InTranslationFile}
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.{Failure, Success}

class TranslationServiceTest extends UnitSuite with TestEnvironment {

  val service = new TranslationService

  override def beforeEach = {
    resetMocks()
  }

  test("that addTranslation returns a failure if from-language is not supported") {
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("eng", "English")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Failure(new RuntimeException("Not supported fromLanguage")))

    val translateResponse = service.addTranslation(TranslateRequest(1, "nob", "eng"))
    translateResponse should be a 'Failure
    translateResponse.failed.get.getMessage should equal ("Not supported fromLanguage")
  }

  test("that addTranslation returns a failure if toLanguage is not supported") {
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("eng", "English")))

    val translateResponse = service.addTranslation(TranslateRequest(1, "nob", "fra"))
    translateResponse should be a 'Failure
    translateResponse.failed.get.asInstanceOf[ValidationException].errors.head.message should equal ("The language 'fra' is not a supported language to translate to.")
  }

  test("that addTranslation only adds a new user when the translation exist") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("nob"))
    val existingInTranslations = Seq(
      TestData.Domain.DefaultinTranslation.copy(id = Some(1), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh")),
      inTranslationToUpdate
    )

    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(writeTranslationService.translationsForOriginalId(any[Long])).thenReturn(existingInTranslations)
    when(writeTranslationService.addUserToTranslation(any[InTranslation])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Success

    verify(writeTranslationService, times(1)).addUserToTranslation(any[InTranslation])
    verify(writeTranslationService, times(1)).translationsForOriginalId(any[Long])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(writeTranslationService)
  }

  test("that addTranslation adds a new target language when fromLanguage exists") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh"))

    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(writeTranslationService.translationsForOriginalId(any[Long])).thenReturn(Seq(inTranslationToUpdate))
    when(writeTranslationService.filesForTranslation(any[InTranslation])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(writeTranslationService.addTranslationWithFiles(any[InTranslation], any[Seq[InTranslationFile]], any[TranslateRequest])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Success

    verify(writeTranslationService, times(1)).translationsForOriginalId(any[Long])
    verify(writeTranslationService, times(1)).filesForTranslation(any[InTranslation])
    verify(crowdinClientMock, times(1)).addTargetLanguage(any[String])
    verify(writeTranslationService, times(1)).addTranslationWithFiles(any[InTranslation], any[Seq[InTranslationFile]], any[TranslateRequest])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(writeTranslationService)
  }

  test("that addTranslation creates a new translation") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(writeTranslationService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[Book])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[Book])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[Book], any[Seq[Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))

    when(writeTranslationService.newInTranslation(any[TranslateRequest], any[CrowdinFile], any[Seq[CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Success
  }


  test("that addTranslate deletes directory in Crowdin when DB-updates fails") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(writeTranslationService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[Book])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[Book])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[Book], any[Seq[Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))

    when(crowdinClientMock.deleteDirectoryFor(any[Book])).thenReturn(Success())

    when(writeTranslationService.newInTranslation(any[TranslateRequest], any[CrowdinFile], any[Seq[CrowdinFile]], any[String])).thenReturn(Failure(new DBException(new RuntimeException("Some message"))))


    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Failure

    verify(crowdinClientMock, times(1)).deleteDirectoryFor(any[Book])
  }

}
