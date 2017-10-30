/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.time.LocalDate

import no.gdl.bookapi.model.domain._
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class EditorsPickRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val editorsPickRepository = new EditorsPickRepository
  override val bookRepository = new BookRepository
  override val publisherRepository = new PublisherRepository
  override val licenseRepository = new LicenseRepository
  override val categoryRepository = new CategoryRepository
  override val translationRepository = new TranslationRepository

  test("that forLanguage returns None when no defined editor picks for any languages") {
    val editorsPick = editorsPickRepository.forLanguage("eng")
    editorsPick.isDefined should be (false)
  }

  test("that forLanguage returns None when no defined editor picks for given language") {
    withRollback {implicit session =>
      val book1 = addBookDef()
      val translation1 = addTranslationDef("ext1", "Some title 1", book1.id.get, "eng")
      editorsPickRepository.add(EditorsPick(None, None, "eng", Seq(translation1.id.get), LocalDate.now()))

      val editorsPick = editorsPickRepository.forLanguage("nob")
      editorsPick.isDefined should be (false)
    }
  }

  test("that forLanguage returns expected ids when language exists") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val translation1 = addTranslationDef("ext1", "Some title 1", book1.id.get, "eng")

      val book2 = addBookDef()
      val translation2 = addTranslationDef("ext2", "Some title 2", book2.id.get, "eng")
      addTranslationDef("ext2.1", "En alternativ tittel for book2", book2.id.get, "nob")

      val book3 = addBookDef()
      addTranslationDef("ext3", "En tittel på norsk bokmål", book3.id.get, "nob")

      editorsPickRepository.add(EditorsPick(None, None, "eng", Seq(translation1.id.get, translation2.id.get), LocalDate.now()))

      val editorsPick = editorsPickRepository.forLanguage("eng")
      editorsPick.isDefined should be (true)
      editorsPick.get.translationIds should equal (Seq(translation1.id.get, translation2.id.get))
    }
  }
}
