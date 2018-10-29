/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.CrowdinTranslatorPlaceHolder
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.network.AuthUser
import io.digitallibrary.bookapi.integration.crowdin._
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.api.internal.NewTranslatedChapter
import io.digitallibrary.bookapi.model.domain.{Book => _, Chapter => _, _}
import io.digitallibrary.bookapi.repository.{ChapterRepository, TransactionHandler, TranslationRepository}
import io.digitallibrary.bookapi.service.{ReadService, WriteService}

import scala.util.{Failure, Success, Try}

trait TranslationService {
  this: CrowdinClientBuilder with ReadService with WriteService with SupportedLanguageService with TranslationDbService with MergeService with TranslationRepository with TransactionHandler with ChapterRepository =>
  val translationService: TranslationService


  class TranslationService extends LazyLogging {
    def allFilesTranslated(inTranslationFile: InTranslationFile): Boolean = {
      val translationFiles = translationDbService.filesForTranslation(inTranslationFile.inTranslationId)
      translationFiles.forall(_.translationStatus == TranslationStatus.TRANSLATED)
    }

    def fetchUpdatesFor(inTranslation: InTranslation): Try[SynchronizeResponse] = {
      for {
        newTranslationId    <- newTranslationId(inTranslation)
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
            existingTranslationWithContrib <- Try(extractContributors(translatedMetadata, existingTranslation))
            persisted                      <- writeService.updateTranslation(existingTranslationWithContrib.copy(title = translatedMetadata.title, about = translatedMetadata.description))
            mergedChapters                 <- Try(mergeService.mergeChapters(persisted, translatedChapters.flatMap(_.toOption)))
            _                              <- Try(mergedChapters.map(ch => writeService.updateChapter(ch)))
          } yield persisted
        }
      } yield SynchronizeResponse(persisted.bookId, CrowdinUtils.crowdinUrlToBook(originalBook, crowdinClient.getProjectIdentifier, inTranslation.crowdinToLanguage))
    }


    def fetchTranslatedFile(projectIdentifier: String, crowdinToLanguage: String, fileId: String, status: TranslationStatus.Value): Try[InTranslationFile] = {
      val toLanguage = LanguageTag(crowdinToLanguage)

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
                addedTranslation <- addTargetLanguageForTranslation(inTranslation, TranslateRequest(inTranslation.originalTranslationId, inTranslation.fromLanguage.toString, crowdinToLanguage), original, crowdinClient)
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

    def allFilesHaveTranslationStatusGreatherOrEqualTo(inTranslationFile: InTranslationFile, status: TranslationStatus.Value): Boolean = {
      translationDbService.filesForTranslation(inTranslationFile.inTranslationId).forall(_.translationStatus.id >= status.id)
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

      
    def addTranslation(translateRequest: api.TranslateRequest, userId: String): Try[api.TranslateResponse] = {
      Try(LanguageTag(translateRequest.fromLanguage)).flatMap(fromLanguage => {
        validateToLanguage(fromLanguage, translateRequest.toLanguage).flatMap(toLanguage => {
          readService.withIdAndLanguage(translateRequest.bookId, fromLanguage) match {
            case None => Failure(new NotFoundException())
            case Some(originalBook) => {
              crowdinClientBuilder.forSourceLanguage(fromLanguage).flatMap(crowdinClient => {
                val existingTranslations = translationDbService.translationsForOriginalId(translateRequest.bookId)

                val toAddUser = existingTranslations.find(tr => tr.fromLanguage == fromLanguage && tr.toLanguage == LanguageTag(toLanguage))
                val toAddLanguage = existingTranslations.find(tr => tr.fromLanguage == fromLanguage)

                val inTranslationTry = (toAddUser, toAddLanguage) match {
                  case (Some(addUser), _) => addUserToTranslation(addUser, userId)
                  case (None, Some(addLanguage)) => addTargetLanguageForTranslation(addLanguage, translateRequest, originalBook, crowdinClient)
                  case _ => createTranslation(translateRequest, originalBook, fromLanguage, toLanguage, crowdinClient)
                }

                inTranslationTry.map(inTranslation =>
                  api.TranslateResponse(inTranslation.id.get, CrowdinUtils.crowdinUrlToBook(originalBook, inTranslation.crowdinProjectId, toLanguage)))
              })
            }
          }
        })
      })
    }

    private def addUserToTranslation(inTranslation: InTranslation, userId: String): Try[InTranslation] = {
      if(inTranslation.userIds.contains(userId)) {
        Success(inTranslation)
      } else {
        translationDbService.addUserToTranslation(inTranslation, userId)
      }
    }

    private def addTargetLanguageForTranslation(inTranslation: InTranslation, translateRequest: TranslateRequest, originalBook: Book, crowdinClient: CrowdinClient): Try[InTranslation] = {
      for {
        files <- Try(translationDbService.filesForTranslation(inTranslation.id.get))
        _ <- crowdinClient.addTargetLanguage(translateRequest.toLanguage)
        newTranslation <- writeService.newTranslationForBook(originalBook, translateRequest)
        persistedTranslation <- translationDbService.addTranslationWithFiles(inTranslation, files, newTranslation, translateRequest)
      } yield persistedTranslation
    }

    private def createTranslation(translateRequest: TranslateRequest, originalBook: Book, fromLanguage: LanguageTag, toLanguage: String, crowdinClient: CrowdinClient): Try[InTranslation] = {
      writeService.newTranslationForBook(originalBook, translateRequest).flatMap(newTranslation => {
        val chaptersToTranslate: Seq[Chapter] = newTranslation.chapters
          .filter(_.chapterType != ChapterType.License)
          .filter(_.containsText())
          .flatMap(ch => readService.chapterWithId(ch.id.get))


        val inTranslation = for {
          _ <- writeService.addInTransportMark(originalBook)
          _ <- crowdinClient.addTargetLanguage(toLanguage)
          directory <- crowdinClient.addDirectoryFor(newTranslation)
          crowdinMeta <- crowdinClient.addBookMetadata(newTranslation)
          crowdinChapters <- crowdinClient.addChaptersFor(newTranslation, chaptersToTranslate)
          persistedTranslation <- translationDbService.newTranslation(translateRequest, newTranslation, crowdinMeta, crowdinChapters, crowdinClient.getProjectIdentifier)
          _ <- writeService.removeInTransportMark(originalBook)
        } yield persistedTranslation

        inTranslation match {
          case Success(x) => Success(x)
          case Failure(e@(_: CrowdinException | _: DBException)) =>
            writeService.removeInTransportMark(originalBook)
            crowdinClient.deleteDirectoryFor(newTranslation)
            writeService.deleteTranslation(newTranslation)
            Failure(e)

          case Failure(e) => Failure(e)
        }
      })
    }

    private def validateToLanguage(fromLanguage: LanguageTag, toLanguage: String): Try[String] = {
      Try(LanguageTag(toLanguage)).flatMap(_ => {
        if (supportedLanguageService.getSupportedLanguages(Some(fromLanguage)).exists(_.code == toLanguage))
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
      unFlaggedTranslationsRepository.withId(translationId) match {
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
  }

}
