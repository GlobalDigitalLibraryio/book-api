package io.digitallibrary.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.CrowdinTranslatorPlaceHolder
import io.digitallibrary.bookapi.integration.crowdin.{BookMetaData, CrowdinClientBuilder, CrowdinUtils}
import io.digitallibrary.bookapi.model.api.{Book, NotFoundException, SynchronizeResponse}
import io.digitallibrary.bookapi.model.domain
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.repository.{ChapterRepository, TransactionHandler, TranslationRepository}
import io.digitallibrary.bookapi.service.{ReadService, WriteService}
import io.digitallibrary.language.model.LanguageTag

import scala.util.{Failure, Success, Try}

trait SynchronizeService {
  this: CrowdinClientBuilder with TranslationDbService with TransactionHandler with WriteService with MergeService with ChapterRepository with TranslationRepository with ReadService with TranslationService =>
  val synchronizeService: SynchronizeService

  class SynchronizeService extends LazyLogging {

    def fetchUpdatesFor(inTranslation: InTranslation): Try[SynchronizeResponse] = {
      translationDbService.filesForTranslation(inTranslation.id.get)
        .map(file => fetchTranslatedFile(
          inTranslation.crowdinProjectId, inTranslation.crowdinToLanguage, file.crowdinFileId, file.translationStatus))
        .filter(_.isFailure).map(_.failed.get) match {
        case err :: _ => Failure(err)
        case _ => for {
          existingTranslation <- newTranslationId(inTranslation).flatMap(existingTranslation)
          originalBook <- originalBook(inTranslation)
        } yield SynchronizeResponse(existingTranslation.bookId, CrowdinUtils.crowdinUrlToBook(originalBook, inTranslation.crowdinProjectId, inTranslation.toLanguage.toString()))
      }
    }

