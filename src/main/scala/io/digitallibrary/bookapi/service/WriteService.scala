/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service


import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.controller.NewFeaturedContent
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.internal.{ChapterId, NewChapter, NewTranslation, TranslationId}
import io.digitallibrary.bookapi.model.api.{CrowdinException, FeaturedContentId, NotFoundException, TranslateRequest, ValidationMessage}
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.bookapi.service.search.IndexService
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.license.model.License
import io.digitallibrary.network.AuthUser

import scala.util.{Failure, Success, Try}


trait WriteService {
  this: TransactionHandler
    with ConverterService
    with ValidationService
    with ReadService
    with IndexService
    with BookRepository
    with CategoryRepository
    with ChapterRepository
    with ContributorRepository
    with TranslationRepository
    with EducationalAlignmentRepository
    with PersonRepository
    with PublisherRepository
    with FeaturedContentRepository
    with InTranslationRepository
  =>

  val writeService: WriteService

  class WriteService extends LazyLogging {

    def updateTranslation(translationToUpdate: Translation): Try[Translation] = {
      for {
        updatedTranslation <- Try(getTranslationRepository.updateTranslation(translationToUpdate))
        indexedTranslation <- indexService.updateOrRemoveDocument(updatedTranslation)
      } yield indexedTranslation
    }

    def addPerson(personName: String): Person = {
      personRepository.withName(personName) match {
        case Some(person) => person
        case None => personRepository.add(Person(id = None, revision = None, name = personName, gdlId = None))
      }
    }

    def addTranslatorToTranslation(translationId: Long, person: Person): Contributor = {
      contributorRepository.add(Contributor(None, None, person.id.get, translationId, ContributorType.Translator, person))
    }

    def removeContributor(contributor: Contributor): Unit = {
      contributorRepository.remove(contributor)
    }


    def newFeaturedContent(newFeaturedContent: NewFeaturedContent): Try[FeaturedContentId] = {
      newFeaturedContent.category match {
        case Some(categoryName) =>
          categoryRepository.withName(categoryName) match {
            case Some(category) => newFeaturedContentWithCategory(newFeaturedContent, Some(category))
            case None => Failure(new api.ValidationException(errors = Seq(ValidationMessage("category", s"No category with name '$categoryName' found"))))
          }
        case None => newFeaturedContentWithCategory(newFeaturedContent, None)
      }
    }

    def newFeaturedContentWithCategory(newFeaturedContent: NewFeaturedContent, existingCategory: Option[Category]): Try[FeaturedContentId] = {
      for {
        valid <- validationService.validateFeaturedContent(converterService.toFeaturedContent(newFeaturedContent, existingCategory))
        persisted <- Try(featuredContentRepository.addContent(valid))
      } yield FeaturedContentId(persisted.id.get)
    }

    def updateFeaturedContent(content: api.FeaturedContent): Try[FeaturedContentId] = {
      content.category match {
        case Some(category) if categoryRepository.withId(category.id).isEmpty =>
          Failure(new api.ValidationException(errors = Seq(ValidationMessage("category", s"No category with id=${category.id} found"))))
        case _ =>
          for {
            valid <- validationService.validateUpdatedFeaturedContent(content)
            persistedId <- featuredContentRepository.updateContent(valid)
          } yield persistedId
      }
    }

    def deleteFeaturedContent(id: Long): Try[Unit] = {
      featuredContentRepository.deleteContent(id)
    }

    def updateChapter(chapter: domain.Chapter) = chapterRepository.updateChapter(chapter)

    def updateChapter(chapterid: Long, replacementChapter: NewChapter): Option[api.internal.ChapterId] = {
      chapterRepository.withId(chapterid).map(existing => {
        val updated = chapterRepository.updateChapter(existing.copy(
          title = replacementChapter.title,
          content = replacementChapter.content,
          chapterType = ChapterType.valueOfOrDefault(replacementChapter.chapterType)))

        api.internal.ChapterId(updated.id.get)
      })
    }

