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
  override val translationRepository = new TranslationRepository
  override val licenseRepository = new LicenseRepository
  override val publisherRepository = new PublisherRepository

  test("that Translation is added") {
    withRollback { implicit session =>
      val language = "eng"
      val book = addBookDef()
      val addedTranslation = addTranslationDef("external-id", "Some title", book.id.get, LanguageTag(language))

      addedTranslation.id.isDefined should be(true)
      addedTranslation.revision.isDefined should be(true)

      val readTranslation = translationRepository.forBookIdAndLanguage(book.id.get, LanguageTag(language))
      readTranslation.isDefined should be(true)
      readTranslation.head.id should equal(addedTranslation.id)
      readTranslation.head.title should equal("Some title")
      readTranslation.head.categories.length should be(1)
    }
  }

  test("that bookIdsWithLanguageAndStatus only returns ids for translations in given language") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "Title 1 - eng", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext2", "Title 1 - nob", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext3", "Title 2 - eng", book2.id.get, LanguageTag("eng"))
      addTranslationDef("ext4", "Title 2 - nob", book2.id.get, LanguageTag("nob"))
      addTranslationDef("ext6", "Title 3 - nob", book3.id.get, LanguageTag("nob"))
      addTranslationDef("ext8", "Title 4 - nob", book4.id.get, LanguageTag("nob"))

      val searchResult = translationRepository.bookIdsWithLanguageAndStatus(LanguageTag("eng"), PublishingStatus.PUBLISHED, 10, 1, Sort.ByIdAsc)
      searchResult.language.toString should equal("en")
      searchResult.results.length should be(2)
      searchResult.results.sorted should equal(Seq(book1.id.get, book2.id.get))
    }
  }

  test("that bookIdsWithLanguageAndStatus only returns ids for translations with given status") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "Title 1 - eng", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext2", "Title 1 - nob", book2.id.get, LanguageTag("eng"))
      addTranslationDef("ext3", "Title 2 - eng", book3.id.get, LanguageTag("eng"), status = PublishingStatus.UNLISTED)

      val searchResult = translationRepository.bookIdsWithLanguageAndStatus(LanguageTag("eng"), PublishingStatus.PUBLISHED, 10, 1, Sort.ByIdAsc)
      searchResult.language.toString should equal("en")
      searchResult.results.length should be(2)
      searchResult.results.sorted should equal(Seq(book1.id.get, book2.id.get))
    }
  }

  test("that bookIdsWithLanguage gives correct totalCount when pageSize is less than number of books in database") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "Title 1 - eng", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext2", "Title 1 - nob", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext3", "Title 2 - eng", book2.id.get, LanguageTag("eng"))
      addTranslationDef("ext4", "Title 2 - nob", book2.id.get, LanguageTag("nob"))
      addTranslationDef("ext6", "Title 3 - nob", book3.id.get, LanguageTag("nob"))
      addTranslationDef("ext8", "Title 4 - nob", book4.id.get, LanguageTag("nob"))

      val searchResult = translationRepository.bookIdsWithLanguageAndStatus(LanguageTag("nob"), PublishingStatus.PUBLISHED, 3, 1, Sort.ByIdAsc)
      searchResult.totalCount should equal(4)
    }
  }

  test("that languagesFor returns all languages for the given book") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("amh"))
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("swa"))

      translationRepository.languagesFor(book1.id.get).map(_.toString).sorted should equal(Seq("am", "en", "nb", "sw"))
    }
  }

  test("that bookIdsWithLanguageAndLevelAndStatus returns ids with same language when level is None") {
    withRollback { implicit  session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"))
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("amh"))
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"))
      addTranslationDef("ext4", "title 4", book4.id.get, LanguageTag("xho"))

      val ids = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 10, 1, Sort.ByIdAsc)

      ids.results should equal (Seq(book1.id.get, book3.id.get, book4.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevelAndStatus only returns ids for same level") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"), Some("2"))
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("xho"), Some("2"))
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"), Some("1"))

      val ids = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), Some("2"), PublishingStatus.PUBLISHED, 10, 1, Sort.ByIdAsc)
      ids.results should equal (Seq(book1.id.get, book2.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevel gives correct totalCount when pageSize is less than number of books in database") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"), Some("2"))
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("xho"), Some("2"))
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"), Some("1"))
      addTranslationDef("ext4", "title 4", book4.id.get, LanguageTag("xho"), Some("2"))

      val ids = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), Some("2"), PublishingStatus.PUBLISHED, 1, 1, Sort.ByIdAsc)
      ids.totalCount should equal (3)
    }
  }

  test("that bookIdsWithLanguageAndLevelAndStatus only returns ids with given status") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"), Some("2"), status = PublishingStatus.UNLISTED)
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("xho"), Some("2"), status = PublishingStatus.UNLISTED)
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"), Some("1"))

      val ids = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), Some("2"), PublishingStatus.UNLISTED, 10, 1, Sort.ByIdAsc)
      ids.results should equal (Seq(book1.id.get, book2.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevelAndStatus returns correct page and pagesize") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"), None)
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("xho"), None)
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"), None)
      addTranslationDef("ext4", "title 4", book4.id.get, LanguageTag("xho"), None)

      val page1 = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 1, 1, Sort.ByIdAsc)
      page1.results should equal (Seq(book1.id.get))

      val page2 = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 1, 2, Sort.ByIdAsc)
      page2.results should equal (Seq(book2.id.get))

      val page3 = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 1, 3, Sort.ByIdAsc)
      page3.results should equal (Seq(book3.id.get))

      val doublePage = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 2, 2, Sort.ByIdAsc)
      doublePage.results should equal(Seq(book3.id.get, book4.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevelAndStatus returns correctly sorted") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "b", book1.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(1)))
      addTranslationDef("ext2", "a", book2.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(3)))
      addTranslationDef("ext3", "d", book3.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(2)))
      addTranslationDef("ext4", "c", book4.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(4)))

      val byIdAsc = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 10, 1, Sort.ByIdAsc)
      byIdAsc.results should equal (Seq(book1.id.get, book2.id.get, book3.id.get, book4.id.get))

      val byIdDesc = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 10, 1, Sort.ByIdDesc)
      byIdDesc.results should equal (Seq(book4.id.get, book3.id.get, book2.id.get, book1.id.get))

      val byTitleAsc = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 10, 1, Sort.ByTitleAsc)
      byTitleAsc.results should equal (Seq(book2.id.get, book1.id.get, book4.id.get, book3.id.get))

      val byTitleDesc = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 10, 1, Sort.ByTitleDesc)
      byTitleDesc.results should equal (Seq(book3.id.get, book4.id.get, book1.id.get, book2.id.get))

      val byArrivalDateAsc = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 10, 1, Sort.ByArrivalDateAsc)
      byArrivalDateAsc.results should equal (Seq(book4.id.get, book2.id.get, book3.id.get, book1.id.get))

      val byArrivalDateDesc = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 10, 1, Sort.ByArrivalDateDesc)
      byArrivalDateDesc.results should equal (Seq(book1.id.get, book3.id.get, book2.id.get, book4.id.get))
    }
  }

  test("that ByArrivalDate also sorts on id on same date") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()
      val book5 = addBookDef()
      val book6 = addBookDef()

      val firstDate = LocalDate.now().minusDays(3)
      val secondDate = LocalDate.now().minusDays(2)
      val thirdDate = LocalDate.now().minusDays(1)

      addTranslationDef("ext1", "b", book1.id.get, LanguageTag("xho"), None, Some(firstDate))
      addTranslationDef("ext2", "a", book2.id.get, LanguageTag("xho"), None, Some(secondDate))
      addTranslationDef("ext3", "d", book3.id.get, LanguageTag("xho"), None, Some(secondDate))
      addTranslationDef("ext4", "c", book4.id.get, LanguageTag("xho"), None, Some(secondDate))
      addTranslationDef("ext5", "d", book5.id.get, LanguageTag("xho"), None, Some(secondDate))
      addTranslationDef("ext6", "c", book6.id.get, LanguageTag("xho"), None, Some(thirdDate))

      val page1 = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 3, 1, Sort.ByArrivalDateDesc)
      val page2 = translationRepository.bookIdsWithLanguageAndLevelAndStatus(LanguageTag("xho"), None, PublishingStatus.PUBLISHED, 3, 2, Sort.ByArrivalDateDesc)

      page1.results should equal (Seq(book6.id.get, book5.id.get, book4.id.get))
      page2.results should equal (Seq(book3.id.get, book2.id.get, book1.id.get))
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

      val languages = translationRepository.allAvailableLanguagesWithStatus(PublishingStatus.PUBLISHED)

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

      val levels = translationRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, None)
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

      val levels = translationRepository.allAvailableLevelsWithStatus(PublishingStatus.PUBLISHED, Some(LanguageTag("xho")))
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

      val result = translationRepository.allAvailableCategoriesAndReadingLevelsWithStatus(PublishingStatus.PUBLISHED, LanguageTag("xho"))
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
      addTranslationDef("ext1", "title1", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext1", "title1", book1.id.get, LanguageTag("xho"))
      addTranslationDef("ext2", "title2", book2.id.get, LanguageTag("nob"))
      addTranslationDef("ext2", "title2", book2.id.get, LanguageTag("xho"))

      val engTranslations = translationRepository.translationsWithLanguageAndStatus(LanguageTag("eng"), PublishingStatus.PUBLISHED, 10, 0)
      val nobTranslations = translationRepository.translationsWithLanguageAndStatus(LanguageTag("nob"), PublishingStatus.PUBLISHED,10, 0)
      val xhoTranslationsWithLimiting = translationRepository.translationsWithLanguageAndStatus(LanguageTag("xho"), PublishingStatus.PUBLISHED, 1, 0)

      engTranslations.length should be(1)
      nobTranslations.length should be(2)
      xhoTranslationsWithLimiting.length should be(1)
    }
  }

  test("that number of translations is correct") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "title1", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext1", "title1", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext1", "title1", book1.id.get, LanguageTag("xho"))
      addTranslationDef("ext2", "title2", book2.id.get, LanguageTag("eng"))
      addTranslationDef("ext2", "title2", book2.id.get, LanguageTag("nob"))
      addTranslationDef("ext3", "title3", book3.id.get, LanguageTag("nob"))

      val nobBooks = translationRepository.numberOfTranslationsWithStatus(LanguageTag("nob"), PublishingStatus.PUBLISHED)
      val engBooks = translationRepository.numberOfTranslationsWithStatus(LanguageTag("eng"), PublishingStatus.PUBLISHED)
      val xhoBooks = translationRepository.numberOfTranslationsWithStatus(LanguageTag("xho"), PublishingStatus.PUBLISHED)
      val sweBooks = translationRepository.numberOfTranslationsWithStatus(LanguageTag("swe"), PublishingStatus.PUBLISHED)

      nobBooks should be(3)
      engBooks should be(2)
      xhoBooks should be(1)
      sweBooks should be(0)
    }
  }
}