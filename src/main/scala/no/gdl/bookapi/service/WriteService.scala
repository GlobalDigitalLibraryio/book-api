/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import java.time.LocalDate

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.NotFoundException
import no.gdl.bookapi.model.api.internal.{NewChapter, NewTranslation}
import no.gdl.bookapi.model.domain._
import no.gdl.bookapi.repository.TransactionHandler

import scala.util.{Failure, Success, Try}


trait WriteService {
  this: TransactionHandler with ConverterService with ValidationService with ReadService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {
    def updateChapter(chapterid: Long, replacementChapter: NewChapter): Option[api.internal.ChapterId] = {
      Chapter.withId(chapterid).map(existing => {
        val updated = Chapter.updateChapter(existing.copy(
          title = replacementChapter.title,
          content = replacementChapter.content))

        api.internal.ChapterId(updated.id.get)
      })
    }

    def newChapter(translationId: Long, newChapter: api.internal.NewChapter): Try[api.internal.ChapterId] = {
      for {
        valid <- validationService.validateChapter(converterService.toDomainChapter(newChapter, translationId))
        persisted <- Try(Chapter.add(valid))
      } yield api.internal.ChapterId(persisted.id.get)
    }

    def updateBook(bookId: Long, bookReplacement: api.internal.NewBook): Try[api.internal.BookId] = {
      Book.withId(bookId) match {
        case None => Failure(new NotFoundException(s"Book with id $bookId was not found"))
        case Some(existingBook) => {
          val optLicense = License.withName(bookReplacement.license)
          val optPublisher = Publisher.withName(bookReplacement.publisher) match {
            case Some(x) => Some(x)
            case None => Some(Publisher(None, None, bookReplacement.publisher))
          }

          for {
            validLicense <- validationService.validateLicense(optLicense)
            validPublisher <- validationService.validatePublisher(optPublisher)
            persistedBook <- inTransaction { implicit session =>
              val persistedPublisher = validPublisher.id match {
                case None => Try(Publisher.add(validPublisher))
                case Some(_) => Success(validPublisher)
              }

              persistedPublisher.flatMap(p => {
                existingBook.copy(
                  publisherId = p.id.get,
                  licenseId = validLicense.id.get,
                  publisher = p,
                  license = validLicense)

                Book.updateBook(existingBook)

              })
            }
          } yield api.internal.BookId(persistedBook.id.get)
        }
      }
    }

    def newBook(newBook: api.internal.NewBook): Try[api.internal.BookId] = {
      val optLicense = License.withName(newBook.license)
      val optPublisher = Publisher.withName(newBook.publisher) match {
        case Some(x) => Some(x)
        case None => Some(Publisher(None, None, newBook.publisher))
      }

      for {
        validLicense <- validationService.validateLicense(optLicense)
        validPublisher <- validationService.validatePublisher(optPublisher)
        persistedBook <- inTransaction { implicit session =>
          val persistedPublisher = validPublisher.id match {
            case None => Try(Publisher.add(validPublisher))
            case Some(_) => Success(validPublisher)
          }

          persistedPublisher.flatMap(p => {
            val newBook = Book(
              id = None,
              revision = None,
              publisherId = p.id.get,
              licenseId = p.id.get,
              publisher = p,
              license = validLicense)

            Book.add(newBook)

          })
        }
      } yield api.internal.BookId(persistedBook.id.get)
    }


    def newTranslationForBook(bookId: Long, newTranslation: api.internal.NewTranslation): Try[api.internal.TranslationId] = {
      val domainTranslation = converterService.toDomainTranslation(newTranslation, bookId)

      val categories = newTranslation.categories.map(cat => {
        Category.withName(cat.name) match {
          case Some(category) => category
          case None => Category(None, None, cat.name)
        }
      })

      val contributerToPerson = newTranslation.contributors.map(ctb => {
        Person.withName(ctb.person.name) match {
          case Some(person) => (ctb, person)
          case None => (ctb, Person(None, None, ctb.person.name))
        }
      })

      inTransaction { implicit session =>
        val persistedCategories = categories.map {
          case x if x.id.isEmpty => Category.add(x)
          case y => y
        }

        val optPersistedEA = domainTranslation.educationalAlignment.flatMap(ea => {
          EducationalAlignment.add(ea).id
        })

        val translation = Translation.add(
          domainTranslation.copy(
            categoryIds = persistedCategories.map(_.id.get),
            eaId = optPersistedEA)
        )

        val persistedContributorsToPersons = contributerToPerson.map {
          case (ctb, persisted) if persisted.id.isDefined => (ctb, persisted)
          case (ctb, unpersisted) => (ctb, Person.add(unpersisted))
        }

        val persistedContributors = persistedContributorsToPersons.map { case (ctb, person) => {
          Contributor.add(
            Contributor(
              None,
              None,
              person.id.get,
              translation.id.get,
              ctb.`type`,
              person))
        }
        }

        Success(api.internal.TranslationId(translation.id.get))
      }
    }

    def updateTranslationForBook(bookId: Long, translationId: Long, translationReplacement: NewTranslation): Option[Try[api.internal.TranslationId]] = {
      Translation.withId(translationId).map(existing => {
        val replacement = converterService.toDomainTranslation(translationReplacement, bookId)

        val categories = translationReplacement.categories.map(cat => {
          Category.withName(cat.name) match {
            case Some(category) => category
            case None => Category(None, None, cat.name)
          }
        })

        val contributerToPerson = translationReplacement.contributors.map(ctb => {
          Person.withName(ctb.person.name) match {
            case Some(person) => (ctb, person)
            case None => (ctb, Person(None, None, ctb.person.name))
          }
        })

        inTransaction { implicit session =>
          val persistedCategories = categories.map {
            case x if x.id.isEmpty => Category.add(x)
            case y => y
          }

          val optPersistedEA = (existing.educationalAlignment, replacement.educationalAlignment) match {
            case (Some(existingEa), Some(replacementEa)) => EducationalAlignment.updateEducationalAlignment(existingEa.copy(
              alignmentType = replacementEa.alignmentType,
              educationalFramework = replacementEa.educationalFramework,
              targetDescription = replacementEa.targetDescription,
              targetName = replacementEa.targetName,
              targetUrl = replacementEa.targetUrl)).id

            case (Some(existingEa), None) =>
              EducationalAlignment.remove(existingEa.id)
              None
            case (None, Some(ea)) => EducationalAlignment.add(ea).id
            case (None, None) => None
          }

          val translation = Translation.update(
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
              accessibilityHazard = replacement.accessibilityHazard
            )
          )

          val persistedContributorsToPersons = contributerToPerson.map {
            case (ctb, persisted) if persisted.id.isDefined => (ctb, persisted)
            case (ctb, unpersisted) => (ctb, Person.add(unpersisted))
          }


          val persistedContributors = persistedContributorsToPersons.map { case (ctb, person) => {
            Contributor.add(
              Contributor(
                None,
                None,
                person.id.get,
                translation.id.get,
                ctb.`type`,
                person))
          }
          }
          existing.contributors.foreach(Contributor.remove)

          Success(api.internal.TranslationId(translation.id.get))
        }
      })
    }
  }

}
