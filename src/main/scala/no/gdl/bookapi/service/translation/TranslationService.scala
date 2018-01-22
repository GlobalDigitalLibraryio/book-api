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
import no.gdl.bookapi.model.api.internal.NewChapter
import no.gdl.bookapi.model.domain.{InTranslation, InTranslationFile, TranslationStatus}
import no.gdl.bookapi.service.{ReadService, WriteService}

import scala.util.{Failure, Success, Try}

trait TranslationService {
  this: CrowdinClientBuilder with ReadService with WriteService with SupportedLanguageService with TranslationDbService with MergeService =>
  val translationService: TranslationService


  class TranslationService extends LazyLogging {
    def fetchTranslationsIfAllTranslated(inTranslationFile: InTranslationFile): Try[Unit] = {
      val translationFiles = translationDbService.filesForTranslation(inTranslationFile.inTranslationId)
      if (translationFiles.forall(_.translationStatus == TranslationStatus.TRANSLATED)) {
        translationDbService.translationWithId(inTranslationFile.inTranslationId) match {
          case None => Failure(new NotFoundException("TODO"))
          case Some(inTranslation) => {
            readService.withIdAndLanguage(inTranslation.originalId, inTranslation.fromLanguage) match {
              case None => Failure(new NotFoundException("TODO"))
              case Some(originalBook) => {
                crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage).flatMap(crowdinClient => {
                  crowdinClient.fetchTranslatedMetaData(originalBook, inTranslation).flatMap(metadata => {
                    val translatedChapters = originalBook.chapters.map(chapter => crowdinClient.fetchTranslatedChapter(originalBook, chapter.id, inTranslation))
                    if (!translatedChapters.forall(_.isSuccess)) {
                      Failure(new RuntimeException("Could not fetch all translations....TODO"))
                    } else {
                      writeService.newTranslationForBook(originalBook, inTranslation, metadata).flatMap(translationId => {
                        val chaptersToAdd: Seq[NewChapter] = mergeService.mergeChapters(originalBook, translatedChapters.map(_.get))

                        val persistedChapters = chaptersToAdd.map(newChapter => writeService.newChapter(translationId.id, newChapter))
                        val persistErrors = persistedChapters.filter(_.isFailure).map(_.failed.get)
                        if(persistErrors.nonEmpty) {
                          Failure(new RuntimeException(persistErrors.head))
                        } else {
                          Success()
                        }
                      })
                    }

                  })
                })
              }
            }
          }
        }
      } else {
        Success()
      }
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
  }

}
