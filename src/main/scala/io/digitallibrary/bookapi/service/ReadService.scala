/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service


import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.MyBook
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.network.AuthUser

trait ReadService {
  this: ConverterService with BookRepository with ChapterRepository with TranslationRepository with InTranslationRepository
  with FeaturedContentRepository with CategoryRepository with SourceRepository =>
  val readService: ReadService

  class ReadService {

    def withLanguageAndStatus(languageTag: Option[LanguageTag], status: PublishingStatus.Value, pageSize: Int, page: Int, sort: Sort.Value): api.SearchResult = {
      val searchResult = getTranslationRepository.withLanguageAndStatus(languageTag, status, pageSize, page, sort = Some(sort))
      val books = searchResult.results.flatMap(translation =>
          converterService.toApiBookHit(Some(translation), bookRepository.withId(translation.bookId)))

      api.SearchResult(searchResult.totalCount, searchResult.page, searchResult.pageSize, Some(converterService.toApiLanguage(searchResult.language)), books)
    }

    def listAvailablePublishedCategoriesForLanguage(language: LanguageTag): Map[Category, Set[String]] = {
      unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
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
          fc.imageUrl,
          fc.categoryId.flatMap(categoryRepository.withId).map(converterService.toApiCategory)
        )
      })
    }

    def listAvailablePublishedLanguages: Seq[api.Language] = {
      unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED).map(converterService.toApiLanguage).sortBy(_.name)
    }

    def listAvailablePublishedLanguagesAsLanguageTags: Seq[LanguageTag] = {
      unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)
    }

    def listAvailablePublishedLevelsForLanguage(lang: Option[LanguageTag] = None, category: Option[String] = None): Seq[String] = {
      val cat = category.flatMap(categoryRepository.withName)
      unFlaggedTranslationsRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, lang, cat)
    }

    def myBookOrdering(sort: MyBooksSort.Value): Ordering[MyBook] = sort match {
      case (MyBooksSort.ByIdAsc) => Ordering.by[api.MyBook, Long](_.id)
      case (MyBooksSort.ByIdDesc) => Ordering.by[api.MyBook, Long](_.id).reverse
      case (MyBooksSort.ByTitleAsc) => Ordering.by[api.MyBook, String](_.title)
      case (MyBooksSort.ByTitleDesc) => Ordering.by[api.MyBook, String](_.title).reverse
    }

    def forUserWithLanguage(userId: String, sort: MyBooksSort.Value): Seq[api.MyBook] = {
      val inTranslationForUser = inTranslationRepository.inTranslationForUser(userId)
      val myBooks = for {
        inTranslation <- inTranslationForUser.filter(_.newTranslationId.isDefined)
        newTranslation <- unFlaggedTranslationsRepository.withId(inTranslation.newTranslationId.get)
        availableLanguages = unFlaggedTranslationsRepository.languagesFor(newTranslation.bookId)
        book <- withIdAndLanguage(newTranslation.bookId, inTranslation.fromLanguage)
      } yield converterService.toApiMyBook(inTranslation, newTranslation, availableLanguages, book)
      implicit val ordering: Ordering[MyBook] = myBookOrdering(sort)
      myBooks.sorted
    }

    def translationWithIdAndLanguageListingAllTranslationsIfAdmin(bookId: Long, language: LanguageTag): Option[Translation] =
      getTranslationRepository.forBookIdAndLanguage(bookId, language)

    def withIdAndLanguage(bookId: Long, language: LanguageTag): Option[api.Book] = {
      for {
        translation <- unFlaggedTranslationsRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toApiBook(translation, unFlaggedTranslationsRepository.languagesFor(bookId), book)
    }

    def withIdAndLanguageListingAllBooksIfAdmin(bookId: Long, language: LanguageTag): Option[api.Book] = {
      for {
        translation <- getTranslationRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toApiBook(translation, unFlaggedTranslationsRepository.languagesFor(bookId), book)
    }

    def withIdAndLanguageForExport(bookId: Long, language: LanguageTag): Option[api.internal.Book] = {
      for {
        translation <- unFlaggedTranslationsRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toInternalApiBook(translation, unFlaggedTranslationsRepository.languagesFor(bookId), book)
    }

    def chaptersForIdAndLanguage(bookId: Long, language: LanguageTag): Seq[api.ChapterSummary] = {
      chapterRepository.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def domainChapterForBookWithLanguageAndId(bookId: Long, language: LanguageTag, chapterId: Long): Option[domain.Chapter] = {
      chapterRepository.chapterForBookWithLanguageAndId(bookId, language, chapterId)
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
        .flatMap(unFlaggedTranslationsRepository.withExternalId)
        .flatMap(_.id)
        .map(api.internal.TranslationId)
    }

    def uuidWithTranslationId(translationId: Option[Long]): Option[api.internal.UUID] = {
      translationId
        .flatMap(unFlaggedTranslationsRepository.withId)
        .map(_.uuid)
        .map(api.internal.UUID)
    }

    def withExternalId(externalId: Option[String]): Option[api.Book] = {
      externalId.flatMap(unFlaggedTranslationsRepository.withExternalId) match {
        case Some(translation) => bookRepository.withId(translation.bookId).map(book =>
          converterService.toApiBook(translation, unFlaggedTranslationsRepository.languagesFor(translation.bookId), book))
        case None => None
      }
    }

    private def getTranslationRepository: TranslationRepository = {
      if(AuthUser.hasRole(BookApiProperties.RoleWithWriteAccess)) {
        allTranslationsRepository
      } else {
        unFlaggedTranslationsRepository
      }
    }

    def listSourcesForLanguage(language: LanguageTag): Seq[api.Source] = {
      sourceRepository.getSources(language).map(source => api.Source(source.source, source.count))
    }

  }
}