    def newChapter(translationId: Long, newChapter: api.internal.NewChapter): Try[api.internal.ChapterId] = {
      for {
        valid <- validationService.validateChapter(converterService.toDomainChapter(newChapter, translationId))
        persisted <- Try(chapterRepository.add(valid))
      } yield api.internal.ChapterId(persisted.id.get)
    }

    def updateBook(bookId: Long, bookReplacement: api.internal.NewBook): Try[api.internal.BookId] = {
      bookRepository.withId(bookId) match {
        case None => Failure(new NotFoundException(s"Book with id $bookId was not found"))
        case Some(existingBook) => {
          val optPublisher = publisherRepository.withName(bookReplacement.publisher) match {
            case Some(x) => Some(x)
            case None => Some(Publisher(None, None, bookReplacement.publisher))
          }

          for {
            validLicense <- Try(License(bookReplacement.license))
            validPublisher <- validationService.validatePublisher(optPublisher)
            persistedBook <- inTransaction { implicit session =>
              val persistedPublisher = validPublisher.id match {
                case None => Try(publisherRepository.add(validPublisher))
                case Some(_) => Success(validPublisher)
              }

              persistedPublisher.flatMap(p => {
                bookRepository.updateBook(existingBook.copy(
                  publisherId = p.id.get,
                  license = validLicense,
                  publisher = p,
                  source = bookReplacement.source))

              })
            }
          } yield api.internal.BookId(persistedBook.id.get)
        }
      }
    }

    def newBook(newBook: api.internal.NewBook): Try[api.internal.BookId] = {
      val optPublisher = publisherRepository.withName(newBook.publisher) match {
        case Some(x) => Some(x)
        case None => Some(Publisher(None, None, newBook.publisher))
      }

      for {
        validLicense <- Try(License(newBook.license))
        validPublisher <- validationService.validatePublisher(optPublisher)
        persistedBook <- inTransaction { implicit session =>
          val persistedPublisher = validPublisher.id match {
            case None => Try(publisherRepository.add(validPublisher))
            case Some(_) => Success(validPublisher)
          }

          persistedPublisher.flatMap(p => {
            val toAdd = Book(
              id = None,
              revision = None,
              publisherId = p.id.get,
              publisher = p,
              license = validLicense,
              source = newBook.source)

            Try(bookRepository.add(toAdd))

          })
        }
      } yield api.internal.BookId(persistedBook.id.get)
    }

    def newTranslationForBook(originalBookId: Long, language: LanguageTag, translateRequest: domain.TranslateRequest, translationStatus: TranslationStatus.Value = TranslationStatus.IN_PROGRESS): Try[domain.Translation] = {
      unFlaggedTranslationsRepository.forBookIdAndLanguage(originalBookId, language) match {
        case None => Failure(new NotFoundException())
        case Some(translation) => {


          val newTranslation = translation.copy(
            id = None,
            revision = None,
            externalId = None,
            uuid = UUID.randomUUID().toString,
            language = LanguageTag(translateRequest.toLanguage),
            translatedFrom = Some(LanguageTag(translateRequest.fromLanguage)),
            publishingStatus = PublishingStatus.UNLISTED,
            translationStatus = Some(translationStatus))

          Try {
            inTransaction { implicit session =>
              val persistedTranslation = unFlaggedTranslationsRepository.add(newTranslation)
              val persistedContributors = translation.contributors.filter(_.`type` != ContributorType.Translator)
                .map(ctb => contributorRepository.add(ctb.copy(id = None, revision = None, translationId = persistedTranslation.id.get)))

              val persistedChapters = translation.chapters.map(chapterToCopy => {
                val newChapter = chapterToCopy.copy(
                  id = None,
                  revision = None,
                  translationId = persistedTranslation.id.get)

                chapterRepository.add(newChapter)
              })
              persistedTranslation.copy(chapters = persistedChapters, contributors = persistedContributors)
            }
          }
        }
      }
    }

