/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import no.gdl.bookapi.model._
import no.gdl.bookapi.model.domain.Book
import no.gdl.bookapi.repository.BooksRepository

trait ReadService {
  this: BooksRepository with ConverterService =>
  val readService: ReadService

  class ReadService {

    def personWithName(name: String): Option[domain.Person] = {
      booksRepository.personWithName(name)
    }

    def categoryWithName(name: String): Option[domain.Category] = {
      booksRepository.categoryWithName(name)
    }

    def publisherWithName(publisher: String): Option[domain.Publisher] = {
      booksRepository.publisherWithName(publisher)
    }

    def licenseWithKey(license: String): Option[domain.License] = {
      booksRepository.licenseWithName(license)
    }

    def withLanguage(language: String, pageSize: Int, page: Int): api.SearchResult = {
      val searchResult = booksRepository.bookIdsWithLanguage(language, pageSize, page)
      val books = searchResult.results.flatMap(id => withIdAndLanguage(id, language))

      api.SearchResult(
        books.length,
        searchResult.page,
        searchResult.pageSize,
        converterService.toApiLanguage(searchResult.language),
        books)
    }

    def withIdAndLanguage(bookId: Long, language: String): Option[api.Book] = {
      val translation = booksRepository.translationForBookIdAndLanguage(bookId, language)
      val availableLanguages: Seq[String] = booksRepository.languagesFor(bookId)
      val book: Option[Book] = booksRepository.withId(bookId)

      converterService.toApiBook(translation, availableLanguages, book)
    }

    def chaptersForIdAndLanguage(bookId: Long, language: String): Seq[api.ChapterSummary] = {
      booksRepository.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: String, chapterId: Long): Option[api.Chapter] = {
      booksRepository.chapterForBookWithLanguageAndId(bookId, language, chapterId).map(converterService.toApiChapter)
    }

    def withExternalId(externalId: Option[String]): Option[api.Book] = {
      externalId.flatMap(booksRepository.translationWithExternalId) match {
        case Some(translation) => converterService.toApiBook(
          Option(translation),
          booksRepository.languagesFor(translation.bookId),
          booksRepository.withId(translation.bookId))

        case None => None
      }
    }
  }
}
