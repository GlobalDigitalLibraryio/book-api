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

      val translationForBook1Eng = addTranslationDef("ext1", "Title 1 - eng", book1.id.get, "eng")
      val translationForBook1Nob = addTranslationDef("ext2", "Title 1 - nob", book1.id.get, "nob")
      val translationForBook2Eng = addTranslationDef("ext3", "Title 2 - eng", book2.id.get, "eng")
      val translationForBook2Nob = addTranslationDef("ext4", "Title 2 - nob", book2.id.get, "nob")
      val translationForBook3Nob = addTranslationDef("ext6", "Title 3 - nob", book3.id.get, "nob")
      val translationForBook4Nob = addTranslationDef("ext8", "Title 4 - nob", book4.id.get, "nob")


      val searchResult = translationRepository.bookIdsWithLanguage("eng", 10, 1)
      searchResult.language should equal("eng")
      searchResult.results.length should be(2)
      searchResult.results.sorted should equal(Seq(book1.id.get, book2.id.get))
    }
  }

  test("that languagesFor returns all languages for the given book") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val eng = addTranslationDef("ext1", "title 1", book1.id.get, "eng")
      val nob = addTranslationDef("ext1", "title 1", book1.id.get, "nob")
      val amh = addTranslationDef("ext1", "title 1", book1.id.get, "amh")
      val swa = addTranslationDef("ext1", "title 1", book1.id.get, "swa")

      translationRepository.languagesFor(book1.id.get).sorted should equal(Seq("amh", "eng", "nob", "swa"))
    }
  }

  def addBookDef()(implicit session: DBSession = AutoSession): Book = {
    val publisher = publisherRepository.add(Publisher(None, None, "Publisher Name"))
    val license = licenseRepository.add(License(None, None, "License Name", None, None))

    bookRepository.add(Book(None, None, publisher.id.get, license.id.get, publisher, license))
  }

  def addTranslationDef(externalId: String, title: String, bookId: Long, language: String)(implicit session: DBSession = AutoSession): Translation = {
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
      readingLevel = None,
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

    translationRepository.add(translationDef)
  }
}