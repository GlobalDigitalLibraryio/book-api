/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.integration.crowdin.{CrowdinClient, TranslatedChapter}
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.domain.{ChapterType, ContributorType, Person}
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchers.{eq => eqTo}
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
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(api.Language("eng", "English")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Failure(new RuntimeException("Not supported fromLanguage")))

    val translateResponse = service.addTranslation(api.TranslateRequest(bookId = 1, fromLanguage = "nob", toLanguage = "eng"))
    translateResponse should be a 'Failure
    translateResponse.failed.get.getMessage should equal("Not supported fromLanguage")
  }

  test("that addTranslation returns a failure if toLanguage is not supported") {
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(api.Language("eng", "English")))

    val translateResponse = service.addTranslation(api.TranslateRequest(1, "nob", "fra"))
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

    when(writeService.addPersonFromAuthUser()).thenReturn(TestData.Domain.DefaultPerson)
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(existingInTranslations)
    when(translationDbService.addUserToTranslation(any[domain.InTranslation], any[domain.Person])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(api.TranslateRequest(1, "eng", "nob"))
    response should be a 'Success

    verify(translationDbService).addUserToTranslation(any[domain.InTranslation], any[domain.Person])
    verify(writeService, never()).addTranslatorToTranslation(any[Long], any[domain.Person])
    verify(translationDbService).translationsForOriginalId(any[Long])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("that addTranslation adds a new target language when fromLanguage exists") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh"))

    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(writeService.addPersonFromAuthUser()).thenReturn(TestData.Domain.DefaultPerson)
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq(inTranslationToUpdate))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(writeService.newTranslationForBook(any[api.Book], any[api.TranslateRequest])).thenReturn(Success(TestData.Domain.DefaultTranslation))
    when(translationDbService.addTranslationWithFiles(any[domain.InTranslation], any[Seq[domain.InTranslationFile]], any[domain.Translation], any[api.TranslateRequest])).thenReturn(Success(inTranslationToUpdate))

    val response = service.addTranslation(api.TranslateRequest(1, "eng", "nob"))
    response should be a 'Success

    verify(translationDbService, times(1)).translationsForOriginalId(any[Long])
    verify(translationDbService, times(1)).filesForTranslation(any[Long])
    verify(crowdinClientMock, times(1)).addTargetLanguage(any[String])
    verify(translationDbService, times(1)).addTranslationWithFiles(any[domain.InTranslation], any[Seq[domain.InTranslationFile]], any[domain.Translation], any[api.TranslateRequest])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("that addTranslation creates a new translation") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(writeService.addPersonFromAuthUser()).thenReturn(TestData.Domain.DefaultPerson)
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterWithId(any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(writeService.addInTransportMark(any[api.Book])).thenReturn(Success())
    when(writeService.removeInTransportMark(any[api.Book])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[domain.Translation], any[Seq[api.Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))
    when(writeService.newTranslationForBook(any[api.Book], any[api.TranslateRequest])).thenReturn(Success(TestData.Domain.DefaultTranslation))

    when(translationDbService.newTranslation(any[api.TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))

    val response = service.addTranslation(api.TranslateRequest(1, "eng", "nob"))
    response should be a 'Success
  }

  test("that addTranslation filters out chapters of type license before sending to translation") {
    val crowdinClientMock = mock[CrowdinClient]

    val chapter1 = TestData.Domain.DefaultChapter.copy(id = Some(1), chapterType = ChapterType.Content)
    val chapter2 = TestData.Domain.DefaultChapter.copy(id = Some(2), chapterType = ChapterType.License)
    val chapters = Seq(chapter1, chapter2)

    val persistedNewTranslation = TestData.Domain.DefaultTranslation.copy(chapters = chapters)

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(writeService.addPersonFromAuthUser()).thenReturn(TestData.Domain.DefaultPerson)
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(writeService.newTranslationForBook(any[api.Book], any[api.TranslateRequest])).thenReturn(Success(persistedNewTranslation))
    when(readService.chapterWithId(chapter1.id.get)).thenReturn(Some(TestData.Api.Chapter1))

    when(writeService.addInTransportMark(any[api.Book])).thenReturn(Success())
    when(writeService.removeInTransportMark(any[api.Book])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[domain.Translation], any[Seq[api.Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))

    when(translationDbService.newTranslation(any[api.TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))

    val response = service.addTranslation(api.TranslateRequest(1, "eng", "nob"))
    response should be a 'Success

    verify(readService).chapterWithId(chapter1.id.get)
    verify(readService, never()).chapterWithId(chapter2.id.get)
  }

  test("that addTranslate deletes directory in Crowdin when DB-updates fails") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(writeService.addPersonFromAuthUser()).thenReturn(TestData.Domain.DefaultPerson)
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readService.chapterWithId(any[Long])).thenReturn(Some(TestData.Api.Chapter1))

    when(writeService.addInTransportMark(any[api.Book])).thenReturn(Success())
    when(writeService.removeInTransportMark(any[api.Book])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersFor(any[domain.Translation], any[Seq[api.Chapter]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))
    when(writeService.newTranslationForBook(any[api.Book], any[api.TranslateRequest])).thenReturn(Success(TestData.Domain.DefaultTranslation))

    when(crowdinClientMock.deleteDirectoryFor(any[domain.Translation])).thenReturn(Success())

    when(translationDbService.newTranslation(any[api.TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Failure(new api.DBException(new RuntimeException("Some message"))))

    val response = service.addTranslation(api.TranslateRequest(1, "eng", "nob"))
    response should be a 'Failure

    verify(crowdinClientMock, times(1)).deleteDirectoryFor(any[domain.Translation])
    verify(writeService, times(1)).deleteTranslation(any[domain.Translation])
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
    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(None)

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation.copy(newTranslationId = Some(1)))

    result should be a 'Failure
    result.failed.get.getMessage contains "The translated book with id" should be (true)
  }

  test("that fetchUpdatesFor returns Failure when metadata is not found") {
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(mock[CrowdinClient]))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq())

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation.copy(newTranslationId = Some(1)))

    result should be a 'Failure
    result.failed.get.getMessage contains "No metadata for translation with id" should be (true)
  }

  test("that fetchTranslatedFile returns Failure when no translation files for any languages are not found") {
    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(None)
    when(translationDbService.fileForCrowdinProjectWithFileId(any[String], any[String])).thenReturn(Seq())
    val result = service.fetchTranslatedFile("abc", "nb", "file-id", domain.TranslationStatus.TRANSLATED)
    result should be a 'Failure
  }

  test("that fetchTranslatedFile returns Failure when fetchTranslatedChapter fails") {
    val crowdinClientMock = mock[CrowdinClient]

    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile.copy(fileType = domain.FileType.CONTENT)))
    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedChapter(any[domain.InTranslationFile], any[String])).thenReturn(Failure(new RuntimeException("this failed")))

    val result = service.fetchTranslatedFile("abc", "nb", "file-id", domain.TranslationStatus.TRANSLATED)
    result should be a 'Failure

    verify(chapterRepository, never()).updateChapter(any[domain.Chapter])
    verify(translationDbService, never()).updateTranslationStatus(any[domain.InTranslationFile], any[domain.TranslationStatus.Value])

  }

  test("that fetchTranslatedFile returns Success when retrieving chapter is ok") {
    val crowdinClientMock = mock[CrowdinClient]

    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile.copy(fileType = domain.FileType.CONTENT)))
    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedChapter(any[domain.InTranslationFile], any[String])).thenReturn(Success(TestData.Crowdin.DefaultTranslatedChapter))
    when(chapterRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(mergeService.mergeChapter(any[domain.Chapter], any[TranslatedChapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(chapterRepository.updateChapter(any[domain.Chapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(translationDbService.updateTranslationStatus(any[domain.InTranslationFile], any[domain.TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))

    val result = service.fetchTranslatedFile("abc", "nb", "file-id", domain.TranslationStatus.TRANSLATED)
    result should be a 'Success
  }

  test("that fetchTranslatedFile returns Failure when fetchTranslatedMetadata fails") {
    val crowdinClientMock = mock[CrowdinClient]

    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile.copy(fileType = domain.FileType.METADATA)))
    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedMetaData(any[domain.InTranslationFile], any[String])).thenReturn(Failure(new RuntimeException("This failed")))

    val result = service.fetchTranslatedFile("abc", "nb", "file-id", domain.TranslationStatus.TRANSLATED)
    result should be a 'Failure

    verify(unFlaggedTranslationsRepository, never()).updateTranslation(any[domain.Translation])
    verify(translationDbService, never()).updateTranslationStatus(any[domain.InTranslationFile], any[domain.TranslationStatus.Value])

  }

  test("that fetchTranslatedFile returns Success when retrieving metadata is ok") {
    val crowdinClientMock = mock[CrowdinClient]

    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile.copy(fileType = domain.FileType.METADATA)))
    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedMetaData(any[domain.InTranslationFile], any[String])).thenReturn(Success(TestData.Crowdin.DefaultBookMetaData))
    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))

    when(unFlaggedTranslationsRepository.updateTranslation(any[domain.Translation])(any[DBSession])).thenReturn(TestData.Domain.DefaultTranslation)
    when(translationDbService.updateTranslationStatus(any[domain.InTranslationFile], any[domain.TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))

    val result = service.fetchTranslatedFile("abc", "nb", "file-id", domain.TranslationStatus.TRANSLATED)
    result should be a 'Success
  }

  test("that fetchTranslatedFile creates a new translation when the targetLanguage is different from initiated language") {
    val crowdinClientMock = mock[CrowdinClient]

    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag]))
      .thenReturn(None) //First iteration
      .thenReturn(Some(TestData.Domain.DefaultInTranslationFile)) // Second iteration

    when(translationDbService.fileForCrowdinProjectWithFileId(any[String], any[String])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(writeService.newTranslationForBook(any[api.Book], any[api.TranslateRequest])).thenReturn(Success(TestData.Domain.DefaultTranslation))

    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("nob"))
    when(translationDbService.addTranslationWithFiles(any[domain.InTranslation], any[Seq[domain.InTranslationFile]], any[domain.Translation], any[api.TranslateRequest])).thenReturn(Success(inTranslationToUpdate))
    when(crowdinClientMock.fetchTranslatedChapter(any[domain.InTranslationFile], any[String])).thenReturn(Success(TestData.Crowdin.DefaultTranslatedChapter))
    when(chapterRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(mergeService.mergeChapter(any[domain.Chapter], any[TranslatedChapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(chapterRepository.updateChapter(any[domain.Chapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(translationDbService.updateTranslationStatus(any[domain.InTranslationFile], any[domain.TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))

    val result = service.fetchTranslatedFile("abc", "es", "file-id", domain.TranslationStatus.TRANSLATED)
    result should be a 'Success
  }

  test("that exctractContributors adds new contributors and keeps existing contributors to translation") {

    val existingContributor1 = TestData.Domain.DefaultContributor.copy(id = Some(1), personId = 1, person = Person(Some(1), Some(1), "Translator 1", None), `type` = ContributorType.Translator)
    val existingContributor2 = TestData.Domain.DefaultContributor.copy(id = Some(2), personId = 2, person = Person(Some(2), Some(1), "Translator 2", None), `type` = ContributorType.Translator)
    val existingTranslation = TestData.Domain.DefaultTranslation.copy(contributors = Seq(existingContributor1, existingContributor2))

    val person1 = existingContributor1.person //already exists on translation, and should be kept
    val person3 = Person(Some(3), Some(1), "Translator 3", None) // new and should be added
    val addedContributor = domain.Contributor(Some(3), Some(1), person3.id.get, existingTranslation.id.get, ContributorType.Translator, person3)
    val bookMetadata = TestData.Crowdin.DefaultBookMetaData.copy(translators = Some("Translator 1, Translator 3"))

    when(writeService.addPerson(any[String]))
      .thenReturn(person1)
      .thenReturn(person3)

    when(writeService.addTranslatorToTranslation(eqTo(existingTranslation.id.get), eqTo(person3))).thenReturn(addedContributor)

    val translation = service.extractContributors(bookMetadata, existingTranslation)
    translation.contributors.size should be (3)
    translation.contributors.contains(existingContributor1) should be (true)
    translation.contributors.contains(existingContributor2) should be (true)
    translation.contributors.contains(addedContributor) should be (true)
  }
}
