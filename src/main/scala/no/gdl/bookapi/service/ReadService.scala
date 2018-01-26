/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.network.AuthUser
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.api.MyBook
import no.gdl.bookapi.model.domain.{PublishingStatus, Sort}
import no.gdl.bookapi.repository._

trait ReadService {
  this: ConverterService with BookRepository with ChapterRepository with TranslationRepository with InTranslationRepository
  with FeaturedContentRepository =>
  val readService: ReadService

  class ReadService {
    def featuredContentForLanguage(tag: LanguageTag): Seq[api.FeaturedContent] = {
      featuredContentRepository.forLanguage(tag).map(fc => {
        api.FeaturedContent(
          fc.id.get,
          fc.revision.get,
          converterService.toApiLanguage(fc.language),
          fc.title,
          fc.description,
          fc.link,
          fc.imageUrl)
      })
    }

    def listAvailableLanguages: Seq[api.Language] = {
      translationRepository.allAvailableLanguages().map(converterService.toApiLanguage).sortBy(_.name)
    }

    def listAvailableLevelsForLanguage(lang: Option[LanguageTag] = None): Seq[String] =
      translationRepository.allAvailableLevels(lang)


    def withLanguageAndLevelAndStatus(language: LanguageTag, readingLevel: Option[String], status: PublishingStatus.Value, pageSize: Int, page: Int, sort: Sort.Value): api.SearchResult = {
      val searchResult = translationRepository
        .bookIdsWithLanguageAndLevelAndStatus(language, readingLevel, status, pageSize, page, sort)

      val books = searchResult.results.flatMap(id => withIdAndLanguage(id, language))

      api.SearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        converterService.toApiLanguage(language),
        books)
    }

    def forUserWithLanguage(userId: String, pageSize: Int, page: Int, sort: Sort.Value): Seq[api.MyBook] = {
      val inTranslationForUser = inTranslationRepository.inTranslationForUser(userId)
      inTranslationForUser.filter(_.newTranslationId.isDefined).flatMap(inTranslation => {
        translationRepository.withId(inTranslation.newTranslationId.get).flatMap(newTranslation => {
          val availableLanguages = translationRepository.languagesFor(newTranslation.bookId)
          withIdAndLanguage(newTranslation.bookId, inTranslation.fromLanguage).map(book => {
            converterService.toApiMyBook(inTranslation, newTranslation, availableLanguages, book)
          })
        })
      })
    }

    def withLanguageAndStatus(language: LanguageTag, status: PublishingStatus.Value, pageSize: Int, page: Int, sort: Sort.Value): api.SearchResult = {
      val searchResult = translationRepository.bookIdsWithLanguageAndStatus(language, status, pageSize, page, sort)
      val books = searchResult.results.flatMap(id => withIdAndLanguage(id, language))

      api.SearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        converterService.toApiLanguage(searchResult.language),
        books)
    }

    def withIdAndLanguage(bookId: Long, language: LanguageTag): Option[api.Book] = {
      val translation = translationRepository.forBookIdAndLanguage(bookId, language)
      val availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(bookId)
      val book: Option[domain.Book] = bookRepository.withId(bookId)

      converterService.toApiBook(translation, availableLanguages, book)
    }

    def similarTo(bookId: Long, language: LanguageTag, pageSize: Int, page: Int, sort: Sort.Value): api.SearchResult = {
      withIdAndLanguage(bookId, language) match {
        case None => api.SearchResult(0, page, pageSize, converterService.toApiLanguage(language), Seq())
        case Some(book) =>
          val searchResult = translationRepository
            .bookIdsWithLanguageAndLevelAndStatus(language, book.readingLevel, PublishingStatus.PUBLISHED, pageSize, page, sort)

          val books = searchResult.results
            .flatMap(id => withIdAndLanguage(id, language))
            .filter(_.id != bookId)

          api.SearchResult(
            searchResult.totalCount,
            searchResult.page,
            searchResult.pageSize,
            converterService.toApiLanguage(language),
            books)
      }
    }

    def chaptersForIdAndLanguage(bookId: Long, language: LanguageTag): Seq[api.ChapterSummary] = {
      chapterRepository.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: LanguageTag, chapterId: Long): Option[api.Chapter] = {
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
