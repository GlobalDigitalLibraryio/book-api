/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.time.LocalDate

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.Sort
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


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

  test("that bookIdsWithLanguage only returns ids for translations in given language") {
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

      val searchResult = translationRepository.bookIdsWithLanguage(LanguageTag("eng"), 10, 1, Sort.ByIdAsc)
      searchResult.language.toString should equal("eng")
      searchResult.results.length should be(2)
      searchResult.results.sorted should equal(Seq(book1.id.get, book2.id.get))
    }
  }

  test("that languagesFor returns all languages for the given book") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("eng"))
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("nob"))
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("amh"))
      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("swa"))

      translationRepository.languagesFor(book1.id.get).map(_.toString).sorted should equal(Seq("amh", "eng", "nob", "swa"))
    }
  }

  test("that bookIdsWithLanguageAndLevel returns ids with same language when level is None") {
    withRollback { implicit  session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"))
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("amh"))
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"))
      addTranslationDef("ext4", "title 4", book4.id.get, LanguageTag("xho"))

      val ids = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 10, 1, Sort.ByIdAsc)

      ids.results should equal (Seq(book1.id.get, book3.id.get, book4.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevel only returns ids for same level") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"), Some("2"))
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("xho"), Some("2"))
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"), Some("1"))

      val ids = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), Some("2"), 10, 1, Sort.ByIdAsc)
      ids.results should equal (Seq(book1.id.get, book2.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevel returns correct page and pagesize") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "title 1", book1.id.get, LanguageTag("xho"), None)
      addTranslationDef("ext2", "title 2", book2.id.get, LanguageTag("xho"), None)
      addTranslationDef("ext3", "title 3", book3.id.get, LanguageTag("xho"), None)
      addTranslationDef("ext4", "title 4", book4.id.get, LanguageTag("xho"), None)

      val page1 = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 1, 1, Sort.ByIdAsc)
      page1.results should equal (Seq(book1.id.get))

      val page2 = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 1, 2, Sort.ByIdAsc)
      page2.results should equal (Seq(book2.id.get))

      val page3 = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 1, 3, Sort.ByIdAsc)
      page3.results should equal (Seq(book3.id.get))

      val doublePage = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 2, 2, Sort.ByIdAsc)
      doublePage.results should equal(Seq(book3.id.get, book4.id.get))
    }
  }

  test("that bookIdsWithLanguageAndLevel returns correctly sorted") {
    withRollback { implicit session =>
      val book1 = addBookDef()
      val book2 = addBookDef()
      val book3 = addBookDef()
      val book4 = addBookDef()

      addTranslationDef("ext1", "b", book1.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(1)))
      addTranslationDef("ext2", "a", book2.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(3)))
      addTranslationDef("ext3", "d", book3.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(2)))
      addTranslationDef("ext4", "c", book4.id.get, LanguageTag("xho"), None, Some(LocalDate.now().minusDays(4)))

      val byIdAsc = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 10, 1, Sort.ByIdAsc)
      byIdAsc.results should equal (Seq(book1.id.get, book2.id.get, book3.id.get, book4.id.get))

      val byIdDesc = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 10, 1, Sort.ByIdDesc)
      byIdDesc.results should equal (Seq(book4.id.get, book3.id.get, book2.id.get, book1.id.get))

      val byTitleAsc = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 10, 1, Sort.ByTitleAsc)
      byTitleAsc.results should equal (Seq(book2.id.get, book1.id.get, book4.id.get, book3.id.get))

      val byTitleDesc = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 10, 1, Sort.ByTitleDesc)
      byTitleDesc.results should equal (Seq(book3.id.get, book4.id.get, book1.id.get, book2.id.get))

      val byArrivalDateAsc = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 10, 1, Sort.ByArrivalDateAsc)
      byArrivalDateAsc.results should equal (Seq(book4.id.get, book2.id.get, book3.id.get, book1.id.get))

      val byArrivalDateDesc = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 10, 1, Sort.ByArrivalDateDesc)
      byArrivalDateDesc.results should equal (Seq(book1.id.get, book3.id.get, book2.id.get, book4.id.get))
    }
  }

  test("that that ByArrivalDate also sorts on id on same date") {
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

      val page1 = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 3, 1, Sort.ByArrivalDateDesc)
      val page2 = translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("xho"), None, 3, 2, Sort.ByArrivalDateDesc)

      page1.results should equal (Seq(book6.id.get, book5.id.get, book4.id.get))
      page2.results should equal (Seq(book3.id.get, book2.id.get, book1.id.get))
    }
  }
}