/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.model.api.{NewBook, NewBookInLanguage, Book}
import no.gdl.bookapi.model.domain.{CoverPhoto, Downloads, BookInLanguage}
import no.gdl.bookapi.repository.BooksRepository

import scala.util.{Failure, Success, Try}


trait WriteService {
  this: BooksRepository with ConverterService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def newBookInLanguage(id: Long, bookInLanguage: NewBookInLanguage): Try[Book] = {
      booksRepository.insertBookInLanguage(
        converterService.toDomainBookInLanguage(bookInLanguage).copy(bookId = Some(id)))

      booksRepository.withId(id).flatMap(a => converterService.toApiBook(a, bookInLanguage.language)) match {
        case Some(x) => Success(x)
        case None => Failure(new RuntimeException(s"Could not read newly inserted book for language ${bookInLanguage.language}"))
      }
    }

    def newBook(newBook: NewBook): Try[Book] = {
      converterService.toApiBook(booksRepository.insertBook(
        converterService.toDomainBook(newBook)), newBook.language) match {
        case Some(x) => Success(x)
        case None => Failure(new RuntimeException(s"Could not read newly inserted book for langeuage ${newBook.language}"))
      }
    }

    def updateBookInLanguage(existing: BookInLanguage, newTranslation: NewBookInLanguage): BookInLanguage = {
      val newDomainTranslation = converterService.toDomainBookInLanguage(newTranslation)
      val toUpdate = existing.copy(
        title = newDomainTranslation.title,
        description = newDomainTranslation.description,
        coverPhoto = newDomainTranslation.coverPhoto,
        downloads = newDomainTranslation.downloads,
        dateCreated = newDomainTranslation.dateCreated,
        datePublished = newDomainTranslation.datePublished,
        tags = newDomainTranslation.tags,
        authors = newDomainTranslation.authors,
        language = newDomainTranslation.language)

      val updated = booksRepository.updateBookInLanguage(toUpdate)
      logger.info(s"Updated book-in-language with id = ${updated.id}")
      updated
    }

    def updateBook(existing: no.gdl.bookapi.model.domain.Book, newBook: NewBook): Book = {
      val inLanguageToKeep = existing.bookInLanguage.filterNot(_.language == newBook.language)
      val inLanguageToUpdate = existing.bookInLanguage.find(_.language == newBook.language)
        .map(_.copy(title = newBook.title,
          description = newBook.description,
          language = newBook.language,
          coverPhoto = CoverPhoto(newBook.coverPhoto.large, newBook.coverPhoto.small),
          downloads = Downloads(newBook.downloads.epub),
          dateCreated = newBook.dateCreated,
          datePublished = newBook.datePublished,
          tags = newBook.tags,
          authors = newBook.authors))


      val toUpdate = existing.copy(
        title = newBook.title,
        description = newBook.description,
        language = newBook.language,
        license = converterService.licenses.getOrElse(newBook.license, converterService.DefaultLicense).license,
        publisher = newBook.publisher,
        readingLevel = newBook.readingLevel,
        typicalAgeRange = newBook.typicalAgeRange,
        educationalUse = newBook.educationalUse,
        educationalRole = newBook.educationalRole,
        timeRequired = newBook.timeRequired,
        categories = newBook.categories,
        bookInLanguage = inLanguageToKeep ++ inLanguageToUpdate)

      logger.info(s"Updated book with id = ${existing.id}")
      converterService.toApiBook(booksRepository.updateBook(toUpdate), newBook.language).get
    }
  }

}
