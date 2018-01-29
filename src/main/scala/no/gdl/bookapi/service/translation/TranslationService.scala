/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.translation

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.crowdin.{CrowdinClient, CrowdinClientBuilder, CrowdinUtils, TranslatedChapter}
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api._
import no.gdl.bookapi.model.api.internal.{NewChapter, NewTranslatedChapter}
import no.gdl.bookapi.model.domain.{FileType, InTranslation, InTranslationFile, TranslationStatus}
import no.gdl.bookapi.repository.TranslationRepository
import no.gdl.bookapi.service.{ReadService, WriteService}

import scala.util.{Failure, Success, Try}

trait TranslationService {
  this: CrowdinClientBuilder with ReadService with WriteService with SupportedLanguageService with TranslationDbService with MergeService with TranslationRepository =>
  val translationService: TranslationService


  class TranslationService extends LazyLogging {
    def inTranslationWithId(inTranslationId: Long): Option[InTranslation] = {
      translationDbService.translationWithId(inTranslationId)
    }

    def fetchUpdatesFor(inTranslation: InTranslation): Try[SynchronizeResponse] = {
      inTranslation.newTranslationId match {
        case None => Failure(new RuntimeException(s"The book for ${inTranslation.originalTranslationId} has not yet been translated. Cannot fetch updates."))
        case Some(newTranslationId) => {
          val inTranslationFilesToUpdate = translationDbService.filesForTranslation(inTranslation.id.get)
          crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage).flatMap(crowdinClient => {
            readService.withIdAndLanguage(inTranslation.originalTranslationId, inTranslation.fromLanguage) match {
              case None => Failure(new RuntimeException(s"The original book with id ${inTranslation.originalTranslationId} was not found. Cannot fetch updates."))
              case Some(originalBook) => {

                translationRepository.withId(newTranslationId) match {
                  case None => Failure(new RuntimeException(s"The translated book with id $newTranslationId was not found. Cannot fetch updates."))
                  case Some(existingTranslation) => {
                    val metaDataOpt = inTranslationFilesToUpdate.find(_.fileType == FileType.METADATA).map(metadata => crowdinClient.fetchTranslatedMetaData(metadata, inTranslation.crowdinToLanguage))
                    metaDataOpt match {
                      case None => Failure(new RuntimeException(s"No metadata for translation with id ${inTranslation.id} found. Cannot fetch updates"))
                      case Some(metadataTry) => {
                        val chapters: Seq[Try[TranslatedChapter]] = inTranslationFilesToUpdate.filter(_.fileType == FileType.CONTENT).map(content => crowdinClient.fetchTranslatedChapter(content, inTranslation.crowdinToLanguage))

                        val persisted_all = for {
                          metadata <- metadataTry
                          persisted <- Try(writeService.updateTranslation(existingTranslation.copy(title = metadata.title, about = metadata.description)))
                          mergedChapters <- Try(mergeService.mergeChapters(persisted, chapters.filter(_.isSuccess).map(_.get)))
                          _ <- Try(mergedChapters.map(ch => writeService.updateChapter(ch)))
                        } yield persisted

                        persisted_all match {
                          case Success(translation) => Success(SynchronizeResponse(translation.bookId, CrowdinUtils.crowdinUrlToBook(originalBook, crowdinClient.getProjectIdentifier, inTranslation.crowdinToLanguage)))
                          case Failure(err) => Failure(err)
                        }
                      }
                    }
                  }
                }
              }
            }
          })
        }
      }
    }

    def fetchTranslationsIfAllTranslated(inTranslationFile: InTranslationFile): Try[Unit] = {
      val translationFiles = translationDbService.filesForTranslation(inTranslationFile.inTranslationId)
      if (translationFiles.forall(_.translationStatus == TranslationStatus.TRANSLATED)) {
        translationDbService.translationWithId(inTranslationFile.inTranslationId) match {
          case None => Failure(new RuntimeException(s"InTranslation with id ${inTranslationFile.inTranslationId} was not found. Cannot continue."))
          case Some(inTranslation) => {
            readService.withIdAndLanguage(inTranslation.originalTranslationId, inTranslation.fromLanguage) match {
              case None => Failure(new RuntimeException(s"Original translation with id ${inTranslation.originalTranslationId} and language ${inTranslation.fromLanguage} was not found. Cannot continue."))
              case Some(originalBook) => {
                crowdinClientBuilder.forSourceLanguage(inTranslation.fromLanguage).flatMap(crowdinClient => {
                  val metadataOpt = translationFiles.find(_.fileType == FileType.METADATA).map(metadata => crowdinClient.fetchTranslatedMetaData(metadata, inTranslation.crowdinToLanguage))
                  metadataOpt match {
                    case None => Failure(new RuntimeException(s"No metadata for translation with id ${inTranslationFile.inTranslationId} found. Cannot continue."))
                    case Some(metadataTry) => {
                        val newTranslationIdTry = metadataTry.flatMap(metadata => writeService.newTranslationForBook(originalBook, inTranslation, metadata))

                        newTranslationIdTry.map(newTranslationId => {
                          val translatedChapters = translationFiles.filter(_.fileType == FileType.CONTENT).map(content => crowdinClient.fetchTranslatedChapter(content, inTranslation.crowdinToLanguage))
                          val translatedFailures = translatedChapters.filter(_.isFailure).map(_.failed.get)
                          if(translatedFailures.nonEmpty) {
                            Failure(new CrowdinException(translatedFailures))
                          } else {
                            val chaptersToAdd: Seq[NewTranslatedChapter] = mergeService.mergeChapters(originalBook, translatedChapters.map(_.get))
                            val persisted: Seq[Try[InTranslationFile]] = chaptersToAdd.map(chapterToAdd => {
                              writeService.newTranslatedChapter(newTranslationId.id, chapterToAdd).flatMap(newChapterId => {
                                val inTranslationOpt = translationFiles.find(_.originalChapterId.contains(chapterToAdd.originalChapterId))
                                inTranslationOpt match {
                                  case None => Failure(new RuntimeException(s"No translationfile found for ${chapterToAdd.originalChapterId}. Cannot continue."))
                                  case Some(inTranslationF) => translationDbService.updateInTranslationFile(inTranslationF.copy(newChapterId = Some(newChapterId.id)))
                                }
                              })
                            })

                            val persistErrors = persisted.filter(_.isFailure).map(_.failed.get)
                            if(persistErrors.nonEmpty) {
                              Failure(new DBException(persistErrors))
                            } else {
                              Success()
                            }
                          }
                        })
                      }
                    }
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
