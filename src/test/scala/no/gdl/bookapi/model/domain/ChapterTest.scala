/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class ChapterTest extends IntegrationSuite with TestEnvironment {

  test("that Chapter.add returns a Chapter with id") {
    withRollback { implicit session =>
      val book = TranslationTest.addBookDef()
      val translation = TranslationTest.addTranslationDef("external-id", "Some title", book.id.get, "eng")

      val chapter = Chapter(None, None, translation.id.get, 1, Some("Chaptertitle"), "Chaptercontent")

      val persisted = Chapter.add(chapter)
      persisted.id.isDefined should be(true)
      persisted.revision.isDefined should be(true)
    }
  }

  test("that Chapter.chaptersForBookIdAndLanguage returns all chapters for a translation") {
    withRollback { implicit session =>
      val book = TranslationTest.addBookDef()
      val translation = TranslationTest.addTranslationDef("external-id", "Some title", book.id.get, "eng")

      val chapter1 = Chapter.add(Chapter(None, None, translation.id.get, 1, Some("Chaptertitle1"), "Chaptercontent1"))
      val chapter2 = Chapter.add(Chapter(None, None, translation.id.get, 2, Some("Chaptertitle2"), "Chaptercontent2"))

      val chapters = Chapter.chaptersForBookIdAndLanguage(book.id.get, "eng")
      chapters.length should be(2)
      chapters.minBy(_.id).id should equal(chapter1.id)
      chapters.maxBy(_.id).id should equal(chapter2.id)
    }
  }

  test("that Chapter.chapterForBookWithLanguageAndId returns chapter with given id") {
    withRollback { implicit session =>
      val book = TranslationTest.addBookDef()
      val translation = TranslationTest.addTranslationDef("external-id", "Some title", book.id.get, "eng")

      val chapter1 = Chapter.add(Chapter(None, None, translation.id.get, 1, Some("Chaptertitle1"), "Chaptercontent1"))

      val chapter = Chapter.chapterForBookWithLanguageAndId(book.id.get, "eng", chapter1.id.get)
      chapter.isDefined should be(true)
      chapter.head.id should equal(chapter1.id)
      chapter.head.content should equal(chapter1.content)
    }
  }

}
