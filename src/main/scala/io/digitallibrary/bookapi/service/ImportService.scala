/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.{ValidationException, ValidationMessage}
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.bookapi.service.search.IndexService
import io.digitallibrary.license.model.License
import scalikejdbc.{AutoSession, DBSession}

import scala.util.{Failure, Success, Try}

trait ImportService {
  this: TransactionHandler
    with PublisherRepository
    with BookRepository
    with CategoryRepository
    with ValidationService
    with TranslationRepository
    with PersonRepository
    with ContributorRepository
    with ChapterRepository
    with ConverterService
    with InTranslationRepository
    with IndexService =>

  val importService: ImportService

  class ImportService extends LazyLogging {

    def importBook(book: api.internal.Book): Try[api.internal.TranslationId] = {
      val persistedTranslation = book.externalId.flatMap(unFlaggedTranslationsRepository.withExternalId) match {
        case None => addBook(book)
        case Some(existingTranslation) => updateBook(book, existingTranslation)
      }

      persistedTranslation
        .flatMap(indexService.indexDocument)
        .map(x => api.internal.TranslationId(x.id.get))
    }

    private def addBook(book: api.internal.Book): Try[domain.Translation] = {
      inTransaction { implicit session =>
        for {
          license <- validLicense(book)
          publisher <- validPublisher(book)
          persistedPublisher <- persistPublisher(publisher)
          persistedBook <- persistBook(book, license, persistedPublisher)
          persistedTranslation <- persistTranslation(persistedBook, book)
          _ <- persistChapters(book, persistedTranslation)
        } yield persistedTranslation
      }
    }

    private def updateBook(book: api.internal.Book, existingTranslation: domain.Translation): Try[domain.Translation] = {
      if (inTranslationRepository.forOriginalId(book.id).nonEmpty) {
        Failure(new RuntimeException(s"Book with id ${book.id} is currently being translated. Cannot update at the moment."))
      } else {
        bookRepository.withId(existingTranslation.bookId) match {
          case None => Failure(new api.NotFoundException(s"The book with id ${existingTranslation.bookId} was not found. Cannot update."))
          case Some(originalBook) => inTransaction { implicit session =>
            for {
              license <- validLicense(book)
              publisher <- validPublisher(book)
              persistedPublisher <- persistPublisher(publisher)
              persistedBook <- persistBookUpdate(originalBook, book, license, persistedPublisher)
              persistedTranslation <- persistTranslationUpdate(persistedBook, existingTranslation, book)
              _ <- persistChapterUpdates(book, persistedTranslation)
            } yield persistedTranslation

          }
        }
      }
    }

    def importBookAsTranslation(newTranslation: api.internal.Book, bookId: Long): Try[api.internal.TranslationId] = {
      bookRepository.withId(bookId) match {
        case None => Failure(new api.NotFoundException(s"The book with id $bookId was not found. Cannot update."))
        case Some(book) => {
          newTranslation.externalId.flatMap(unFlaggedTranslationsRepository.withExternalId) match {
            case None => addBookAsTranslation(newTranslation, book)
            case Some(existingTranslation) => updateBookAsTranslation(newTranslation, existingTranslation, book)
          }
        }
      }
    }

    def addBookAsTranslation(newTranslation: api.internal.Book, book: domain.Book): Try[api.internal.TranslationId] = {
      inTransaction { implicit session =>
        for {
          persistedTranslation <- persistTranslation(book, newTranslation)
          _ <- persistChapters(newTranslation, persistedTranslation)
        } yield api.internal.TranslationId(persistedTranslation.id.get)
      }
    }

    def updateBookAsTranslation(newTranslation: api.internal.Book, existingTranslation: domain.Translation, book: domain.Book): Try[api.internal.TranslationId] = {
      if(inTranslationRepository.forOriginalId(existingTranslation.bookId).exists(_.fromLanguage == existingTranslation.language)) {
        Failure(new RuntimeException(s"Book with id ${book.id} is currently being translated. Cannot update at the moment."))
      } else {
        inTransaction { implicit session =>
          for {
            persistedTranslation <- persistTranslationUpdate(book, existingTranslation, newTranslation)
            _ <- persistChapterUpdates(newTranslation, existingTranslation)
          } yield api.internal.TranslationId(persistedTranslation.id.get)
        }
      }
    }

    private def persistChapters(book: api.internal.Book, translation: domain.Translation)(implicit session: DBSession = AutoSession): Try[Seq[domain.Chapter]] = {
      Try {
        book.chapters.map(chapter => {
          chapterRepository.add(converterService.toDomainChapter(chapter, translation.id.get))
        })
      }
    }

