/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.network.AuthUser
import io.digitallibrary.bookapi.model.api.{DBException, TranslateRequest}
import io.digitallibrary.bookapi.model.crowdin.CrowdinFile
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.repository.{InTranslationFileRepository, InTranslationRepository, TransactionHandler}
import io.digitallibrary.bookapi.service.ConverterService

import scala.util.{Failure, Success, Try}

trait TranslationDbService {
  this: ConverterService with InTranslationRepository with InTranslationFileRepository with TransactionHandler =>
  val translationDbService: TranslationDbService

  class TranslationDbService extends LazyLogging {
    def fileForCrowdinProjectWithFileIdAndLanguage(crowdinProjectId: String, fileId: String, language: LanguageTag): Option[InTranslationFile] =
      inTranslationFileRepository.forCrowdinProjectWithFileIdAndLanguage(crowdinProjectId, fileId, language)

    def fileForCrowdinProjectWithFileId(crowdinProjectId: String, fileId: String): Seq[InTranslationFile] =
      inTranslationFileRepository.forCrowdinProjectWithFileId(crowdinProjectId, fileId)

    def filesForTranslation(inTranslationId: Long): Seq[InTranslationFile] =
      inTranslationFileRepository.withTranslationId(inTranslationId)

    def translationsForOriginalId(originalId: Long): Seq[InTranslation] =
      inTranslationRepository.forOriginalId(originalId)

    def translationWithId(translationId: Long): Option[InTranslation] = inTranslationRepository.withId(translationId)

    def newTranslation(translateRequest: TranslateRequest, translation: Translation, crowdinMeta: CrowdinFile, crowdinChapters: Seq[CrowdinFile], crowdinProject: String): Try[InTranslation] = {
      inTransaction { implicit session =>
        val persisted = for {
          newInTranslation <- Try(inTranslationRepository.add(converterService.asDomainInTranslation(translateRequest, translation, crowdinProject)))
          _ <- Try(inTranslationFileRepository.add(converterService.asDomainInTranslationFile(crowdinMeta, newInTranslation)))
          _ <- Try(crowdinChapters.map(ch => inTranslationFileRepository.add(converterService.asDomainInTranslationFile(ch, newInTranslation))))
        } yield newInTranslation

        persisted match {
          case Success(x) => Success(x)
          case Failure(e) => Failure(new DBException(e))
        }
      }
    }

    def addTranslationWithFiles(inTranslation: InTranslation, files: Seq[InTranslationFile], newTranslation: Translation, translateRequest: TranslateRequest): Try[InTranslation] = {
      inTransaction { implicit session =>
        val persisted = for {
          persistedTranslation <- Try(inTranslationRepository.add(converterService.asDomainInTranslation(translateRequest, newTranslation, inTranslation.crowdinProjectId)))

          persistedMetadata <- Try(files.find(_.fileType == FileType.METADATA)
            .map(_.copy(id = None, revision = None, inTranslationId = persistedTranslation.id.get, translationStatus = TranslationStatus.IN_PROGRESS, etag = None)).map(inTranslationFileRepository.add))

          persistedFiles <- Try(files.flatMap(file => {
            newTranslation.chapters.find(_.seqNo == file.seqNo).map(chapter => {
              file.copy(id = None, revision = None, inTranslationId = persistedTranslation.id.get, translationStatus = TranslationStatus.IN_PROGRESS, etag = None, newChapterId = chapter.id)
            })
          }).map(inTranslationFileRepository.add))

        } yield persistedTranslation

        persisted match {
          case Success(x) => Success(x)
          case Failure(e) => Failure(new DBException(e))
        }
      }
    }

    def addUserToTranslation(inTranslation: InTranslation, person: Person): Try[InTranslation] = {
      val newListOfUsers = inTranslation.userIds :+ person.gdlId.get
      val toUpdate = inTranslation.copy(userIds = newListOfUsers)

      Try(inTranslationRepository.updateTranslation(toUpdate))
    }

    def updateInTranslationFile(file: InTranslationFile): Try[InTranslationFile] = Try {
      inTranslationFileRepository.updateInTranslationFile(file)
    }

    def updateTranslationStatus(file: InTranslationFile, newStatus: TranslationStatus.Value): Try[InTranslationFile] = {
      Try(
        inTranslationFileRepository.updateInTranslationFile(
          file.copy(translationStatus = newStatus)
        )
      )
    }
  }

}
