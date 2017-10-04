/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain._
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class ChapterRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val bookRepository = new BookRepository
  override val categoryRepository = new CategoryRepository
  override val chapterRepository = new ChapterRepository
  override val translationRepository = new TranslationRepository
  override val licenseRepository = new LicenseRepository
  override val publisherRepository = new PublisherRepository

  test("that Chapter.add returns a Chapter with id") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, "eng")

      val chapter = Chapter(None, None, translation.id.get, 1, Some("Chaptertitle"), "Chaptercontent")

      val persisted = chapterRepository.add(chapter)
      persisted.id.isDefined should be(true)
      persisted.revision.isDefined should be(true)
    }
  }

  test("that Chapter.chaptersForBookIdAndLanguage returns all chapters for a translation") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, "eng")

      val chapter1 = chapterRepository.add(Chapter(None, None, translation.id.get, 1, Some("Chaptertitle1"), "Chaptercontent1"))
      val chapter2 = chapterRepository.add(Chapter(None, None, translation.id.get, 2, Some("Chaptertitle2"), "Chaptercontent2"))

      val chapters = chapterRepository.chaptersForBookIdAndLanguage(book.id.get, "eng")
      chapters.length should be(2)
      chapters.minBy(_.id).id should equal(chapter1.id)
      chapters.maxBy(_.id).id should equal(chapter2.id)
    }
  }

  test("that Chapter.chapterForBookWithLanguageAndId returns chapter with given id") {
    withRollback { implicit session =>
      val book = addBookDef()
      val translation = addTranslationDef("external-id", "Some title", book.id.get, "eng")

      val chapter1 = chapterRepository.add(Chapter(None, None, translation.id.get, 1, Some("Chaptertitle1"), "Chaptercontent1"))

      val chapter = chapterRepository.chapterForBookWithLanguageAndId(book.id.get, "eng", chapter1.id.get)
      chapter.isDefined should be(true)
      chapter.head.id should equal(chapter1.id)
      chapter.head.content should equal(chapter1.content)
    }
  }
}