/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import no.gdl.bookapi.model._
import no.gdl.bookapi.model.domain._

trait ReadService {
  this: ConverterService =>
  val readService: ReadService

  class ReadService {

    def withLanguage(language: String, pageSize: Int, page: Int): api.SearchResult = {
      val searchResult = Translation.bookIdsWithLanguage(language, pageSize, page)
      val books = searchResult.results.flatMap(id => withIdAndLanguage(id, language))

      api.SearchResult(
        books.length,
        searchResult.page,
        searchResult.pageSize,
        converterService.toApiLanguage(searchResult.language),
        books)
    }

    def withIdAndLanguage(bookId: Long, language: String): Option[api.Book] = {
      val translation = Translation.forBookIdAndLanguage(bookId, language)
      val availableLanguages: Seq[String] = Translation.languagesFor(bookId)
      val book: Option[domain.Book] = Book.withId(bookId)

      converterService.toApiBook(translation, availableLanguages, book)
    }

    def chaptersForIdAndLanguage(bookId: Long, language: String): Seq[api.ChapterSummary] = {
      Chapter.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: String, chapterId: Long): Option[api.Chapter] = {
      Chapter.chapterForBookWithLanguageAndId(bookId, language, chapterId).map(converterService.toApiChapter)
    }

    def withExternalId(externalId: Option[String]): Option[api.Book] = {
      externalId.flatMap(Translation.withExternalId) match {
        case Some(translation) => converterService.toApiBook(
          Option(translation),
          Translation.languagesFor(translation.bookId),
          Book.withId(translation.bookId))

        case None => None
      }
    }
  }
}
