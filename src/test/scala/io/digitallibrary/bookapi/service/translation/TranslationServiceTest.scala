/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import io.digitallibrary.bookapi.integration.crowdin.CrowdinClient
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class TranslationServiceTest extends UnitSuite with TestEnvironment {

  val service = new TranslationService

  override def beforeEach = {
    resetMocks()
  }

  test("that addTranslation returns a failure if from-language is not supported") {
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("eng", "English")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Failure(new RuntimeException("Not supported fromLanguage")))

    val translateResponse = service.addTranslation(TranslateRequest(bookId = 1, fromLanguage = "nob", toLanguage = "eng", Some("userId")))
    translateResponse should be a 'Failure
    translateResponse.failed.get.getMessage should equal("Not supported fromLanguage")
  }

  test("that addTranslation returns a failure if toLanguage is not supported") {
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("eng", "English")))

    val translateResponse = service.addTranslation(TranslateRequest(1, "nob", "fra", Some("userId")))
    translateResponse should be a 'Failure
    translateResponse.failed.get.asInstanceOf[api.ValidationException].errors.head.message should equal("The language 'fra' is not a supported language to translate to.")
  }

  test("that addTranslation only adds a new user when the translation exist") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("nob"))
    val existingInTranslations = Seq(
      TestData.Domain.DefaultinTranslation.copy(id = Some(1), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh")),
      inTranslationToUpdate
    )

    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(existingInTranslations)
    when(translationDbService.addUserToTranslation(any[domain.InTranslation], any[String])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success

    verify(translationDbService).addUserToTranslation(any[domain.InTranslation], any[String])
    verify(writeService, never()).addTranslatorToTranslation(any[Long], any[domain.Person])
    verify(translationDbService).translationsForOriginalId(any[Long])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("that addTranslation adds a new target language when fromLanguage exists") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh"))

    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq(inTranslationToUpdate))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(writeService.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultTranslation))
    when(translationDbService.addTranslationWithFiles(any[domain.InTranslation], any[Seq[domain.InTranslationFile]], any[domain.Translation], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success

    verify(translationDbService, times(1)).translationsForOriginalId(any[Long])
    verify(translationDbService, times(1)).filesForTranslation(any[Long])
    verify(crowdinClientMock, times(1)).addTargetLanguage(any[String])
    verify(translationDbService, times(1)).addTranslationWithFiles(any[domain.InTranslation], any[Seq[domain.InTranslationFile]], any[domain.Translation], any[TranslateRequest], any[TranslationStatus.Value])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("that addTranslation creates a new translation") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterWithId(any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(writeService.addInTransportMark(any[api.Book])).thenReturn(Success())
    when(writeService.removeInTransportMark(any[api.Book])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[domain.Translation], any[Seq[api.Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))
    when(writeService.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultTranslation))

    when(translationDbService.forOriginalIdWithToLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile.copy(fileType = domain.FileType.METADATA)))

    when(translationDbService.newTranslation(any[TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))
    when(synchronizeService.fetchPseudoFiles(any[InTranslation])).thenReturn(Success(TestData.Api.DefaultSynchronizeResponse))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success
  }

  test("that addTranslation filters out chapters of type license before sending to translation") {
    val crowdinClientMock = mock[CrowdinClient]

    val chapter1 = TestData.Domain.DefaultChapter.copy(id = Some(1), chapterType = ChapterType.Content)
    val chapter2 = TestData.Domain.DefaultChapter.copy(id = Some(2), chapterType = ChapterType.License)
    val chapters = Seq(chapter1, chapter2)

    val persistedNewTranslation = TestData.Domain.DefaultTranslation.copy(chapters = chapters)

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(writeService.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(persistedNewTranslation))
    when(readService.chapterWithId(chapter1.id.get)).thenReturn(Some(TestData.Api.Chapter1))

    when(writeService.addInTransportMark(any[api.Book])).thenReturn(Success())
    when(writeService.removeInTransportMark(any[api.Book])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[domain.Translation], any[Seq[api.Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))

    when(translationDbService.newTranslation(any[TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))
    when(synchronizeService.fetchPseudoFiles(any[InTranslation])).thenReturn(Success(TestData.Api.DefaultSynchronizeResponse))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success

    verify(readService).chapterWithId(chapter1.id.get)
    verify(readService, never()).chapterWithId(chapter2.id.get)
  }

  test("that addTranslate deletes directory in Crowdin when DB-updates fails") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterWithId(any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(writeService.addInTransportMark(any[api.Book])).thenReturn(Success())
    when(writeService.removeInTransportMark(any[api.Book])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[domain.Translation], any[Seq[api.Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))
    when(writeService.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultTranslation))

    when(crowdinClientMock.deleteDirectoryFor(any[domain.Translation])).thenReturn(Success())

    when(translationDbService.newTranslation(any[TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Failure(new api.DBException(new RuntimeException("Some message"))))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Failure

    verify(crowdinClientMock, times(1)).deleteDirectoryFor(any[domain.Translation])
    verify(writeService, times(1)).deleteTranslation(any[domain.Translation])
  }
}