    def persistChapterUpdates(book: api.internal.Book, translation: domain.Translation)(implicit session: DBSession = AutoSession): Try[Seq[domain.Chapter]] = {
      Try {
        inTransaction { implicit session =>
          chapterRepository.deleteChaptersExceptGivenSeqNumbers(translation.id.get, book.chapters.map(_.seqNo))
          book.chapters.map(toUpdate => {
            chapterRepository.forTranslationWithSeqNo(translation.id.get, toUpdate.seqNo) match {
              case Some(chapter) => chapterRepository.updateChapter(converterService.mergeChapter(chapter, toUpdate))
              case None => chapterRepository.add(converterService.toDomainChapter(toUpdate, translation.id.get))
            }
          })
        }
      }
    }

    private def persistTranslation(persistedBook: domain.Book, newBook: api.internal.Book)(implicit session: DBSession = AutoSession): Try[domain.Translation] = {
      for {
        validCategories <- validCategories(newBook)
        domainTranslation = converterService.toDomainTranslation(newBook, persistedBook, validCategories)
        persistedTranslation <- Try(unFlaggedTranslationsRepository.add(domainTranslation))
        persistedContributors <- persistContributors(newBook.contributors, persistedTranslation)
      } yield persistedTranslation.copy(contributors = persistedContributors)
    }


    def persistTranslationUpdate(persistedBook: domain.Book, existingTranslation: domain.Translation, newTranslation: api.internal.Book): Try[domain.Translation] = {
      for {
        validCategories <- validCategories(newTranslation)
        domainTranslation <- Success(converterService.mergeTranslation(existingTranslation, newTranslation, validCategories))
        persistedTranslation <- Try(unFlaggedTranslationsRepository.updateTranslation(domainTranslation))
        persistedContributors <- persistContributorsUpdate(persistedTranslation, newTranslation)
      } yield persistedTranslation.copy(contributors = persistedContributors)
    }

    def persistContributors(contributors: Seq[api.Contributor], translation: domain.Translation)(implicit session: DBSession = AutoSession): Try[Seq[domain.Contributor]] = {
      Try(
        contributors.map(contributor => {
          val person: domain.Person = personRepository.withName(contributor.name) match {
            case None => personRepository.add(domain.Person(None, None, contributor.name, None))
            case Some(x) => x
          }
          contributorRepository.add(new domain.Contributor(None, None, person.id.get, translation.id.get, domain.ContributorType.valueOf(contributor.`type`).get, person))
        })
      )
    }

    def persistContributorsUpdate(persistedTranslation: domain.Translation, book: api.internal.Book)(implicit session: DBSession = AutoSession): Try[Seq[domain.Contributor]] = {
      val (toKeep, toDelete) = persistedTranslation.contributors.partition(ctb => book.contributors.exists(tf => tf.`type` == ctb.`type`.toString && tf.name == ctb.person.name))
      val toAdd = book.contributors.filterNot(newCtb => persistedTranslation.contributors.exists(ctb => ctb.`type`.toString == newCtb.`type` && ctb.person.name == newCtb.name))

      for {
        _ <- Try(toDelete.foreach(contributorRepository.remove))
        addedContributors <- persistContributors(toAdd, persistedTranslation)
      } yield toKeep ++ addedContributors
    }

    def validCategories(book: api.internal.Book): Try[Seq[domain.Category]] = {
      val possibleCategories = book.categories.map(cat => {
        categoryRepository.withName(cat.name) match {
          case Some(category) => Right(category)
          case None => Left(ValidationMessage("categories", s"${cat.name} is not a valid category."))
        }
      })

      possibleCategories.filter(_.isLeft).map(_.left.get) match {
        case first :: rest => Failure(new ValidationException(errors = first :: rest))
        case _ => Success(possibleCategories.map(_.right.get))
      }
    }

    private def persistBook(book: api.internal.Book, license: License, publisher: domain.Publisher)(implicit session: DBSession = AutoSession): Try[domain.Book] = {
      Try(
        bookRepository.add(
          domain.Book(
            id = None,
            revision = None,
            publisherId = publisher.id.get,
            publisher = publisher,
            license = license,
            source = book.source)))

    }

    def persistBookUpdate(originalBook: domain.Book, book: api.internal.Book, license: License, persistedPubliser: domain.Publisher): Try[domain.Book] = {
      bookRepository.updateBook(
        originalBook.copy(
          publisherId = persistedPubliser.id.get,
          publisher = persistedPubliser,
          license = license,
          source = book.source))
    }

    private def validLicense(book: api.internal.Book): Try[License] = {
      validationService.validateLicense(book.license.name)
    }

    def persistPublisher(publisher: domain.Publisher)(implicit session: DBSession = AutoSession): Try[domain.Publisher] = {
      publisher.id match {
        case None => Try(publisherRepository.add(publisher))
        case Some(_) => Success(publisher)
      }
    }

    private def validPublisher(book: api.internal.Book): Try[domain.Publisher] = {
      val optPublisher = publisherRepository.withName(book.publisher.name) match {
        case Some(x) => Some(x)
        case None => Some(domain.Publisher(None, None, book.publisher.name))
      }

      validationService.validatePublisher(optPublisher)
    }

  }

}
