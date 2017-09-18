/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.domain._
import no.gdl.bookapi.repository.TransactionHandler

import scala.util.{Success, Try}


trait WriteService {
  this: TransactionHandler with ConverterService with ValidationService with ReadService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def newChapter(translationId: Long, newChapter: api.internal.NewChapter): Try[api.internal.ChapterId] = {
      for {
        valid <- validationService.validateChapter(converterService.toDomainChapter(newChapter, translationId))
        persisted <- Try(Chapter.add(valid))
      } yield api.internal.ChapterId(persisted.id.get)
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

        val persistedContributors = persistedContributorsToPersons.map{ case (ctb, person) => {
          Contributor.add(
            Contributor(
              None,
              None,
              person.id.get,
              translation.id.get,
              ctb.`type`,
              person))
        }}

        Success(api.internal.TranslationId(translation.id.get))
      }
    }
  }
}
