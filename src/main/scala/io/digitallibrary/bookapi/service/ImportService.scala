package io.digitallibrary.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.domain.ContributorType
import io.digitallibrary.bookapi.repository._
import scalikejdbc.{AutoSession, DBSession}

import scala.util.{Failure, Success, Try}

trait ImportService {
  this: TransactionHandler
    with LicenseRepository
    with PublisherRepository
    with BookRepository
    with CategoryRepository
    with ValidationService
    with TranslationRepository
    with PersonRepository
    with ContributorRepository
    with ChapterRepository
    with ConverterService =>

  val importService: ImportService

  class ImportService extends LazyLogging {

    def importBook(book: api.internal.Book): Try[api.internal.TranslationId] = {
      inTransaction { implicit session =>
        for {
          license               <- validLicense(book)
          publisher             <- validPublisher(book)
          persistedPublisher    <- persistPublisher(publisher)
          persistedBook         <- persistBook(book, license, persistedPublisher)
          persistedTranslation  <- persistTranslation(persistedBook, book)
          _                     <- persistChapters(book, persistedTranslation)
        } yield api.internal.TranslationId(persistedTranslation.id.get)
      }
    }

    private def persistChapters(book: api.internal.Book, translation: domain.Translation)(implicit session: DBSession = AutoSession): Try[Seq[domain.Chapter]] = {
      Try {
        book.chapters.map(chapter => {
          chapterRepository.add(converterService.toDomainChapter(chapter, translation.id.get))
        })
      }
    }

    private def persistTranslation(persistedBook: domain.Book, newBook: api.internal.Book)(implicit session: DBSession = AutoSession): Try[domain.Translation] = {
      for {
          validCategories <- validCategories(newBook)
          domainTranslation <- Success(converterService.toDomainTranslation(newBook, persistedBook, validCategories))
          persistedTranslation <- Try(translationRepository.add(domainTranslation))
          persistedContributors <- persistContributors(newBook, persistedTranslation)
        } yield persistedTranslation.copy(contributors = persistedContributors)
    }

    private def persistContributors(newBook: api.internal.Book, translation: domain.Translation)(implicit session: DBSession = AutoSession): Try[Seq[domain.Contributor]] = {
      Try(
        newBook.contributors.map(contributor => {
          val person: domain.Person = personRepository.withName(contributor.name) match {
            case None => personRepository.add(domain.Person(None, None, contributor.name, None))
            case Some(x) => x
          }
          contributorRepository.add(new domain.Contributor(None, None, person.id.get, translation.id.get, ContributorType.valueOf(contributor.`type`).get, person))
        })
      )
    }

    private def validCategories(book: api.internal.Book): Try[Seq[domain.Category]] = {
      val validCategories = book.categories.flatMap(cat => categoryRepository.withName(cat.name))
      if(validCategories.lengthCompare(book.categories.size) != 0) {
        Failure(new RuntimeException("adsfasdf"))
      } else {
        Success(validCategories)

      }
    }

    private def persistBook(book: api.internal.Book, license: domain.License, publisher: domain.Publisher)(implicit session: DBSession = AutoSession): Try[domain.Book] = {
      val toPersist = domain.Book(
        id = None,
        revision = None,
        publisherId = publisher.id.get,
        publisher = publisher,
        licenseId = license.id.get,
        license = license,
        source = book.source)

      Try(bookRepository.add(toPersist))

    }

    private def validLicense(book: api.internal.Book): Try[domain.License] = {
      validationService.validateLicense(licenseRepository.withName(book.license.name))
    }

    private def persistPublisher(publisher: domain.Publisher)(implicit session: DBSession = AutoSession): Try[domain.Publisher] = {
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
