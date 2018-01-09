/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.network.AuthUser
import no.gdl.bookapi.model.api.{DBException, TranslateRequest}
import no.gdl.bookapi.model.crowdin.CrowdinFile
import no.gdl.bookapi.model.domain.{InTranslation, InTranslationFile, TranslationStatus}
import no.gdl.bookapi.repository.{InTranslationFileRepository, InTranslationRepository, TransactionHandler}
import no.gdl.bookapi.service.ConverterService

import scala.util.{Failure, Success, Try}

trait TranslationDbService {
  this: ConverterService with InTranslationRepository with InTranslationFileRepository with TransactionHandler =>
  val translationDbService: TranslationDbService

  class TranslationDbService extends LazyLogging {
    def fileForInTranslationWithFileId(inTranslation: InTranslation, fileId: String): Option[InTranslationFile] =
      inTranslationFileRepository.forTranslationWithCrowdinFileId(inTranslation.id, fileId)

    def filesForTranslation(inTranslation: InTranslation): Seq[InTranslationFile] =
      inTranslationFileRepository.withTranslationId(inTranslation.id)

    def translationsForOriginalId(originalId: Long): Seq[InTranslation] =
      inTranslationRepository.forOriginalId(originalId)

    def inTranslationForProjectIdentifierAndLanguage(projectIdentifier: String, language: LanguageTag): Option[InTranslation] =
      inTranslationRepository.withProjectIdentifierAndToLanguage(projectIdentifier, language)

    def newTranslation(translateRequest: TranslateRequest, crowdinMeta: CrowdinFile, crowdinChapters: Seq[CrowdinFile], crowdinProject: String): Try[InTranslation] = {
      val persisted = for {
        newInTranslation <- Try(inTranslationRepository.add(converterService.asDomainInTranslation(translateRequest, crowdinProject)))
        _ <- Try(inTranslationFileRepository.add(converterService.asDomainInTranslationFile(crowdinMeta, newInTranslation)))
        _ <- Try(crowdinChapters.map(ch => inTranslationFileRepository.add(converterService.asDomainInTranslationFile(ch, newInTranslation))))
      } yield newInTranslation

      persisted match {
        case Success(x) => Success(x)
        case Failure(e) => Failure(new DBException(e))
      }
    }

    def addTranslationWithFiles(inTranslation: InTranslation, files: Seq[InTranslationFile], translateRequest: TranslateRequest): Try[InTranslation] = {
      inTransaction { implicit session =>
        val persisted = for {
          persistedTranslation <- Try(inTranslationRepository.add(converterService.asDomainInTranslation(translateRequest, inTranslation.crowdinProjectId)))
          _ <- Try(files.map(f => f.copy(id = None, revision = None, inTranslationId = persistedTranslation.id.get, translationStatus = TranslationStatus.IN_PROGRESS, etag = None)).map(fh => inTranslationFileRepository.add(fh)))
        } yield persistedTranslation

        persisted match {
          case Success(x) => Success(x)
          case Failure(e) => Failure(new DBException(e))
        }
      }
    }

    def addUserToTranslation(inTranslation: InTranslation): Try[InTranslation] = {
      if (inTranslation.userIds.contains(AuthUser.get.get)) {
        Success(inTranslation)
      } else {
        val newListOfUsers = inTranslation.userIds :+ AuthUser.get.get
        val toUpdate = inTranslation.copy(userIds = newListOfUsers)

        Try(inTranslationRepository.updateTranslation(toUpdate))
      }
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
