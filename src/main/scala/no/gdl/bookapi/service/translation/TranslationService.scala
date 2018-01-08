/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.crowdin.{CrowdinClient, CrowdinClientBuilder, CrowdinUtils}
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api._
import no.gdl.bookapi.model.domain.InTranslation
import no.gdl.bookapi.service.ReadService

import scala.util.{Failure, Success, Try}

trait TranslationService {
  this: CrowdinClientBuilder with ReadService with SupportedLanguageService with WriteTranslationService =>
  val translationService: TranslationService


  class TranslationService extends LazyLogging {
    def addTranslation(translateRequest: api.TranslateRequest): Try[api.TranslateResponse] = {
      val fromLanguage = LanguageTag(translateRequest.fromLanguage)
      val toLanguage = validateToLanguage(translateRequest.toLanguage)

      readService.withIdAndLanguage(translateRequest.bookId, fromLanguage) match {
        case None => Failure(new NotFoundException())
        case Some(originalBook) => {
          crowdinClientBuilder.forSourceLanguage(fromLanguage).flatMap(crowdinClient => {
            val existingTranslations = writeTranslationService.translationsForOriginalId(translateRequest.bookId)
            val toAddUser = existingTranslations.find(tr => tr.fromLanguage == fromLanguage && tr.toLanguage == LanguageTag(toLanguage))
            val toAddLanguage = existingTranslations.find(tr => tr.fromLanguage == fromLanguage)

            val inTranslation = if (toAddUser.isDefined) {
              addUserToTranslation(toAddUser.get)
            } else if (toAddLanguage.isDefined) {
              addTargetLanguageForTranslation(toAddLanguage.get, translateRequest, crowdinClient)
            } else {
              createTranslation(translateRequest, originalBook, fromLanguage, toLanguage, crowdinClient)
            }

            inTranslation match {
              case Success(x) => Success(api.TranslateResponse(x.id.get, CrowdinUtils.crowdinUrlToBook(originalBook, x.crowdinProjectId, toLanguage)))
              case Failure(err) =>
                err.printStackTrace()
                Failure(err)
            }
          })
        }
      }
    }

    def addUserToTranslation(inTranslation: InTranslation): Try[InTranslation] = {
      writeTranslationService.addUserToTranslation(inTranslation)
    }

    def addTargetLanguageForTranslation(inTranslation: InTranslation, translateRequest: TranslateRequest, crowdinClient: CrowdinClient): Try[InTranslation] = {
      for {
        files <- Try(writeTranslationService.filesForTranslation(inTranslation))
        _ <- crowdinClient.addTargetLanguage(translateRequest.toLanguage)
        persistedTranslation <- writeTranslationService.addTranslationWithFiles(inTranslation, files, translateRequest)
      } yield persistedTranslation
    }

    def createTranslation(translateRequest: TranslateRequest, originalBook: Book, fromLanguage: LanguageTag, toLanguage: String, crowdinClient: CrowdinClient): Try[InTranslation] = {
      val chapters: Seq[Chapter] = originalBook.chapters.flatMap(ch => readService.chapterForBookWithLanguageAndId(originalBook.id, fromLanguage, ch.id))

      val inTranslation = for {
        _ <- crowdinClient.addTargetLanguage(toLanguage)
        directory <- crowdinClient.addDirectoryFor(originalBook)
        crowdinMeta <- crowdinClient.addBookMetadata(originalBook)
        crowdinChapters <- crowdinClient.addChaptersFor(originalBook, chapters)
        persistedTranslation <- writeTranslationService.newInTranslation(translateRequest, crowdinMeta, crowdinChapters, crowdinClient.getProjectIdentifier)
      } yield persistedTranslation

      inTranslation match {
        case Success(x) => Success(x)
        case Failure(e@(_: CrowdinException | _: DBException)) =>
          crowdinClient.deleteDirectoryFor(originalBook)
          Failure(e)

        case Failure(e) => Failure(e)
      }
    }

    def validateToLanguage(toLanguage: String): String = {
      LanguageTag(toLanguage)
      val isSupported = supportedLanguageService.getSupportedLanguages.exists(_.code == toLanguage)

      if (isSupported)
        toLanguage
      else
        throw new ValidationException(errors = Seq(ValidationMessage("toLanguage", s"The language '$toLanguage' is not a supported language to translate to.")))

    }
  }

}
