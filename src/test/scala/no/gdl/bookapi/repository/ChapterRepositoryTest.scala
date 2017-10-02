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


class ChapterRepositoryTest extends IntegrationSuite with TestEnvironment {

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
