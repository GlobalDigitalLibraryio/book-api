package io.digitallibrary.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.model._
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
    with ConverterService
    with InTranslationRepository =>

  val importService: ImportService

  class ImportService extends LazyLogging {

    def importBook(book: api.internal.Book): Try[api.internal.TranslationId] = {
      book.externalId.flatMap(translationRepository.withExternalId) match {
        case None => addBook(book)
        case Some(existingTranslation) => updateBook(book, existingTranslation)
      }
    }

    private def addBook(book: api.internal.Book): Try[api.internal.TranslationId] = {
      logger.info(s"IMPORTING ${book.title} (${book.externalId})")
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

    private def updateBook(book: api.internal.Book, existingTranslation: domain.Translation): Try[api.internal.TranslationId] = {
      logger.info(s"UPDATING ${book.title} (${book.externalId})")
      if (inTranslationRepository.forOriginalId(book.id).nonEmpty) {
        Failure(new RuntimeException("We do not support updating books that are currently being translated...."))
      } else {
        bookRepository.withId(existingTranslation.bookId) match {
          case None => Failure(new RuntimeException("Cannot find original book"))
          case Some(originalBook) => {
            for {
              license              <- validLicense(book)
              publisher            <- validPublisher(book)
              persistedPublisher   <- persistPublisher(publisher)
              persistedBook        <- persistBookUpdate(originalBook, book, license, persistedPublisher)
              persistedTranslation <- persistTranslationUpdate(persistedBook, existingTranslation, book)
              _                    <- persistChapterUpdates(book, persistedTranslation)
            } yield api.internal.TranslationId(persistedTranslation.id.get)

          }
        }
      }
    }

    def importBookAsTranslation(book: api.internal.Book, bookId: Long): Try[api.internal.TranslationId] = {
      bookRepository.withId(bookId) match {
        case None => Failure(new RuntimeException("TODO - Finner ikke boken"))
        case Some(persistedBook) => inTransaction { implicit session =>
          for {
            persistedTranslation  <- persistTranslation(persistedBook, book)
            _                     <- persistChapters(book, persistedTranslation)
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

    private def persistChapterUpdates(book: api.internal.Book, translation: domain.Translation)(implicit session: DBSession = AutoSession): Try[Seq[domain.Chapter]] = {
      Try {
        book.chapters.map(toUpdate => {
          chapterRepository.forTranslationWithSeqNo(translation.id.get, toUpdate.seqNo) match {
            case Some(chapter) => chapterRepository.updateChapter(converterService.mergeChapter(chapter, toUpdate))
            case None => chapterRepository.add(converterService.toDomainChapter(toUpdate, translation.id.get))
          }
        })
      }
    }

    private def persistTranslation(persistedBook: domain.Book, newBook: api.internal.Book)(implicit session: DBSession = AutoSession): Try[domain.Translation] = {
      for {
          validCategories <- validCategories(newBook)
          domainTranslation <- Success(converterService.toDomainTranslation(newBook, persistedBook, validCategories))
          persistedTranslation <- Try(translationRepository.add(domainTranslation))
          persistedContributors <- persistContributors(newBook.contributors, persistedTranslation)
        } yield persistedTranslation.copy(contributors = persistedContributors)
    }


    def persistTranslationUpdate(persistedBook: domain.Book, existingTranslation: domain.Translation, book: api.internal.Book): Try[domain.Translation] = {
      for {
        validCategories <- validCategories(book)
        domainTranslation <- Success(converterService.mergeTranslation(existingTranslation, book, validCategories))
        persistedTranslation <- Try(translationRepository.updateTranslation(domainTranslation))
        persistedContributors <- persistContributorsUpdate(persistedTranslation, book)
      } yield persistedTranslation.copy(contributors = persistedContributors)
    }

    private def persistContributors(contributors: Seq[api.Contributor], translation: domain.Translation)(implicit session: DBSession = AutoSession): Try[Seq[domain.Contributor]] = {
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
      val toDelete = persistedTranslation.contributors.filterNot(ctb => book.contributors.exists(tf => tf.`type` == ctb.`type`.toString && tf.name == ctb.person.name))
      val toKeep = persistedTranslation.contributors.filter(ctb => book.contributors.exists(tf => tf.`type` == ctb.`type`.toString && tf.name == ctb.person.name))
      val toAdd = book.contributors.filterNot(newCtb => persistedTranslation.contributors.exists(ctb => ctb.`type`.toString == newCtb.`type` && ctb.person.name == newCtb.name))

      for {
        _ <- Try(toDelete.foreach(contributorRepository.remove))
        addedContributors <- persistContributors(toAdd, persistedTranslation)
      } yield toKeep ++ addedContributors
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

    def persistBookUpdate(originalBook: domain.Book, book: api.internal.Book, license: domain.License, persistedPubliser: domain.Publisher): Try[domain.Book] = {
      val toPersist = originalBook.copy(
        publisherId = persistedPubliser.id.get,
        publisher = persistedPubliser,
        licenseId = license.id.get,
        license = license,
        source = book.source
      )

      bookRepository.updateBook(toPersist)
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
