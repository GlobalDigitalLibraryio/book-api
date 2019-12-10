package io.digitallibrary.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.crowdin.{CrowdinClient, CrowdinClientBuilder, CrowdinUtils}
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.{api, domain}
import io.digitallibrary.bookapi.model.domain.{ChapterType, InTranslation, TranslationStatus}
import io.digitallibrary.bookapi.repository.{ChapterRepository, TransactionHandler, TranslationRepository}
import io.digitallibrary.bookapi.service._
import io.digitallibrary.language.model.LanguageTag

import scala.util.{Failure, Success, Try}

trait TranslationServiceV2 {
  this: CrowdinClientBuilder with ReadServiceV2 with WriteServiceV2 with SupportedLanguageService with TranslationDbService with MergeService with TranslationRepository with TransactionHandler with ChapterRepository with ConverterService with SynchronizeService =>
  val translationServiceV2: TranslationServiceV2

  class TranslationServiceV2 extends LazyLogging {

    def addTranslation(translateRequest: domain.TranslateRequest): Try[api.TranslateResponse] = {
      Try(LanguageTag(translateRequest.fromLanguage)).flatMap(fromLanguage => {
        validateToLanguage(fromLanguage, translateRequest.toLanguage).flatMap(toLanguage => {
          readServiceV2.withIdAndLanguage(translateRequest.bookId, fromLanguage) match {
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
                  api.TranslateResponse(inTranslation.id.get, CrowdinUtils.crowdinUrlToBookV2(originalBook, inTranslation.crowdinProjectId, toLanguage)))
              })
            }
          }
        })
      })
    }

    private def createTranslation(translateRequest: domain.TranslateRequest, originalBook: BookV2, fromLanguage: LanguageTag, toLanguage: String, crowdinClient: CrowdinClient): Try[InTranslation] = {
      writeServiceV2.newTranslationForBook(originalBook.id, LanguageTag(originalBook.language.code), translateRequest).flatMap(newTranslation => {
        val chaptersToTranslate: Seq[ChapterV2] = newTranslation.chapters
          .filter(_.chapterType != ChapterType.License)
          .filter(_.containsText())
          .flatMap(ch => readServiceV2.chapterWithId(ch.id.get, LanguageTag(toLanguage)))

        val inTranslation = for {
          _ <- writeServiceV2.addInTransportMark(originalBook)
          _ <- crowdinClient.addTargetLanguage(toLanguage)
          directory <- crowdinClient.addDirectoryFor(newTranslation)
          crowdinMeta <- crowdinClient.addBookMetadata(newTranslation)
          crowdinChapters <- crowdinClient.addChaptersForV2(newTranslation, chaptersToTranslate)
          persistedTranslation <- translationDbService.newTranslation(translateRequest, newTranslation, crowdinMeta, crowdinChapters, crowdinClient.getProjectIdentifier)
          _ <- writeServiceV2.removeInTransportMark(originalBook)
          pseudoFiles <- synchronizeService.fetchPseudoFiles(persistedTranslation)
        } yield persistedTranslation

        inTranslation match {
          case Success(x) => Success(x)
          case Failure(e@(_: CrowdinException | _: DBException)) =>
            writeServiceV2.removeInTransportMark(originalBook)
            crowdinClient.deleteDirectoryFor(newTranslation)
            writeServiceV2.deleteTranslation(newTranslation)
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
        newTranslation <- writeServiceV2.newTranslationForBook(originalBookId, originalBookLanguage, translateRequest, translationStatus)
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

    def forTranslation(translatedFrom: LanguageTag, bookId: Long): Option[api.BookForTranslationV2] = {
      readServiceV2.withIdLanguageAndFromLanguage(bookId, BookApiProperties.CrowdinPseudoLanguage, translatedFrom).flatMap(converterService.toBookV2ForTranslation)
    }

    def forTranslationAndChapter(bookId: Long, chapterId: Long, language: LanguageTag): Option[api.ChapterV2] = {
      readServiceV2.chapterWithId(chapterId, language)
    }
  }
}
