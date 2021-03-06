/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.{IntegrationSuite, TestEnvironment}


class ChapterRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val bookRepository = new BookRepository
  override val categoryRepository = new CategoryRepository
  override val chapterRepository = new ChapterRepository
  override val unFlaggedTranslationsRepository = new TranslationRepository
  override val publisherRepository = new PublisherRepository

  test("that Chapter.add returns a Chapter with id") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, LanguageTag("eng"))

      val chapter = Chapter(None, None, translation.id.get, 1, Some("Chaptertitle"), "Chaptercontent", ChapterType.Content)

      val persisted = chapterRepository.add(chapter)
      persisted.id.isDefined should be(true)
      persisted.revision.isDefined should be(true)
    }
  }

  test("that Chapter.chaptersForBookIdAndLanguage returns all chapters for a translation") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, LanguageTag("eng"))

      val chapter1 = chapterRepository.add(Chapter(None, None, translation.id.get, 1, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))
      val chapter2 = chapterRepository.add(Chapter(None, None, translation.id.get, 2, Some("Chaptertitle2"), "Chaptercontent2", ChapterType.Content))

      val chapters = chapterRepository.chaptersForBookIdAndLanguage(book.id.get, LanguageTag("eng"))
      chapters.length should be(2)
      chapters.minBy(_.id).id should equal(chapter1.id)
      chapters.maxBy(_.id).id should equal(chapter2.id)
    }
  }

  test("that Chapter.chapterForBookWithLanguageAndId returns chapter with given id") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, LanguageTag("eng"))

      val chapter1 = chapterRepository.add(Chapter(None, None, translation.id.get, 1, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))

      val chapter = chapterRepository.chapterForBookWithLanguageAndId(book.id.get, LanguageTag("eng"), chapter1.id.get)
      chapter.isDefined should be(true)
      chapter.head.id should equal(chapter1.id)
      chapter.head.content should equal(chapter1.content)
    }
  }

  test("that Chapter.deleteChaptersExceptGivenSeqNumbers deletes chapters with seqNo not in the provided list, and no more") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation1 = addTranslationDef("external-id1", "Some title 1", book.id.get, LanguageTag("eng"))
      val translation2 = addTranslationDef("external-id2", "Some title 2", book.id.get, LanguageTag("swa"))

      chapterRepository.add(Chapter(None, None, translation1.id.get, 1, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))
      chapterRepository.add(Chapter(None, None, translation1.id.get, 2, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))
      chapterRepository.add(Chapter(None, None, translation1.id.get, 3, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))
      chapterRepository.add(Chapter(None, None, translation1.id.get, 4, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))
      chapterRepository.add(Chapter(None, None, translation1.id.get, 5, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))

      chapterRepository.add(Chapter(None, None, translation2.id.get, 4, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))
      chapterRepository.add(Chapter(None, None, translation2.id.get, 5, Some("Chaptertitle1"), "Chaptercontent1", ChapterType.Content))

      chapterRepository.chaptersForBookIdAndLanguage(book.id.get, LanguageTag("eng")).size should equal(5)
      chapterRepository.chaptersForBookIdAndLanguage(book.id.get, LanguageTag("swa")).size should equal(2)

      chapterRepository.deleteChaptersExceptGivenSeqNumbers(translation1.id.get, Seq(1, 2, 3))

      chapterRepository.chaptersForBookIdAndLanguage(book.id.get, LanguageTag("eng")).size should equal(3)
      chapterRepository.chaptersForBookIdAndLanguage(book.id.get, LanguageTag("swa")).size should equal(2)
    }
  }
}
