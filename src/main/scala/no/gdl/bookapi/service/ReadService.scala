/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service


import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model._
import no.gdl.bookapi.model.domain.{PublishingStatus, Sort}
import no.gdl.bookapi.repository._

trait ReadService {
  this: ConverterService with BookRepository with ChapterRepository with TranslationRepository with InTranslationRepository
  with FeaturedContentRepository =>
  val readService: ReadService

  class ReadService {

    def listAvailablePublishedCategoriesForLanguage(language: LanguageTag): Map[String, Seq[String]] = {
      translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
        .groupBy(_._1).mapValues(_.map(_._2))
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

    def listAvailablePublishedLevelsForLanguage(lang: Option[LanguageTag] = None): Seq[String] =
      translationRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, lang)

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
      val translation = translationRepository.forBookIdAndLanguage(bookId, language)
      val availableLanguages: Seq[LanguageTag] = translationRepository.languagesFor(bookId)
      val book: Option[domain.Book] = bookRepository.withId(bookId)

      converterService.toApiBook(translation, availableLanguages, book)
    }

    def chaptersForIdAndLanguage(bookId: Long, language: LanguageTag): Seq[api.ChapterSummary] = {
      chapterRepository.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: LanguageTag, chapterId: Long): Option[api.Chapter] = {
      chapterRepository.chapterForBookWithLanguageAndId(bookId, language, chapterId).map(converterService.toApiChapter)
    }

    def chapterWithId(chapterId: Long): Option[api.Chapter] = {
      chapterRepository.withId(chapterId).map(converterService.toApiChapter)
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
        case Some(translation) => converterService.toApiBook(
          Option(translation),
          translationRepository.languagesFor(translation.bookId),
          bookRepository.withId(translation.bookId))

        case None => None
      }
    }
  }
}
