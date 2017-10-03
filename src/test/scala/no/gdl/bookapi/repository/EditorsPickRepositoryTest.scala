/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.time.LocalDate
import java.util.UUID

import no.gdl.bookapi.model.domain._
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}
import scalikejdbc.{AutoSession, DBSession}


class EditorsPickRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val editorsPickRepository = new EditorsPickRepository

  test("that forLanguage returns None when no defined editor picks for any languages") {
    val editorsPick = editorsPickRepository.forLanguage("eng")
    editorsPick.isDefined should be (false)
  }

  test("that forLanguage returns None when no defined editor picks for given language") {
    withRollback {implicit session =>
      val book1 = addBookDef()
      val translation1 = addTranslationDef("ext1", "Some title 1", book1.id.get, "eng")
      editorsPickRepository.add(EditorsPick(None, None, "eng", Seq(translation1.id.get)))

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

      editorsPickRepository.add(EditorsPick(None, None, "eng", Seq(translation1.id.get, translation2.id.get)))

      val editorsPick = editorsPickRepository.forLanguage("eng")
      editorsPick.isDefined should be (true)
      editorsPick.get.translationIds should equal (Seq(translation1.id.get, translation2.id.get))
    }
  }

  def addBookDef(implicit session: DBSession = AutoSession): Book = {
    val publisher = Publisher.add(Publisher(None, None, "Publisher Name"))
    val license = License.add(License(None, None, "License Name", None, None))

    Book.add(Book(None, None, publisher.id.get, license.id.get, publisher, license)).get
  }

  def addTranslationDef(externalId: String, title: String, bookId: Long, language: String, readingLevel: Option[String] = None)(implicit session: DBSession = AutoSession): Translation = {
    val cat1 = Category.add(Category(None, None, "some-category"))

    val translationDef = Translation(
      id = None,
      revision = None,
      bookId = bookId,
      externalId = Some(externalId),
      uuid = UUID.randomUUID().toString,
      title = title,
      about = "Some description",
      numPages = Some(123),
      language = language,
      datePublished = Some(LocalDate.now()),
      dateCreated = Some(LocalDate.now()),
      categoryIds = Seq(cat1.id.get),
      coverphoto = None,
      tags = Seq("tag1", "tag2"),
      isBasedOnUrl = None,
      educationalUse = None,
      educationalRole = None,
      eaId = None,
      timeRequired = None,
      typicalAgeRange = None,
      readingLevel = readingLevel,
      interactivityType = None,
      learningResourceType = None,
      accessibilityApi = None,
      accessibilityControl = None,
      accessibilityFeature = None,
      accessibilityHazard = None,
      educationalAlignment = None,
      chapters = Seq(),
      contributors = Seq(),
      categories = Seq(cat1)
    )

    Translation.add(translationDef)
  }
}
