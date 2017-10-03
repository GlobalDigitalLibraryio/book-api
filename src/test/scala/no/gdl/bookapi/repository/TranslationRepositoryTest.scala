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


class TranslationRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val bookRepository = new BookRepository
  override val categoryRepository = new CategoryRepository
  override val translationRepository = new TranslationRepository
  override val licenseRepository = new LicenseRepository
  override val publisherRepository = new PublisherRepository

  test("that Translation is added") {
    withRollback { implicit session =>
      val language = "eng"
      val book = addBookDef()
      val addedTranslation = addTranslationDef("external-id", "Some title", book.id.get, language)

      addedTranslation.id.isDefined should be(true)
      addedTranslation.revision.isDefined should be(true)

      val readTranslation = translationRepository.forBookIdAndLanguage(book.id.get, language)
      readTranslation.isDefined should be(true)
      readTranslation.head.id should equal(addedTranslation.id)
      readTranslation.head.title should equal("Some title")
      readTranslation.head.categories.length should be(1)
    }
  }


  test("that bookIdsWithLanguage only returns ids for translations in given language") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "Title 1 - eng", book1.id.get, "eng")
      addTranslationDef("ext2", "Title 1 - nob", book1.id.get, "nob")
      addTranslationDef("ext3", "Title 2 - eng", book2.id.get, "eng")
      addTranslationDef("ext4", "Title 2 - nob", book2.id.get, "nob")
      addTranslationDef("ext6", "Title 3 - nob", book3.id.get, "nob")
      addTranslationDef("ext8", "Title 4 - nob", book4.id.get, "nob")

      val searchResult = translationRepository.bookIdsWithLanguage("eng", 10, 1)
      searchResult.language should equal("eng")
      searchResult.results.length should be(2)
      searchResult.results.sorted should equal(Seq(book1.id.get, book2.id.get))
    }
  }

  test("that languagesFor returns all languages for the given book") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      addTranslationDef("ext1", "title 1", book1.id.get, "eng")
      addTranslationDef("ext1", "title 1", book1.id.get, "nob")
      addTranslationDef("ext1", "title 1", book1.id.get, "amh")
      addTranslationDef("ext1", "title 1", book1.id.get, "swa")

      translationRepository.languagesFor(book1.id.get).sorted should equal(Seq("amh", "eng", "nob", "swa"))
    }
  }

  test("that bookIdsWithLanguageAndLevel returns ids with same language when level is None") {
    withRollback { implicit  session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, "xho")
      addTranslationDef("ext2", "title 2", book2.id.get, "amh")
      addTranslationDef("ext3", "title 3", book3.id.get, "xho")
      addTranslationDef("ext4", "title 4", book4.id.get, "xho")

      val ids = translationRepository.bookIdsWithLanguageAndLevel("xho", None, 10, 1)

      ids.results should equal (Seq(book1.id.get, book3.id.get, book4.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevel only returns ids for same level") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, "xho", Some("2"))
      addTranslationDef("ext2", "title 2", book2.id.get, "xho", Some("2"))
      addTranslationDef("ext3", "title 3", book3.id.get, "xho", Some("1"))

      val ids = translationRepository.bookIdsWithLanguageAndLevel("xho", Some("2"), 10, 1)
      ids.results should equal (Seq(book1.id.get, book2.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevel returns correct page and pagesize") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, "xho", None)
      addTranslationDef("ext2", "title 2", book2.id.get, "xho", None)
      addTranslationDef("ext3", "title 3", book3.id.get, "xho", None)
      addTranslationDef("ext4", "title 4", book4.id.get, "xho", None)

      val page1 = translationRepository.bookIdsWithLanguageAndLevel("xho", None, 1, 1)
      page1.results should equal (Seq(book1.id.get))

      val page2 = translationRepository.bookIdsWithLanguageAndLevel("xho", None, 1, 2)
      page2.results should equal (Seq(book2.id.get))

      val page3 = translationRepository.bookIdsWithLanguageAndLevel("xho", None, 1, 3)
      page3.results should equal (Seq(book3.id.get))

      val doublePage = translationRepository.bookIdsWithLanguageAndLevel("xho", None, 2, 2)
      doublePage.results should equal(Seq(book3.id.get, book4.id.get))
    }
  }

  def addBookDef()(implicit session: DBSession = AutoSession): Book = {
    val publisher = publisherRepository.add(Publisher(None, None, "Publisher Name"))
    val license = licenseRepository.add(License(None, None, "License Name", None, None))

    bookRepository.add(Book(None, None, publisher.id.get, license.id.get, publisher, license))
  }

  def addTranslationDef(externalId: String, title: String, bookId: Long, language: String, readingLevel: Option[String] = None)(implicit session: DBSession = AutoSession): Translation = {
    val cat1 = categoryRepository.add(Category(None, None, "some-category"))

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
      categories = Seq(cat1))

    translationRepository.add(translationDef)
  }
}