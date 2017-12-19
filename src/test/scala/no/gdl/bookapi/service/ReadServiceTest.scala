/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import java.time.LocalDate

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.model.domain.{EditorsPick, SearchResult, Sort}
import org.mockito.Mockito._
import org.mockito.Matchers._
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import no.gdl.bookapi.model._

class ReadServiceTest extends UnitSuite with TestEnvironment {
  override val readService = new ReadService

  test("that editorsPickForLanguage returns Seq() when no editorspick defined for language") {
    val languageTag = LanguageTag("nob")

    when(editorsPickRepository.forLanguage(languageTag)).thenReturn(None)
    readService.editorsPickForLanguage(languageTag) should be(None)
  }

  test("that editorsPickForLanguage returns list of books for correct language") {
    val editorsPick = EditorsPick(Some(1), Some(1), LanguageTag("nob"), Seq(1, 2), LocalDate.now())

    when(editorsPickRepository.forLanguage(LanguageTag("nob"))).thenReturn(Some(editorsPick))
    when(translationRepository.withId(1)).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(translationRepository.withId(2)).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(converterService.toApiBook(any[Option[domain.Translation]], any[Seq[LanguageTag]], any[Option[domain.Book]])).thenReturn(Some(TestData.Api.DefaultBook))

    val editorsPickForNob = readService.editorsPickForLanguage(LanguageTag("nob"))
    editorsPickForNob.get.books should equal(Seq(TestData.Api.DefaultBook, TestData.Api.DefaultBook))
  }

  test("that editorsPickForLanguage only returns existing translations even if definition is incorrect") {
    val editorsPick = EditorsPick(Some(1), Some(1), LanguageTag("nob"), Seq(1, 2), LocalDate.now())

    when(editorsPickRepository.forLanguage(LanguageTag("nob"))).thenReturn(Some(editorsPick))
    when(translationRepository.withId(1)).thenReturn(Some(TestData.Domain.DefaultTranslation))
    when(translationRepository.withId(2)).thenReturn(None)
    when(converterService.toApiBook(any[Option[domain.Translation]], any[Seq[LanguageTag]], any[Option[domain.Book]])).thenReturn(Some(TestData.Api.DefaultBook))

    val editorsPickForNob = readService.editorsPickForLanguage(LanguageTag("nob"))
     editorsPickForNob.get.books should equal(Seq(TestData.Api.DefaultBook))
  }

  test("that editorsPickForLanguage returns empty list if no ids defined in editorspick") {
    val editorsPick = EditorsPick(Some(1), Some(1), LanguageTag("nob"), Seq(), LocalDate.now())

    when(editorsPickRepository.forLanguage(LanguageTag("nob"))).thenReturn(Some(editorsPick))

    readService.editorsPickForLanguage(LanguageTag("nob")).get.books should equal(Seq())
  }

  test("that listAvailableLanguages returns languages sorted by name") {
    when(translationRepository.allAvailableLanguages()).thenReturn(Seq(LanguageTag(TestData.LanguageCodeEnglish), LanguageTag(TestData.LanguageCodeAmharic)))
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeAmharic))).thenReturn(TestData.Api.amharic)
    when(converterService.toApiLanguage(LanguageTag(TestData.LanguageCodeEnglish))).thenReturn(TestData.Api.english)

    readService.listAvailableLanguages should equal(Seq(TestData.Api.amharic, TestData.Api.english))
  }

  test("that similarTo returns empty searchresult when given id does not exist") {
    when(translationRepository.forBookIdAndLanguage(1, LanguageTag("nob"))).thenReturn(None)
    when(translationRepository.languagesFor(1)).thenReturn(Seq())
    when(bookRepository.withId(1)).thenReturn(None)
    when(converterService.toApiBook(None, Seq(), None)).thenReturn(None)
    when(converterService.toApiLanguage(LanguageTag("nob"))).thenReturn(TestData.Api.norwegian_bokmal)

    val searchResult = readService.similarTo(1, LanguageTag("nob"), 10, 1, Sort.ByIdAsc)
    searchResult.results should equal(Seq())
    searchResult.language should equal(TestData.Api.norwegian_bokmal)
    searchResult.page should equal(1)
    searchResult.pageSize should equal(10)
    searchResult.totalCount should equal(0)
  }

  test("that similarTo filters out the given id from similar books") {
    val page = 1
    val pageSize = 10

    // For getting the book to find similar for
    when(translationRepository.forBookIdAndLanguage(1, LanguageTag("nob"))).thenReturn(None)
    when(translationRepository.languagesFor(1)).thenReturn(Seq())
    when(bookRepository.withId(1)).thenReturn(None)
    when(converterService.toApiLanguage(LanguageTag("nob"))).thenReturn(TestData.Api.norwegian_bokmal)
    when(converterService.toApiBook(None, Seq(), None)).thenReturn(Some(TestData.Api.DefaultBook))

    // For getting book with id 2
    val book2 = TestData.Domain.DefaultBook.copy(id = Some(2))
    val translation2 = TestData.Domain.DefaultTranslation.copy(id = Some(2))
    val similarBook = TestData.Api.DefaultBook.copy(id = 2)

    when(translationRepository.forBookIdAndLanguage(2, LanguageTag("nob"))).thenReturn(Some(translation2))
    when(translationRepository.languagesFor(2)).thenReturn(Seq(LanguageTag("nob")))
    when(bookRepository.withId(2)).thenReturn(Some(book2))
    when(converterService.toApiLanguage(LanguageTag("nob"))).thenReturn(TestData.Api.norwegian_bokmal)
    when(converterService.toApiBook(Some(translation2), Seq(LanguageTag("nob")), Some(book2))).thenReturn(Some(similarBook))

    val searchResult = SearchResult[Long](2, page, pageSize, LanguageTag("nob"), Seq(TestData.Api.DefaultBook.id, 2))
    when(translationRepository.bookIdsWithLanguageAndLevel(LanguageTag("nob"), TestData.Api.DefaultBook.readingLevel, pageSize, page, Sort.ByIdAsc)).thenReturn(searchResult)

    readService.similarTo(TestData.Api.DefaultBook.id, LanguageTag("nob"), pageSize, page, Sort.ByIdAsc).results should equal(Seq(similarBook))
  }
}
