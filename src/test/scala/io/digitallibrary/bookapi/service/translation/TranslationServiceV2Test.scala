package io.digitallibrary.bookapi.service.translation

import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.integration.crowdin.CrowdinClient
import io.digitallibrary.bookapi.model.{api, crowdin, domain}
import io.digitallibrary.bookapi.model.domain.{ChapterType, InTranslation, TranslateRequest, TranslationStatus}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class TranslationServiceV2Test extends UnitSuite with TestEnvironment {

  val serviceV2 = new TranslationServiceV2

  override def beforeEach = {
    resetMocks()
  }

  test("v2: that addTranslation returns a failure if from-language is not supported") {
    when(readServiceV2.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("eng", "English")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Failure(new RuntimeException("Not supported fromLanguage")))

    val translateResponse = serviceV2.addTranslation(TranslateRequest(bookId = 1, fromLanguage = "nob", toLanguage = "eng", Some("userId")))
    translateResponse should be a 'Failure
    translateResponse.failed.get.getMessage should equal("Not supported fromLanguage")
  }

  test("v2: that addTranslation returns a failure if toLanguage is not supported") {
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("eng", "English")))

    val translateResponse = serviceV2.addTranslation(TranslateRequest(1, "nob", "fra", Some("userId")))
    translateResponse should be a 'Failure
    translateResponse.failed.get.asInstanceOf[api.ValidationException].errors.head.message should equal("The language 'fra' is not a supported language to translate to.")
  }

  test("v2: that addTranslation only adds a new user when the translation exist") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("nob"))
    val existingInTranslations = Seq(
      TestData.Domain.DefaultinTranslation.copy(id = Some(1), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh")),
      inTranslationToUpdate
    )

    val crowdinClientMock = mock[CrowdinClient]

    when(readServiceV2.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(existingInTranslations)
    when(translationDbService.addUserToTranslation(any[domain.InTranslation], any[String])).thenReturn(Success(inTranslationToUpdate))

    val response = serviceV2.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success

    verify(translationDbService).addUserToTranslation(any[domain.InTranslation], any[String])
    verify(writeServiceV2, never()).addTranslatorToTranslation(any[Long], any[domain.Person])
    verify(translationDbService).translationsForOriginalId(any[Long])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("v2: that addTranslation adds a new target language when fromLanguage exists") {
    val inTranslationToUpdate = TestData.Domain.DefaultinTranslation.copy(id = Some(2), fromLanguage = LanguageTag("eng"), toLanguage = LanguageTag("amh"))

    val crowdinClientMock = mock[CrowdinClient]

    when(readServiceV2.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))

    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq(inTranslationToUpdate))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(writeServiceV2.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultTranslation))
    when(translationDbService.addTranslationWithFiles(any[domain.InTranslation], any[Seq[domain.InTranslationFile]], any[domain.Translation], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(inTranslationToUpdate))

    val response = serviceV2.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success

    verify(translationDbService, times(1)).translationsForOriginalId(any[Long])
    verify(translationDbService, times(1)).filesForTranslation(any[Long])
    verify(crowdinClientMock, times(1)).addTargetLanguage(any[String])
    verify(translationDbService, times(1)).addTranslationWithFiles(any[domain.InTranslation], any[Seq[domain.InTranslationFile]], any[domain.Translation], any[TranslateRequest], any[TranslationStatus.Value])
    verifyNoMoreInteractions(crowdinClientMock)
    verifyNoMoreInteractions(translationDbService)
  }

  test("v2: that addTranslation creates a new translation") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readServiceV2.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readServiceV2.chapterWithId(any[Long])).thenReturn(Some(TestData.ApiV2.Chapter1))

    when(writeServiceV2.addInTransportMark(any[api.BookV2])).thenReturn(Success())
    when(writeServiceV2.removeInTransportMark(any[api.BookV2])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersForV2(any[domain.Translation], any[Seq[api.ChapterV2]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))
    when(writeServiceV2.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultTranslation))

    when(translationDbService.forOriginalIdWithToLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile.copy(fileType = domain.FileType.METADATA)))

    when(translationDbService.newTranslation(any[TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))
    when(synchronizeService.fetchPseudoFiles(any[InTranslation])).thenReturn(Success(TestData.ApiV2.DefaultSynchronizeResponse))

    val response = serviceV2.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success
  }

  test("v2: that addTranslation filters out chapters of type license before sending to translation") {
    val crowdinClientMock = mock[CrowdinClient]

    val chapter1 = TestData.Domain.DefaultChapter.copy(id = Some(1), chapterType = ChapterType.Content)
    val chapter2 = TestData.Domain.DefaultChapter.copy(id = Some(2), chapterType = ChapterType.License)
    val chapters = Seq(chapter1, chapter2)

    val persistedNewTranslation = TestData.Domain.DefaultTranslation.copy(chapters = chapters)

    when(readServiceV2.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(writeServiceV2.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(persistedNewTranslation))
    when(readServiceV2.chapterWithId(chapter1.id.get)).thenReturn(Some(TestData.ApiV2.Chapter1))

    when(writeServiceV2.addInTransportMark(any[api.BookV2])).thenReturn(Success())
    when(writeServiceV2.removeInTransportMark(any[api.BookV2])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersForV2(any[domain.Translation], any[Seq[api.ChapterV2]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))

    when(translationDbService.newTranslation(any[TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Success(TestData.Domain.DefaultinTranslation))
    when(synchronizeService.fetchPseudoFiles(any[InTranslation])).thenReturn(Success(TestData.Api.DefaultSynchronizeResponse))

    val response = serviceV2.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Success

    verify(readServiceV2).chapterWithId(chapter1.id.get)
    verify(readServiceV2, never()).chapterWithId(chapter2.id.get)
  }

  test("v2: that addTranslate deletes directory in Crowdin when DB-updates fails") {
    val crowdinClientMock = mock[CrowdinClient]

    when(readServiceV2.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))
    when(supportedLanguageService.getSupportedLanguages(any[Option[LanguageTag]])).thenReturn(Seq(api.Language("nob", "Norwegian Bokmål")))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(translationDbService.translationsForOriginalId(any[Long])).thenReturn(Seq())

    when(readServiceV2.chapterWithId(any[Long])).thenReturn(Some(TestData.ApiV2.Chapter1))

    when(writeServiceV2.addInTransportMark(any[api.BookV2])).thenReturn(Success())
    when(writeServiceV2.removeInTransportMark(any[api.BookV2])).thenReturn(Success())
    when(crowdinClientMock.addTargetLanguage(any[String])).thenReturn(Success())
    when(crowdinClientMock.addDirectoryFor(any[domain.Translation])).thenReturn(Success("created-directory"))
    when(crowdinClientMock.addBookMetadata(any[domain.Translation])).thenReturn(Success(TestData.Crowdin.DefaultMetadataCrowdinFile))
    when(crowdinClientMock.addChaptersForV2(any[domain.Translation], any[Seq[api.ChapterV2]])).thenReturn(Success(Seq(TestData.Crowdin.DefaultContentCrowdinFile)))
    when(writeServiceV2.newTranslationForBook(any[Long], any[LanguageTag], any[TranslateRequest], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultTranslation))

    when(crowdinClientMock.deleteDirectoryFor(any[domain.Translation])).thenReturn(Success())

    when(translationDbService.newTranslation(any[TranslateRequest], any[domain.Translation], any[crowdin.CrowdinFile], any[Seq[crowdin.CrowdinFile]], any[String])).thenReturn(Failure(new api.DBException(new RuntimeException("Some message"))))

    val response = serviceV2.addTranslation(TranslateRequest(1, "eng", "nob", Some("userId")))
    response should be a 'Failure

    verify(crowdinClientMock, times(1)).deleteDirectoryFor(any[domain.Translation])
    verify(writeServiceV2, times(1)).deleteTranslation(any[domain.Translation])
  }
}

