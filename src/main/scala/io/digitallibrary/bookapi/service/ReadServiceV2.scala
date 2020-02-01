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
import io.digitallibrary.bookapi.model.api.{ApiVersion, MyBook, MyBookV2}
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.repository._
import io.digitallibrary.network.AuthUser

trait ReadServiceV2 {
  this: ConverterService with BookRepository with ChapterRepository with TranslationRepository with InTranslationRepository
    with FeaturedContentRepository with CategoryRepository with SourceRepository =>
  val readServiceV2: ReadServiceV2

  class ReadServiceV2 {

    def withLanguageAndStatus(languageTag: Option[LanguageTag], status: PublishingStatus.Value, pageSize: Int, page: Int, sort: Sort.Value): api.SearchResultV2 = {
      val searchResult = getTranslationRepository.withLanguageAndStatus(languageTag, status, pageSize, page, sort = Some(sort))
      val books = searchResult.results.flatMap(translation =>
        converterService.toApiBookHitV2(Some(translation), bookRepository.withId(translation.bookId)))

      api.SearchResultV2(searchResult.totalCount, searchResult.page, searchResult.pageSize, Some(converterService.toApiLanguage(searchResult.language)), books)
    }

    def listAvailablePublishedCategoriesForLanguage(language: LanguageTag): Map[Category, Set[String]] = {
      unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, language)
    }

    def featuredContentForLanguage(tag: LanguageTag): Seq[api.FeaturedContentV2] = {
      featuredContentRepository.forLanguage(tag).map(fc => {
        api.FeaturedContentV2(
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

    def myBookOrdering(sort: MyBooksSort.Value): Ordering[MyBookV2] = sort match {
      case (MyBooksSort.ByIdAsc) => Ordering.by[api.MyBookV2, Long](_.id)
      case (MyBooksSort.ByIdDesc) => Ordering.by[api.MyBookV2, Long](_.id).reverse
      case (MyBooksSort.ByTitleAsc) => Ordering.by[api.MyBookV2, String](_.title)
      case (MyBooksSort.ByTitleDesc) => Ordering.by[api.MyBookV2, String](_.title).reverse
    }

    def forUserWithLanguage(userId: String, sort: MyBooksSort.Value): Seq[api.MyBookV2] = {
      val inTranslationForUser = inTranslationRepository.inTranslationForUser(userId)
      val myBooks = for {
        inTranslation <- inTranslationForUser.filter(_.newTranslationId.isDefined)
        newTranslation <- unFlaggedTranslationsRepository.withId(inTranslation.newTranslationId.get)
        availableLanguages = unFlaggedTranslationsRepository.languagesFor(newTranslation.bookId)
        book <- withIdAndLanguage(newTranslation.bookId, inTranslation.fromLanguage)
      } yield converterService.toApiMyBookV2(inTranslation, newTranslation, availableLanguages, book)
      implicit val ordering: Ordering[MyBookV2] = myBookOrdering(sort)
      myBooks.sorted
    }

    def translationsWithTranslationStatus(status: TranslationStatus.Value, paging: Paging, sort: Sort.Value): api.SearchResultV2 = {
      val searchResult = getTranslationRepository.withTranslationStatus(status, paging.pageSize, paging.page, Some(sort))
      val books = searchResult.results.flatMap(translation =>
        converterService.toApiBookHitV2(Some(translation), bookRepository.withId(translation.bookId)))

      api.SearchResultV2(searchResult.totalCount, searchResult.page, searchResult.pageSize, Some(converterService.toApiLanguage(searchResult.language)), books)
    }

    def translationWithIdAndLanguageListingAllTranslationsIfAdmin(bookId: Long, language: LanguageTag): Option[Translation] =
      getTranslationRepository.forBookIdAndLanguage(bookId, language)

    def withIdAndLanguage(bookId: Long, language: LanguageTag): Option[api.BookV2] = {
      for {
        translation <- getTranslationRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toApiBookV2(translation, unFlaggedTranslationsRepository.languagesFor(bookId), book)
    }

    def withIdLanguageAndFromLanguage(bookId: Long, language: LanguageTag, fromLanguage: LanguageTag): Option[api.BookV2] = {
      for {
        translation <- getTranslationRepository.forBookIdLanguageAndFromLanguage(bookId, language, fromLanguage)
        book <- bookRepository.withId(bookId)
      } yield converterService.toApiBookV2(translation, unFlaggedTranslationsRepository.languagesFor(bookId), book)
    }

    def withIdAndLanguageListingAllBooksIfAdmin(bookId: Long, language: LanguageTag): Option[api.BookV2] = {
      for {
        translation <- getTranslationRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toApiBookV2(translation, unFlaggedTranslationsRepository.languagesFor(bookId), book)
    }

    def withIdAndLanguageForExport(bookId: Long, language: LanguageTag): Option[api.internal.BookV2] = {
      for {
        translation <- unFlaggedTranslationsRepository.forBookIdAndLanguage(bookId, language)
        book <- bookRepository.withId(bookId)
      } yield converterService.toInternalApiBookV2(translation, unFlaggedTranslationsRepository.languagesFor(bookId), book)
    }

    def chaptersForIdAndLanguage(bookId: Long, language: LanguageTag): Seq[api.ChapterSummary] = {
      chapterRepository.chaptersForBookIdAndLanguage(bookId, language).map(c => converterService.toApiChapterSummary(c, bookId, language))
    }

    def domainChapterForBookWithLanguageAndId(bookId: Long, language: LanguageTag, chapterId: Long): Option[domain.Chapter] = {
      chapterRepository.chapterForBookWithLanguageAndId(bookId, language, chapterId)
    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: LanguageTag, chapterId: Long, convertApi: Boolean = false): Option[api.ChapterV2] = {
      chapterRepository.chapterForBookWithLanguageAndId(bookId, language, chapterId).map(converterService.toApiChapterV2(_, convertApi))
    }

    def chapterWithId(chapterId: Long): Option[api.ChapterV2] = {
      chapterRepository.withId(chapterId).map(converterService.toApiChapterV2(_))
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

    def withExternalId(externalId: Option[String]): Option[api.BookV2] = {
      externalId.flatMap(unFlaggedTranslationsRepository.withExternalId) match {
        case Some(translation) => bookRepository.withId(translation.bookId).map(book =>
          converterService.toApiBookV2(translation, unFlaggedTranslationsRepository.languagesFor(translation.bookId), book))
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