    def deleteTranslation(translation: domain.Translation): Unit = inTransaction { implicit session =>
      translation.chapters.foreach(chapterRepository.deleteChapter)
      translation.contributors.foreach(contributorRepository.remove)
      unFlaggedTranslationsRepository.deleteTranslation(translation)
    }


    def newTranslationForBook(bookId: Long, newTranslation: api.internal.NewTranslation): Try[api.internal.TranslationId] = {
      validationService.validateNewTranslation(newTranslation).map(validNewTranslation => {
        val domainTranslation = converterService.toDomainTranslation(validNewTranslation, bookId)

        val categories = validNewTranslation.categories.map(cat => {
          categoryRepository.withName(cat.name) match {
            case Some(category) => category
            case None => Category(None, None, cat.name)
          }
        })

        val contributerToPerson = validNewTranslation.contributors.map(ctb => {
          personRepository.withName(ctb.person.name) match {
            case Some(person) => (ctb, person)
            case None => (ctb, Person(None, None, ctb.person.name, None))
          }
        })

        val translation = inTransaction { implicit session =>
          val persistedCategories = categories.map {
            case x if x.id.isEmpty => categoryRepository.add(x)
            case y => y
          }

          val optPersistedEA = domainTranslation.educationalAlignment.flatMap(ea => {
            educationalAlignmentRepository.add(ea).id
          })

          val translation = unFlaggedTranslationsRepository.add(
            domainTranslation.copy(
              categoryIds = persistedCategories.map(_.id.get),
              eaId = optPersistedEA)
          )

          val persistedContributorsToPersons = contributerToPerson.map {
            case (ctb, persisted) if persisted.id.isDefined => (ctb, persisted)
            case (ctb, unpersisted) => (ctb, personRepository.add(unpersisted))
          }

          val persistedContributors = persistedContributorsToPersons.map { case (ctb, person) => {
            contributorRepository.add(
              Contributor(
                None,
                None,
                person.id.get,
                translation.id.get,
                ContributorType.valueOf(ctb.`type`).get,
                person))
          }
          }
          indexService.indexDocument(translation)
          translation
        }

        val persistedChapters: Seq[Try[ChapterId]] = validNewTranslation.chapters.map(chapter => {
          newChapter(translation.id.get, chapter)
        })

        api.internal.TranslationId(translation.id.get)
      })
    }

