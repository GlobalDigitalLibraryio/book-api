/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import no.gdl.bookapi.model._
import no.gdl.bookapi.repository.{BookRepository, ChapterRepository, TranslationRepository}

trait ReadService {
  this: ConverterService with BookRepository with ChapterRepository with TranslationRepository =>
  val readService: ReadService

  class ReadService {
    def listAvailableLanguages: Seq[api.Language] = {
      translationRepository.allAvailableLanguages().map(converterService.toApiLanguage).sortBy(_.name)
    }

    def listAvailableLevelsForLanguage(lang: Option[String] = None): Seq[String] =
      translationRepository.allAvailableLevels(lang)


    def withLanguage(language: String, pageSize: Int, page: Int): api.SearchResult = {
      val searchResult = translationRepository.bookIdsWithLanguage(language, pageSize, page)
      val books = searchResult.results.flatMap(id => withIdAndLanguage(id, language))

      api.SearchResult(
        books.length,
        searchResult.page,
        searchResult.pageSize,
        converterService.toApiLanguage(searchResult.language),
        books)
    }

    def withIdAndLanguage(bookId: Long, language: String): Option[api.Book] = {
      val translation = translationRepository.forBookIdAndLanguage(bookId, language)
      val availableLanguages: Seq[String] = translationRepository.languagesFor(bookId)
      val book: Option[domain.Book] = bookRepository.withId(bookId)

      converterService.toApiBook(translation, availableLanguages, book)
    }

    def chaptersForIdAndLanguage(bookId: Long, language: String): Seq[api.ChapterSummary] = {
      chapterRepository.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: String, chapterId: Long): Option[api.Chapter] = {
      chapterRepository.chapterForBookWithLanguageAndId(bookId, language, chapterId).map(converterService.toApiChapter)
    }

    def chapterWithSeqNoForTranslation(translationId: Long, seqno: Long): Option[api.internal.ChapterId] = {
      chapterRepository.forTranslationWithSeqNo(translationId, seqno).flatMap(_.id).map(api.internal.ChapterId)
    }

    def translationWithExternalId(externalId: Option[String]): Option[api.internal.TranslationId] = {
      externalId
        .flatMap(translationRepository.withExternalId)
        .flatMap(_.id)
        .map(api.internal.TranslationId)
    }

    def withExternalId(externalId: Option[String]): Option[api.Book] = {
      externalId.flatMap(translationRepository.withExternalId) match {
        case Some(translation) => converterService.toApiBook(
          Option(translation),
          translationRepository.languagesFor(translation.bookId),
          bookRepository.withId(translation.bookId))

        case None => None
      }
    }
  }
}
