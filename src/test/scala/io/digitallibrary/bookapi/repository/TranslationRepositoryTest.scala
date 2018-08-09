/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import java.time.LocalDate

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.domain.{PublishingStatus, Sort}
import io.digitallibrary.bookapi.{IntegrationSuite, TestEnvironment}


class TranslationRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val bookRepository = new BookRepository
  override val categoryRepository = new CategoryRepository
  override val unFlaggedTranslationsRepository = new TranslationRepository
  override val publisherRepository = new PublisherRepository

  test("that Translation is added") {
    withRollback { implicit session =>
      val language = "eng"
      val book = addBookDef()
      val addedTranslation = addTranslationDef("external-id", "Some title", book.id.get, LanguageTag(language))

      addedTranslation.id.isDefined should be(true)
      addedTranslation.revision.isDefined should be(true)

      val readTranslation = unFlaggedTranslationsRepository.forBookIdAndLanguage(book.id.get, LanguageTag(language))
      readTranslation.isDefined should be(true)
      readTranslation.head.id should equal(addedTranslation.id)
      readTranslation.head.title should equal("Some title")
      readTranslation.head.categories.length should be(1)
    }
  }

  test("that languagesFor returns all languages for the given book") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext2", "title 2", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext3", "title 3", book1.id.get, LanguageTag("amh"))
      addTranslationDef("ext4", "title 4", book1.id.get, LanguageTag("swa"))

      unFlaggedTranslationsRepository.languagesFor(book1.id.get).map(_.toString).sorted should equal(Seq("am", "en", "nb", "sw"))
    }
  }

  test("that allAvailableLanguagesWithStatus only returns languages where translations with given status exists") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "a", book1.id.get, LanguageTag("xho"), None, Some(LocalDate.now()), status = PublishingStatus.UNLISTED)
      addTranslationDef("ext2", "b", book2.id.get, LanguageTag("amh"), None, Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)
      addTranslationDef("ext3", "c", book3.id.get, LanguageTag("nob"), None, Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)

      val languages = unFlaggedTranslationsRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)

      languages.size should be (2)
      languages.contains(LanguageTag("xho")) should be (false)
      languages.contains(LanguageTag("amh")) should be (true)
      languages.contains(LanguageTag("nob")) should be (true)
    }
  }

  test("that allAvailableLevelsWithStatus only returns levels where translations with given status exists") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef(externalId = "ext1", title = "a", bookId = book1.id.get, language = LanguageTag("xho"), readingLevel = Some("1"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.UNLISTED)
      addTranslationDef(externalId = "ext2", title = "b", bookId = book2.id.get, language = LanguageTag("xho"), readingLevel = Some("2"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)
      addTranslationDef(externalId = "ext3", title = "c", bookId = book3.id.get, language = LanguageTag("xho"), readingLevel = Some("3"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)

      val levels = unFlaggedTranslationsRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, None, None)
      levels.size should be (2)
      levels.contains("1") should be (false)
      levels.contains("2") should be (true)
      levels.contains("3") should be (true)
    }
  }

  test("that allAvailableLevelsWithStatus only returns levels where translations with given status exists for given language") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef(externalId = "ext1", title = "a", bookId = book1.id.get, language = LanguageTag("xho"), readingLevel = Some("1"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.UNLISTED)
      addTranslationDef(externalId = "ext2", title = "b", bookId = book2.id.get, language = LanguageTag("nob"), readingLevel = Some("2"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)
      addTranslationDef(externalId = "ext3", title = "c", bookId = book3.id.get, language = LanguageTag("xho"), readingLevel = Some("3"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)

      val levels = unFlaggedTranslationsRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, Some(LanguageTag("xho")), None)
      levels.size should be (1)
      levels.contains("1") should be (false)
      levels.contains("2") should be (false)
      levels.contains("3") should be (true)
    }
  }

  test("that allAvailableCategoriesAndReadingLevelsWithStatus returns a list of tuples, containing category and reading level") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()
      val book5 = addBookDef()

      addTranslationDef(categoryName = Some("category1"), externalId = "ext1", title = "a", bookId = book1.id.get, language = LanguageTag("xho"), readingLevel = Some("level1"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.UNLISTED)
      addTranslationDef(categoryName = Some("category1"), externalId = "ext2", title = "b", bookId = book2.id.get, language = LanguageTag("xho"), readingLevel = Some("level2"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)
      addTranslationDef(categoryName = Some("category2"), externalId = "ext3", title = "c", bookId = book3.id.get, language = LanguageTag("xho"), readingLevel = Some("level3"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)
      addTranslationDef(categoryName = Some("category1"), externalId = "ext4", title = "d", bookId = book4.id.get, language = LanguageTag("xho"), readingLevel = Some("level3"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)
      addTranslationDef(categoryName = Some("category3"), externalId = "ext5", title = "e", bookId = book5.id.get, language = LanguageTag("eng"), readingLevel = Some("level1"), dateArrived = Some(LocalDate.now()), status = PublishingStatus.PUBLISHED)

      val result = unFlaggedTranslationsRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, LanguageTag("xho"))
      result.keySet.map(_.name) should equal(Set("category1", "category2"))
      result.map{case (category, readingLevels) => category.name -> readingLevels} should equal (Map(
        "category1" -> Set("level2", "level3"),
        "category2" -> Set("level3")))
    }
  }

  test("that translations are fetched correct") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()

      addTranslationDef("ext1", "title1", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext2", "title1", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext3", "title1", book1.id.get, LanguageTag("xho"))
      addTranslationDef("ext4", "title2", book2.id.get, LanguageTag("nob"))
      addTranslationDef("ext5", "title2", book2.id.get, LanguageTag("xho"))

      val engTranslations = unFlaggedTranslationsRepository.withLanguageAndStatus(Some(LanguageTag("eng")), PublishingStatus.PUBLISHED, 10, 1)
      val nobTranslations = unFlaggedTranslationsRepository.withLanguageAndStatus(Some(LanguageTag("nob")), PublishingStatus.PUBLISHED,10, 1)
      val xhoTranslationsWithLimiting = unFlaggedTranslationsRepository.withLanguageAndStatus(Some(LanguageTag("xho")), PublishingStatus.PUBLISHED, 1, 1)

      engTranslations.results.length should be(1)
      nobTranslations.results.length should be(2)
      xhoTranslationsWithLimiting.results.length should be(1)
    }
  }

  test("that number of translations is correct") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "title1", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext2", "title1", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext3", "title1", book1.id.get, LanguageTag("xho"))
      addTranslationDef("ext4", "title2", book2.id.get, LanguageTag("eng"))
      addTranslationDef("ext5", "title2", book2.id.get, LanguageTag("nob"))
      addTranslationDef("ext6", "title3", book3.id.get, LanguageTag("nob"))

      val nobBooks = unFlaggedTranslationsRepository.numberOfTranslationsWithStatus(LanguageTag("nob"), PublishingStatus.PUBLISHED)
      val engBooks = unFlaggedTranslationsRepository.numberOfTranslationsWithStatus(LanguageTag("eng"), PublishingStatus.PUBLISHED)
      val xhoBooks = unFlaggedTranslationsRepository.numberOfTranslationsWithStatus(LanguageTag("xho"), PublishingStatus.PUBLISHED)
      val sweBooks = unFlaggedTranslationsRepository.numberOfTranslationsWithStatus(LanguageTag("swe"), PublishingStatus.PUBLISHED)

      nobBooks should be(3)
      engBooks should be(2)
      xhoBooks should be(1)
      sweBooks should be(0)
    }
  }
}