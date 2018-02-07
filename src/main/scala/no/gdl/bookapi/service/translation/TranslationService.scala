/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.crowdin._
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api._
import no.gdl.bookapi.model.api.internal.NewTranslatedChapter
import no.gdl.bookapi.model.domain.{FileType, InTranslation, InTranslationFile, TranslationStatus}
import no.gdl.bookapi.repository.{TransactionHandler, TranslationRepository}
import no.gdl.bookapi.service.{ReadService, WriteService}

import scala.util.{Failure, Success, Try}

trait TranslationService {
  this: CrowdinClientBuilder with ReadService with WriteService with SupportedLanguageService with TranslationDbService with MergeService with TranslationRepository with TransactionHandler =>
  val translationService: TranslationService


  class TranslationService extends LazyLogging {
    def allFilesTranslated(inTranslationFile: InTranslationFile): Boolean = {
      val translationFiles = translationDbService.filesForTranslation(inTranslationFile.inTranslationId)
      translationFiles.forall(_.translationStatus == TranslationStatus.TRANSLATED)
    }

    def fetchUpdatesFor(inTranslation: InTranslation): Try[SynchronizeResponse] = {
      for {
        newTranslationId <- newTranslationId(inTranslation)
        crowdinClient       <- crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage)
        originalBook        <- originalBook(inTranslation)
        existingTranslation <- existingTranslation(newTranslationId)
        translationFiles    <- Try(translationDbService.filesForTranslation(inTranslation.id.get))
        metadataFile        <- findMetadataFile(inTranslation, translationFiles)
        translatedMetadata  <- crowdinClient.fetchTranslatedMetaData(metadataFile, inTranslation.crowdinToLanguage)
        chapterFiles        <- findChapterFiles(inTranslation, translationFiles)
        translatedChapters  <- Try(chapterFiles.map(chapter => crowdinClient.fetchTranslatedChapter(chapter, inTranslation.crowdinToLanguage)))
        persisted           <- inTransaction { implicit session =>
          for {
            persisted       <- Try(writeService.updateTranslation(existingTranslation.copy(title = translatedMetadata.title, about = translatedMetadata.description)))
            mergedChapters  <- Try(mergeService.mergeChapters(persisted, translatedChapters.filter(_.isSuccess).map(_.get)))
            _               <- Try(mergedChapters.map(ch => writeService.updateChapter(ch)))
          } yield persisted
        }
      } yield SynchronizeResponse(persisted.bookId, CrowdinUtils.crowdinUrlToBook(originalBook, crowdinClient.getProjectIdentifier, inTranslation.crowdinToLanguage))
    }