    def updateTranslationForBook(bookId: Long, translationId: Long, translationReplacement: NewTranslation): Option[Try[api.internal.TranslationId]] = {
      unFlaggedTranslationsRepository.withId(translationId).map(existing => {
        validationService.validateNewTranslation(translationReplacement).map(validTranslationReplacement => {
          val replacement = converterService.toDomainTranslation(validTranslationReplacement, bookId)

          val categories = validTranslationReplacement.categories.map(cat => {
            categoryRepository.withName(cat.name) match {
              case Some(category) => category
              case None => Category(None, None, cat.name)
            }
          })

          val contributerToPerson = validTranslationReplacement.contributors.map(ctb => {
            personRepository.withName(ctb.person.name) match {
              case Some(person) => (ctb, person)
              case None => (ctb, Person(None, None, ctb.person.name, None))
            }
          })

          inTransaction { implicit session =>
            val persistedCategories = categories.map {
              case x if x.id.isEmpty => categoryRepository.add(x)
              case y => y
            }

            val optPersistedEA = (existing.educationalAlignment, replacement.educationalAlignment) match {
              case (Some(existingEa), Some(replacementEa)) => educationalAlignmentRepository.updateEducationalAlignment(existingEa.copy(
                alignmentType = replacementEa.alignmentType,
                educationalFramework = replacementEa.educationalFramework,
                targetDescription = replacementEa.targetDescription,
                targetName = replacementEa.targetName,
                targetUrl = replacementEa.targetUrl)).id

              case (Some(existingEa), None) =>
                educationalAlignmentRepository.remove(existingEa.id)
                None
              case (None, Some(ea)) => educationalAlignmentRepository.add(ea).id
              case (None, None) => None
            }

            val translation = unFlaggedTranslationsRepository.updateTranslation(
              existing.copy(
                categoryIds = persistedCategories.map(_.id.get),
                eaId = optPersistedEA,
                title = replacement.title,
                about = replacement.about,
                numPages = replacement.numPages,
                language = replacement.language,
                datePublished = replacement.datePublished,
                dateCreated = replacement.dateCreated,
                coverphoto = replacement.coverphoto,
                tags = replacement.tags,
                isBasedOnUrl = replacement.isBasedOnUrl,
                educationalUse = replacement.educationalUse,
                educationalRole = replacement.educationalRole,
                timeRequired = replacement.timeRequired,
                typicalAgeRange = replacement.typicalAgeRange,
                readingLevel = replacement.readingLevel,
                interactivityType = replacement.interactivityType,
                learningResourceType = replacement.learningResourceType,
                accessibilityApi = replacement.accessibilityApi,
                accessibilityControl = replacement.accessibilityControl,
                accessibilityFeature = replacement.accessibilityFeature,
                accessibilityHazard = replacement.accessibilityHazard,
                bookFormat = replacement.bookFormat,
                pageOrientation = replacement.pageOrientation,
                additionalInformation = replacement.additionalInformation,
                officiallyApproved = replacement.officiallyApproved
              )
            )

            val persistedContributorsToPersons = contributerToPerson.map {
              case (ctb, persisted) if persisted.id.isDefined => (ctb, persisted)
              case (ctb, unpersisted) => (ctb, personRepository.add(unpersisted))
            }


            val persistedContributors = persistedContributorsToPersons.map { case (ctb, person) => {
              contributorRepository.add(
                Contributor(
                  None,
                  None,
                  person.id.get,
                  translation.id.get,
                  ContributorType.valueOf(ctb.`type`).get,
                  person))
            }
            }
            existing.contributors.foreach(contributorRepository.remove)

            validTranslationReplacement.chapters.map(chapter => {
              chapterRepository.forTranslationWithSeqNo(translation.id.get, chapter.seqNo) match {
                case Some(existingChapter) => updateChapter(existingChapter.id.get, chapter)
                case None => newChapter(translation.id.get, chapter)
              }
            })

            // Remove exceeding chapters if the update contains fewer chapters than the existing version
            chapterRepository.deleteChaptersExceptGivenSeqNumbers(translation.id.get, validTranslationReplacement.chapters.map(_.seqNo))

            indexService.indexDocument(translation)
            api.internal.TranslationId(translation.id.get)
          }

        })
      })
    }

    def addInTransportMark(book: api.Book): Try[Unit] = {
      unFlaggedTranslationsRepository.forBookIdAndLanguage(book.id, LanguageTag(book.language.code)) match {
        case None => Failure(new NotFoundException())
        case Some(inTransport) if inTransport.inTransport => Failure(CrowdinException("Book is currently being transported to Translation system"))
        case Some(translation) if !translation.inTransport => Try(unFlaggedTranslationsRepository.updateTranslation(translation.copy(inTransport = true)))
      }
    }

    def removeInTransportMark(book: api.Book): Try[Unit] = {
      unFlaggedTranslationsRepository
        .forBookIdAndLanguage(book.id, LanguageTag(book.language.code))
        .filter(_.inTransport)
        .map(tr => Try(unFlaggedTranslationsRepository.updateTranslation(tr.copy(inTransport = false))))  match {
        case None => Success()
        case Some(Success(_)) => Success()
        case Some(Failure(err)) => Failure(err)
      }
    }

    private def getTranslationRepository: TranslationRepository = {
      if(AuthUser.hasRole(BookApiProperties.RoleWithWriteAccess)) {
        allTranslationsRepository
      } else {
        unFlaggedTranslationsRepository
      }
    }

  }

}