    def fetchTranslatedFile(projectIdentifier: String, crowdinToLanguage: String, fileId: String, status: TranslationStatus.Value): Try[InTranslationFile] = {
      val toLanguage = LanguageTag(crowdinToLanguage)
      if (translationDbService.checkIfBookCanBeUpdated(fileId, toLanguage)) {
        Failure(new RuntimeException("Won't update government approved files"))
      } else {
        translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(projectIdentifier, fileId, toLanguage) match {
          case None => {
            val anInTranslationOpt = translationDbService.fileForCrowdinProjectWithFileId(projectIdentifier, fileId)
              .headOption
              .flatMap(x => translationDbService.translationWithId(x.inTranslationId))

            anInTranslationOpt match {
              case None => Failure(new RuntimeException(s"No translation for project $projectIdentifier and file_id $fileId"))
              case Some(inTranslation) => for {
                crowdinClient <- crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage)
                original <- originalBook(inTranslation)
                addedTranslation <- translationService.addTargetLanguageForTranslation(inTranslation, domain.TranslateRequest(inTranslation.originalTranslationId, inTranslation.fromLanguage.toString, crowdinToLanguage, None), original.id, LanguageTag(original.language.code), crowdinClient)
                translatedFile <- fetchTranslatedFile(projectIdentifier, crowdinToLanguage, fileId, status)
              } yield translatedFile
            }
          }
          case Some(file) if file.fileType == FileType.CONTENT => {
            for {
              inTranslation             <- Try(translationDbService.translationWithId(file.inTranslationId).get)
              crowdinClient             <- crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage  )
              translatedChapter         <- crowdinClient.fetchTranslatedChapter(file, crowdinToLanguage)
              originalChapter           <- Try(chapterRepository.withId(file.newChapterId.get).get)
              mergedChapter             <- Try(mergeService.mergeChapter(originalChapter, translatedChapter))
              _                         <- Try(chapterRepository.updateChapter(mergedChapter))
              updatedInTranslationFile  <- translationDbService.updateTranslationStatus(file, status)
            } yield updatedInTranslationFile
          }
          case Some(file) if file.fileType == FileType.METADATA => {
            for {
              inTranslation             <- Try(translationDbService.translationWithId(file.inTranslationId).get)
              crowdinClient             <- crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage)
              translatedMetadata        <- crowdinClient.fetchTranslatedMetaData(file, crowdinToLanguage)
              newTranslation            <- Try(unFlaggedTranslationsRepository.withId(inTranslation.newTranslationId.get).get)
              newTranslationWithContrib <- Try(extractContributors(translatedMetadata, newTranslation))
              originalChapter           <- Try(unFlaggedTranslationsRepository.updateTranslation(newTranslationWithContrib.copy(title = translatedMetadata.title, about = translatedMetadata.description)))
              updatedInTranslationFile  <- translationDbService.updateTranslationStatus(file, status)
            } yield updatedInTranslationFile
          }
        }
      }
    }

    def fetchPseudoFiles(originalInTranslation: InTranslation): Try[SynchronizeResponse] = {
      for {
        crowdinClient <- crowdinClientBuilder.forSourceLanguage(originalInTranslation.fromLanguage)
        pseudoInTranslation <- translationDbService.forOriginalIdWithToLanguage(originalInTranslation.originalTranslationId, BookApiProperties.CrowdinPseudoLanguage) match {
          case Some(x) => Success(x)
          case None => translationService.addTargetLanguageForTranslation(
            originalInTranslation,
            domain.TranslateRequest(originalInTranslation.originalTranslationId, originalInTranslation.fromLanguage.toString(), BookApiProperties.CrowdinPseudoLanguage.toString(), None),
            originalInTranslation.originalTranslationId,
            originalInTranslation.fromLanguage,
            crowdinClient,
            TranslationStatus.PSEUDO)
        }
        response <- fetchUpdatesFor(pseudoInTranslation)

      } yield response
    }

    def markTranslationAs(inTranslationFile: InTranslationFile, status: TranslationStatus.Value): Try[Translation] = {
      val updateTranslation = translationDbService.translationWithId(inTranslationFile.inTranslationId)
        .flatMap(_.newTranslationId)
        .flatMap(translationId => unFlaggedTranslationsRepository.withId(translationId))
        .map(translation => unFlaggedTranslationsRepository.updateTranslation(translation.copy(translationStatus = Some(status))))

      updateTranslation match {
        case Some(translation) => Success(translation)
        case None => Failure(new NotFoundException(s"No translation for crowdin file ${inTranslationFile.crowdinFileId} found."))
      }
    }

    def allFilesHaveTranslationStatusGreatherOrEqualTo(inTranslationFile: InTranslationFile, status: TranslationStatus.Value): Boolean = {
      translationDbService.filesForTranslation(inTranslationFile.inTranslationId).forall(_.translationStatus.id >= status.id)
    }

    private def newTranslationId(inTranslation: InTranslation): Try[Long] = {
      inTranslation.newTranslationId match {
        case None => Failure(new RuntimeException(s"The book for ${inTranslation.originalTranslationId} has not yet been translated. Cannot fetch updates."))
        case Some(newTranslationId) => Success(newTranslationId)
      }
    }

    private def originalBook(inTranslation: InTranslation): Try[Book] = {
      readService.withIdAndLanguage(inTranslation.originalTranslationId, inTranslation.fromLanguage) match {
        case None => Failure(new RuntimeException(s"The original book with id ${inTranslation.originalTranslationId} was not found. Cannot fetch updates."))
        case Some(originalBook) => Success(originalBook)
      }
    }

    private def existingTranslation(translationId: Long): Try[domain.Translation] = {
      unFlaggedTranslationsRepository.withId(translationId) match {
        case None => Failure(new RuntimeException(s"The translated book with id $translationId was not found. Cannot fetch updates."))
        case Some(existingTranslation) => Success(existingTranslation)
      }
    }


    def extractContributors(bookMetaData: BookMetaData, translation: Translation): Translation = {
      bookMetaData.translators match {
        case None => translation
        case Some(translators) => {
          val persons = translators.replace(CrowdinTranslatorPlaceHolder, "").split(",").filter(_.nonEmpty).map(_.trim).map(writeService.addPerson)
          val existingTranslators = translation.contributors.filter(_.`type` == ContributorType.Translator)
          val added = persons.filterNot(p => existingTranslators.exists(_.person.id == p.id))
            .map(person => writeService.addTranslatorToTranslation(translation.id.get, person))

          translation.copy(contributors = existingTranslators ++ added)
        }
      }
    }
  }
}