    def fetchTranslations(inTranslationFile: InTranslationFile, files: Seq[InTranslationFile]): Try[Unit] = {
      for {
        translationFiles    <- verifyAllFilesAreTranslated(files)
        inTranslation       <- translationWithId(inTranslationFile.inTranslationId)
        originalBook        <- originalBook(inTranslation)
        crowdinClient       <- crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage)
        metadataFile        <- findMetadataFile(inTranslation, translationFiles)
        translatedMetadata  <- crowdinClient.fetchTranslatedMetaData(metadataFile, inTranslation.crowdinToLanguage)
        chapterFiles        <- findChapterFiles(inTranslation, translationFiles)
        translatedChapters  <- Try(chapterFiles.map(chapter => crowdinClient.fetchTranslatedChapter(chapter, inTranslation.crowdinToLanguage)))
        _                   <- inTransaction { implicit session =>
          val translatedFailures = translatedChapters.filter(_.isFailure).map(_.failed.get)
          if (translatedFailures.nonEmpty) {
            Failure(new CrowdinException(translatedFailures))
          } else {
            val chaptersToAdd: Seq[NewTranslatedChapter] = mergeService.mergeChapters(originalBook, translatedChapters.map(_.get))
            writeService.newTranslationForBook(originalBook, inTranslation, translatedMetadata).map(newTranslationId => {
              val persisted: Seq[Try[InTranslationFile]] = chaptersToAdd.map(chapterToAdd => {
                for {
                  newChapterId <- writeService.newTranslatedChapter(newTranslationId.id, chapterToAdd)
                  translationFile <- findTranslationFileFor(translationFiles, chapterToAdd)
                  updatedTranslationFile <- translationDbService.updateInTranslationFile(translationFile.copy(newChapterId = Some(newChapterId.id)))
                } yield updatedTranslationFile
              })

              val persistErrors = persisted.filter(_.isFailure).map(_.failed.get)
              if (persistErrors.nonEmpty) {
                Failure(new DBException(persistErrors))
              } else {
                Success()
              }
            })
          }
        }
      } yield Success()
    }

    def updateTranslationStatus(projectIdentifier: String, language: LanguageTag, fileId: String, status: TranslationStatus.Value): Try[InTranslationFile] = {
      translationDbService.fileForCrowdinProjectWithFileIdAndLanguage(projectIdentifier, fileId, language) match {
        case None => Failure(new NotFoundException(s"No translation for project $projectIdentifier, language $language and file_id $fileId"))
        case Some(file) =>
          translationDbService.updateTranslationStatus(file, status)
      }
    }

    def addTranslation(translateRequest: api.TranslateRequest): Try[api.TranslateResponse] = {
      Try(LanguageTag(translateRequest.fromLanguage)).flatMap(fromLanguage => {
        validateToLanguage(translateRequest.toLanguage).flatMap(toLanguage => {
          readService.withIdAndLanguage(translateRequest.bookId, fromLanguage) match {
            case None => Failure(new NotFoundException())
            case Some(originalBook) => {
              writeService.addPersonFromAuthUser()

              crowdinClientBuilder.forSourceLanguage(fromLanguage).flatMap(crowdinClient => {
                val existingTranslations = translationDbService.translationsForOriginalId(translateRequest.bookId)

                val toAddUser = existingTranslations.find(tr => tr.fromLanguage == fromLanguage && tr.toLanguage == LanguageTag(toLanguage))
                val toAddLanguage = existingTranslations.find(tr => tr.fromLanguage == fromLanguage)

                val inTranslation = (toAddUser, toAddLanguage) match {
                  case (Some(addUser), _) => addUserToTranslation(addUser)
                  case (None, Some(addLanguage)) => addTargetLanguageForTranslation(addLanguage, translateRequest, crowdinClient)
                  case _ => createTranslation(translateRequest, originalBook, fromLanguage, toLanguage, crowdinClient)
                }

                inTranslation.map(x => api.TranslateResponse(x.id.get, CrowdinUtils.crowdinUrlToBook(originalBook, x.crowdinProjectId, toLanguage)))
              })
            }
          }
        })
      })
    }

    private def addUserToTranslation(inTranslation: InTranslation): Try[InTranslation] = {
      translationDbService.addUserToTranslation(inTranslation)
    }

    private def addTargetLanguageForTranslation(inTranslation: InTranslation, translateRequest: TranslateRequest, crowdinClient: CrowdinClient): Try[InTranslation] = {
      for {
        files <- Try(translationDbService.filesForTranslation(inTranslation.id.get))
        _ <- crowdinClient.addTargetLanguage(translateRequest.toLanguage)
        persistedTranslation <- translationDbService.addTranslationWithFiles(inTranslation, files, translateRequest)
      } yield persistedTranslation
    }

    private def createTranslation(translateRequest: TranslateRequest, originalBook: Book, fromLanguage: LanguageTag, toLanguage: String, crowdinClient: CrowdinClient): Try[InTranslation] = {
      val chapters: Seq[Chapter] = originalBook.chapters.flatMap(ch => readService.chapterForBookWithLanguageAndId(originalBook.id, fromLanguage, ch.id))

      val inTranslation = for {
        _ <- crowdinClient.addTargetLanguage(toLanguage)
        directory <- crowdinClient.addDirectoryFor(originalBook)
        crowdinMeta <- crowdinClient.addBookMetadata(originalBook)
        crowdinChapters <- crowdinClient.addChaptersFor(originalBook, chapters)
        persistedTranslation <- translationDbService.newTranslation(translateRequest, crowdinMeta, crowdinChapters, crowdinClient.getProjectIdentifier)
      } yield persistedTranslation

      inTranslation match {
        case Success(x) => Success(x)
        case Failure(e@(_: CrowdinException | _: DBException)) =>
          crowdinClient.deleteDirectoryFor(originalBook)
          Failure(e)

        case Failure(e) => Failure(e)
      }
    }

    private def validateToLanguage(toLanguage: String): Try[String] = {
      Try(LanguageTag(toLanguage)).flatMap(_ => {
        if (supportedLanguageService.getSupportedLanguages.exists(_.code == toLanguage))
          Success(toLanguage)
        else
          Failure(new ValidationException(errors = Seq(ValidationMessage("toLanguage", s"The language '$toLanguage' is not a supported language to translate to."))))

      })
    }

    private def verifyAllFilesAreTranslated(inTranslationFiles: Seq[InTranslationFile]): Try[Seq[InTranslationFile]] = {
      if(inTranslationFiles.forall(_.translationStatus == TranslationStatus.TRANSLATED)) {
        Success(inTranslationFiles)
      } else {
        Failure(new RuntimeException(s"Not all files for translation are finished translating. Cannot continue."))
      }
    }

    def findAllTranslationFiles(inTranslationFile: InTranslationFile): Seq[InTranslationFile] = {
      translationDbService.filesForTranslation(inTranslationFile.inTranslationId)
    }

    def inTranslationWithId(inTranslationId: Long): Option[InTranslation] = {
      translationDbService.translationWithId(inTranslationId)
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
      translationRepository.withId(translationId) match {
        case None => Failure(new RuntimeException(s"The translated book with id $translationId was not found. Cannot fetch updates."))
        case Some(existingTranslation) => Success(existingTranslation)
      }
    }

    private def findMetadataFile(inTranslation: InTranslation, translationFiles: Seq[InTranslationFile]): Try[InTranslationFile] = {
      translationFiles.find(_.fileType == FileType.METADATA) match {
        case None => Failure(new RuntimeException(s"No metadata for translation with id ${inTranslation.id} found. Cannot fetch updates"))
        case Some(metadataFile) => Success(metadataFile)
      }
    }

    private def findChapterFiles(translation: InTranslation, translationFiles: Seq[InTranslationFile]): Try[Seq[InTranslationFile]] = {
      translationFiles.filter(_.fileType == FileType.CONTENT) match {
        case Nil => Failure(new RuntimeException(s"No chapters found for translation with id ${translation.id}."))
        case list => Success(list)
      }
    }

    private def translationWithId(id: Long): Try[InTranslation] = {
      translationDbService.translationWithId(id) match {
        case None => Failure(new RuntimeException(s"InTranslation with id $id was not found. Cannot continue."))
        case Some(inTranslation) => Success(inTranslation)
      }
    }

    def findTranslationFileFor(translationFiles: Seq[InTranslationFile], chapter: NewTranslatedChapter): Try[InTranslationFile] = {
      val inTranslationOpt = translationFiles.find(_.originalChapterId.contains(chapter.originalChapterId))
      inTranslationOpt match {
        case None => Failure(new RuntimeException(s"No translationfile found for ${chapter.originalChapterId}. Cannot continue."))
        case Some(inTranslationF) => Success(inTranslationF)
      }
    }
  }

}
