/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service


import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.domain.{Category, PublishingStatus, Sort}
import io.digitallibrary.bookapi.repository._

trait ReadService {
  this: ConverterService with BookRepository with ChapterRepository with TranslationRepository with InTranslationRepository
  with FeaturedContentRepository with CategoryRepository =>
  val readService: ReadService

  class ReadService {

    def listAvailablePublishedCategoriesForLanguage(language: LanguageTag): Map[Category, Set[String]] = {
      translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
    }

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

    def listAvailablePublishedLanguages: Seq[api.Language] = {
      translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED).map(converterService.toApiLanguage).sortBy(_.name)
    }

    def listAvailablePublishedLanguagesAsLanguageTags: Seq[LanguageTag] = {
      translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)
    }

    def listAvailablePublishedLevelsForLanguage(lang: Option[LanguageTag] = None, category: Option[String] = None): Seq[String] = {
      val cat = category.flatMap(categoryRepository.withName)
      translationRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, lang, cat)
    }


    def forUserWithLanguage(userId: String, pageSize: Int, page: Int, sort: Sort.Value): Seq[api.MyBook] = {
      val inTranslationForUser = inTranslationRepository.inTranslationForUser(userId)
      for {
        inTranslation <- inTranslationForUser.filter(_.newTranslationId.isDefined)
        newTranslation <- translationRepository.withId(inTranslation.newTranslationId.get)
        availableLanguages = translationRepository.languagesFor(newTranslation.bookId)
        book <- withIdAndLanguage(newTranslation.bookId, inTranslation.fromLanguage)
      } yield converterService.toApiMyBook(inTranslation, newTranslation, availableLanguages, book)
    }

    def withIdAndLanguage(bookId: Long, language: LanguageTag): Option[api.Book] = {
      for {
        translation <- translationRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toApiBook(translation, translationRepository.languagesFor(bookId), book)
    }

    def withIdAndLanguageForExport(bookId: Long, language: LanguageTag): Option[api.internal.Book] = {
      for {
        translation <- translationRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toInternalApiBook(translation, translationRepository.languagesFor(bookId), book)
    }

    def chaptersForIdAndLanguage(bookId: Long, language: LanguageTag): Seq[api.ChapterSummary] = {
      chapterRepository.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: LanguageTag, chapterId: Long): Option[api.Chapter] = {
      chapterRepository.chapterForBookWithLanguageAndId(bookId, language, chapterId).map(converterService.toApiChapter(_))
    }

    def chapterWithId(chapterId: Long): Option[api.Chapter] = {
      chapterRepository.withId(chapterId).map(converterService.toApiChapter(_))
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

    def uuidWithTranslationId(translationId: Option[Long]): Option[api.internal.UUID] = {
      translationId
        .flatMap(translationRepository.withId)
        .map(_.uuid)
        .map(api.internal.UUID)
    }

    def withExternalId(externalId: Option[String]): Option[api.Book] = {
      externalId.flatMap(translationRepository.withExternalId) match {
        case Some(translation) => bookRepository.withId(translation.bookId).map(book =>
          converterService.toApiBook(translation, translationRepository.languagesFor(translation.bookId), book))
        case None => None
      }
    }
  }
}
