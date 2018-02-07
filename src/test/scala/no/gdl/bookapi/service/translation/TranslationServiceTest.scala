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
import no.gdl.bookapi.model.domain.{FileType, InTranslation, InTranslationFile, TranslationStatus}
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
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
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("eng", "English")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Failure(new RuntimeException("Not supported fromLanguage")))

    val translateResponse = service.addTranslation(TranslateRequest(bookId = 1, fromLanguage = "nob", toLanguage = "eng"))
    translateResponse should be a 'Failure
    translateResponse.failed.get.getMessage should equal("Not supported fromLanguage")
  }

  test("that addTranslation returns a failure if toLanguage is not supported") {
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("eng", "English")))

    val translateResponse = service.addTranslation(TranslateRequest(1, "nob", "fra"))
    translateResponse should be a 'Failure
    translateResponse.failed.get.asInstanceOf[ValidationException].errors.head.message should equal("The language 'fra' is not a supported language to translate to.")
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

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(existingInTranslations)
    when(translationDbService.addUserToTranslation(any[InTranslation])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Success

    verify(translationDbService).addUserToTranslation(any[InTranslation])
    verify(translationDbService).translationsForOriginalId(any[Long])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("that addTranslation adds a new target language when fromLanguage exists") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh"))

    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq(inTranslationToUpdate))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(translationDbService.addTranslationWithFiles(any[InTranslation], any[Seq[InTranslationFile]], any[TranslateRequest])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Success

    verify(translationDbService, times(1)).translationsForOriginalId(any[Long])
    verify(translationDbService, times(1)).filesForTranslation(any[Long])
    verify(crowdinClientMock, times(1)).addTargetLanguage(any[String])
    verify(translationDbService, times(1)).addTranslationWithFiles(any[InTranslation], any[Seq[InTranslationFile]], any[TranslateRequest])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("that addTranslation creates a new translation") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[Book])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[Book])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[Book], any[Seq[Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))

    when(translationDbService.newTranslation(any[TranslateRequest], any[CrowdinFile], any[Seq[CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Success
  }


  test("that addTranslate deletes directory in Crowdin when DB-updates fails") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[Book])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[Book])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[Book], any[Seq[Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))

    when(crowdinClientMock.deleteDirectoryFor(any[Book])).thenReturn(Success())

    when(translationDbService.newTranslation(any[TranslateRequest], any[CrowdinFile], any[Seq[CrowdinFile]], any[String])).thenReturn(Failure(new DBException(new RuntimeException("Some message"))))

    val response = service.addTranslation(TranslateRequest(1, "eng", "nob"))
    response should be a 'Failure

    verify(crowdinClientMock, times(1)).deleteDirectoryFor(any[Book])
  }

  test("that updateTranslationStatus returns a Failure when file not found") {
    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(None)
    val result = service.updateTranslationStatus("abc", LanguageTag("nob"), "123", TranslationStatus.TRANSLATED)
    result should be a 'Failure
    result.failed.get.getMessage should equal (s"No translation for project abc, language nob and file_id 123")
  }

  test("that updateTranslationStatus returns a Success when update ok") {
    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile))
    when(translationDbService.updateTranslationStatus(any[InTranslationFile], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))
    val result = service.updateTranslationStatus("abc", LanguageTag("nob"), "123", TranslationStatus.TRANSLATED)
    result should be a 'Success
  }

  test("that fetchTranslationsIfAllTranslated returns Failure when not all translated") {
    val translationFiles = Seq(
      TestData.Domain.DefaultInTranslationFile.copy(id = Some(1), translationStatus = TranslationStatus.IN_PROGRESS),
      TestData.Domain.DefaultInTranslationFile.copy(id = Some(2), translationStatus = TranslationStatus.TRANSLATED)
    )

    val result = service.fetchTranslations(TestData.Domain.DefaultInTranslationFile, translationFiles)
    result should be a 'Failure
    result.failed.get.getMessage contains "Not all files for translation are finished translating" should be (true)
  }

  test("that fetchTranslationsIfAllTranslated returns Failure when InTranslation not found") {
    val translationFiles = Seq(TestData.Domain.DefaultInTranslationFile.copy(id = Some(1), translationStatus = TranslationStatus.TRANSLATED))
    when(translationDbService.translationWithId(any[Long])).thenReturn(None)

    val result = service.fetchTranslations(TestData.Domain.DefaultInTranslationFile, translationFiles)
    result should be a 'Failure
    result.failed.get.getMessage contains "InTranslation with id" should be (true)
  }

  test("that fetchTranslationsIfAllTranslated returns Failure when original cannot be found") {
    val translationFiles = Seq(
      TestData.Domain.DefaultInTranslationFile.copy(id = Some(1), translationStatus = TranslationStatus.TRANSLATED))

    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(None)

    val result = service.fetchTranslations(TestData.Domain.DefaultInTranslationFile, translationFiles)
    result should be a 'Failure
    result.failed.get.getMessage contains "The original book with id" should be (true)
  }

  test("that fetchTranslationsIfAllTranslated returns Failure when missing metadata") {
    val translationFiles = Seq(
      TestData.Domain.DefaultInTranslationFile.copy(id = Some(1), translationStatus = TranslationStatus.TRANSLATED, fileType = FileType.CONTENT))

    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(mock[CrowdinClient]))

    val result = service.fetchTranslations(TestData.Domain.DefaultInTranslationFile, translationFiles)
    result should be a 'Failure
    result.failed.get.getMessage contains "No metadata for translation with id" should be (true)
  }

  test("that fetchTranslationsIfAllTranslated returns Failure when metadata cannot be fetched from crowdin") {
    val translationFiles = Seq(
      TestData.Domain.DefaultInTranslationFile.copy(id = Some(1), translationStatus = TranslationStatus.TRANSLATED, fileType = FileType.METADATA))

    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))

    val crowdinClientMock = mock[CrowdinClient]
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedMetaData(any[InTranslationFile], any[String])).thenReturn(Failure(new RuntimeException("Provoked error from Crowdin")))

    val result = service.fetchTranslations(TestData.Domain.DefaultInTranslationFile, translationFiles)
    result should be a 'Failure
    result.failed.get.getMessage should equal ("Provoked error from Crowdin")
  }

  test("that fetchTranslationsIfAllTranslated returns Failure when a chapter cannot be fetched from crowdin") {
    val translationFiles = Seq(
      TestData.Domain.DefaultInTranslationFile.copy(id = Some(1), translationStatus = TranslationStatus.TRANSLATED, fileType = FileType.METADATA),
      TestData.Domain.DefaultInTranslationFile.copy(id = Some(2), translationStatus = TranslationStatus.TRANSLATED, fileType = FileType.CONTENT)
    )

    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))

    val crowdinClientMock = mock[CrowdinClient]
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedMetaData(any[InTranslationFile], any[String])).thenReturn(Success(TestData.Crowdin.DefaultBookMetaData))
    when(crowdinClientMock.fetchTranslatedChapter(any[InTranslationFile], any[String])).thenReturn(Failure(new RuntimeException("Any error")))

    val result = service.fetchTranslations(TestData.Domain.DefaultInTranslationFile, translationFiles)
    result should be a 'Failure
    result.failed.get.getMessage should equal ("Received Crowdin Exceptions")
  }

  test("that fetchUpdatesFor returns Failure when book has not been translated yet") {
    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation.copy(newTranslationId = None))
    result should be a 'Failure
    result.failed.get.getMessage contains "has not yet been translated. Cannot fetch updates." should be (true)
  }

  test("that fetchUpdatesFor returns Failure when original book is not found") {
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(mock[CrowdinClient]))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(None)

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation.copy(newTranslationId = Some(1)))

    result should be a 'Failure
    result.failed.get.getMessage contains "The original book with id" should be (true)
  }

  test("that fetchUpdatesFor returns Failure when the new translation is not found") {
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(mock[CrowdinClient]))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(translationRepository.withId(any[Long])(any[DBSession])).thenReturn(None)

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation.copy(newTranslationId = Some(1)))

    result should be a 'Failure
    result.failed.get.getMessage contains "The translated book with id" should be (true)
  }

  test("that fetchUpdatesFor returns Failure when metadata is not found") {
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(mock[CrowdinClient]))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(translationRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq())

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation.copy(newTranslationId = Some(1)))

    result should be a 'Failure
    result.failed.get.getMessage contains "No metadata for translation with id" should be (true)
  }
}
