/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.crowdin._
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.domain.{Book => _, Chapter => _, _}
import io.digitallibrary.bookapi.repository.{ChapterRepository, TransactionHandler, TranslationRepository}
import io.digitallibrary.bookapi.service.{ConverterService, ReadService, WriteService}
import io.digitallibrary.language.model.LanguageTag

import scala.util.{Failure, Success, Try}

trait TranslationService {
  this: CrowdinClientBuilder with ReadService with WriteService with SupportedLanguageService with TranslationDbService with MergeService with TranslationRepository with TransactionHandler with ChapterRepository with ConverterService with SynchronizeService =>
  val translationService: TranslationService

  class TranslationService extends LazyLogging {

    def addTranslation(translateRequest: domain.TranslateRequest): Try[api.TranslateResponse] = {
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
                  case (Some(addUser), _) => addUserToTranslation(addUser, translateRequest.userId.get)
                  case (None, Some(addLanguage)) => addTargetLanguageForTranslation(addLanguage, translateRequest, originalBook.id, LanguageTag(originalBook.language.code), crowdinClient)
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

    private def createTranslation(translateRequest: domain.TranslateRequest, originalBook: Book, fromLanguage: LanguageTag, toLanguage: String, crowdinClient: CrowdinClient): Try[InTranslation] = {
      writeService.newTranslationForBook(originalBook.id, LanguageTag(originalBook.language.code), translateRequest).flatMap(newTranslation => {
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
          pseudoFiles <- synchronizeService.fetchPseudoFiles(persistedTranslation)
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

    private def addUserToTranslation(inTranslation: InTranslation, userId: String): Try[InTranslation] = {
      if(inTranslation.userIds.contains(userId)) {
        Success(inTranslation)
      } else {
        translationDbService.addUserToTranslation(inTranslation, userId)
      }
    }

    def addTargetLanguageForTranslation(inTranslation: InTranslation, translateRequest: domain.TranslateRequest, originalBookId: Long, originalBookLanguage: LanguageTag, crowdinClient: CrowdinClient, translationStatus: TranslationStatus.Value = TranslationStatus.IN_PROGRESS): Try[InTranslation] = {
      for {
        files <- Try(translationDbService.filesForTranslation(inTranslation.id.get))
        _ <- crowdinClient.addTargetLanguage(translateRequest.toLanguage)
        newTranslation <- writeService.newTranslationForBook(originalBookId, originalBookLanguage, translateRequest, translationStatus)
        persistedTranslation <- translationDbService.addTranslationWithFiles(inTranslation, files, newTranslation, translateRequest, translationStatus)
      } yield persistedTranslation
    }

    private def validateToLanguage(fromLanguage: LanguageTag, toLanguage: String): Try[String] = {
      Try(LanguageTag(toLanguage)).flatMap(_ => {
        if (supportedLanguageService.getSupportedLanguages(Some(fromLanguage)).exists(_.code == toLanguage))
          Success(toLanguage)
        else
          Failure(new ValidationException(errors = Seq(ValidationMessage("toLanguage", s"The language '$toLanguage' is not a supported language to translate to."))))

      })
    }

    def inTranslationWithId(inTranslationId: Long): Option[InTranslation] = {
      translationDbService.translationWithId(inTranslationId)
    }

    def forTranslation(translatedFrom: LanguageTag, bookId: Long): Option[api.BookForTranslation] = {
      readService.withIdLanguageAndFromLanguage(bookId, BookApiProperties.CrowdinPseudoLanguage, translatedFrom).map(converterService.toBookForTranslation)
    }

    def forTranslationAndChapter(bookId: Long, chapterId: Long): Option[api.Chapter] = {
      readService.chapterWithId(chapterId)
    }
  }
}
