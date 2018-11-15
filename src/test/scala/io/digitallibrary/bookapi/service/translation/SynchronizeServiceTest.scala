package io.digitallibrary.bookapi.service.translation

import io.digitallibrary.bookapi.integration.crowdin.{CrowdinClient, TranslatedChapter}
import io.digitallibrary.bookapi.model.domain
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.mockito.Mockito.{never, verify, when}
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class SynchronizeServiceTest extends UnitSuite with TestEnvironment {

  val service = new SynchronizeService

  override def beforeEach: Unit = {
    resetMocks()
  }

  test ("that fetchUpdates returns Success and fetches updates for all files in a translation") {
    val crowdinClientMock = mock[CrowdinClient]
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile))

    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedChapter(any[InTranslationFile], any[String])).thenReturn(Success(TestData.Crowdin.DefaultTranslatedChapter))
    when(chapterRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(mergeService.mergeChapter(any[Chapter], any[TranslatedChapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(chapterRepository.updateChapter(any[Chapter])(any[DBSession])).thenReturn(TestData.Domain.DefaultChapter)
    when(translationDbService.updateTranslationStatus(any[InTranslationFile], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))

    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation)
    result should be a 'Success
  }

  test ("that fetchUpdates returns Failure when original translation is not found") {
    val crowdinClientMock = mock[CrowdinClient]
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile))

    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedChapter(any[InTranslationFile], any[String])).thenReturn(Success(TestData.Crowdin.DefaultTranslatedChapter))
    when(chapterRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(mergeService.mergeChapter(any[Chapter], any[TranslatedChapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(chapterRepository.updateChapter(any[Chapter])(any[DBSession])).thenReturn(TestData.Domain.DefaultChapter)
    when(translationDbService.updateTranslationStatus(any[InTranslationFile], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))

    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(None)

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation)
    result should be a 'Failure
  }

  test ("that fetchUpdates returns Failure when fetching a file fails") {
    val crowdinClientMock = mock[CrowdinClient]
    when(translationDbService.filesForTranslation(any[Long])).thenReturn(Seq(TestData.Domain.DefaultInTranslationFile))
    when(translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(any[String], any[String], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultInTranslationFile))

    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(crowdinClientBuilder.forSourceLanguage(any[LanguageTag])).thenReturn(Success(crowdinClientMock))
    when(crowdinClientMock.fetchTranslatedChapter(any[InTranslationFile], any[String])).thenReturn(Failure(new RuntimeException("Failed")))

    val result = service.fetchUpdatesFor(TestData.Domain.DefaultinTranslation)
    result should be a 'Failure
  }

  test("that fetchTranslatedFile returns Failure when no translation files for any languages are found") {
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
    when(crowdinClientMock.fetchTranslatedChapter(any[domain.InTranslationFile], any[String])).thenReturn(Success(TestData.Crowdin.DefaultTranslatedChapter))
    when(chapterRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(mergeService.mergeChapter(any[domain.Chapter], any[TranslatedChapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(chapterRepository.updateChapter(any[domain.Chapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(translationDbService.updateTranslationStatus(any[domain.InTranslationFile], any[domain.TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultInTranslationFile))

    when(translationService.addTargetLanguageForTranslation(any[InTranslation], any[TranslateRequest], any[Long], any[LanguageTag], any[CrowdinClient], any[TranslationStatus.Value])).thenReturn(Success(TestData.Domain.DefaultinTranslation))

    val result = service.fetchTranslatedFile("abc", "es", "file-id", domain.TranslationStatus.TRANSLATED)
    result should be a 'Success

    verify(translationService).addTargetLanguageForTranslation(any[InTranslation], any[TranslateRequest], any[Long], any[LanguageTag], any[CrowdinClient], any[TranslationStatus.Value])
  }

  test("that extractContributors adds new contributors and keeps existing contributors to translation") {

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
    translation.contributors.toSet should equal(Set(existingContributor1, existingContributor2, addedContributor))
  }

  test("that markTranslationAs returns a Failure when the file does not belong to a translation registered with crowdin") {
    when(translationDbService.translationWithId(any[Long])).thenReturn(None)
    service.markTranslationAs(TestData.Domain.DefaultInTranslationFile, TranslationStatus.PROOFREAD) should be a 'Failure
  }

  test("that markTranslationAs returns a Failure when a the new translation is not found") {
    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(None)

    service.markTranslationAs(TestData.Domain.DefaultInTranslationFile, TranslationStatus.PROOFREAD) should be a 'Failure
  }

  test("that markTranslationAs returns a Success when all data is found") {
    when(translationDbService.translationWithId(any[Long])).thenReturn(Some(TestData.Domain.DefaultinTranslation))
    when(unFlaggedTranslationsRepository.withId(any[Long])(any[DBSession])).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(unFlaggedTranslationsRepository.updateTranslation(any[domain.Translation])(any[DBSession])).thenReturn(TestData.Domain.DefaultTranslation)

    service.markTranslationAs(TestData.Domain.DefaultInTranslationFile, TranslationStatus.PROOFREAD) should be a 'Success
  }
}
