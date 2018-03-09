/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import com.sksamuel.elastic4s.http.HttpClient
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.E4sClient
import no.gdl.bookapi.model.domain.{Paging, Sort}
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import no.gdl.tag.IntegrationTest
import org.mockito.Mockito.when

@IntegrationTest
class SearchServiceIntegrationTest extends UnitSuite with TestEnvironment {

  override val searchService = new SearchService
  override val indexService = new IndexService
  override val esClient: E4sClient = E4sClient(HttpClient("http://localhost:9200")) // Requires empty running elasticsearch with icu-plugin

  override def beforeAll(): Unit = {

    when(bookRepository.withId(1)).thenReturn(Some(TestData.Domain.DefaultBook))

    when(converterService.toApiBookHit(Some(TestData.Domain.DefaultTranslation), Some(TestData.Domain.DefaultBook))).thenReturn(Some(TestData.Api.DefaultBookHit))
    indexService.indexDocument(TestData.Domain.DefaultTranslation)

    val additionalTranslation = TestData.Domain.DefaultTranslation.copy(id = Some(2), title = "Different")
    when(converterService.toApiBookHit(Some(additionalTranslation), Some(TestData.Domain.DefaultBook))).thenReturn(Some(TestData.Api.DefaultBookHit.copy(id = 2, title = "Different title", description = "Title")))
    indexService.indexDocument(additionalTranslation)

    when(converterService.toApiBookHit(Some(TestData.Domain.AmharicTranslation), Some(TestData.Domain.DefaultBook))).thenReturn(Some(TestData.Api.BookInAmharic))
    indexService.indexDocument(TestData.Domain.AmharicTranslation)

    val bookWithDifferentSource = TestData.Domain.DefaultBook.copy(source = "some_unknown_source")
    val translationWithDifferentSource = TestData.Domain.DefaultTranslation.copy(id = Some(3), bookId = 2, title = "Some stuff here...", language = LanguageTag("eng"))
    val bookHitWithDifferentSource = TestData.Api.DefaultBookHit.copy(id = 3, title = "Some stuff here...", source = "some_unknown_source", language = TestData.Api.english)
    when(bookRepository.withId(2)).thenReturn(Some(bookWithDifferentSource))
    when(converterService.toApiBookHit(Some(translationWithDifferentSource), Some(bookWithDifferentSource))).thenReturn(Some(bookHitWithDifferentSource))
    indexService.indexDocument(translationWithDifferentSource)
  }

  test("that search for unknown language gives empty search result") {
    val searchResult = searchService.searchWithQuery(LanguageTag("aaa"), None, Paging(1,10), Sort.ByRelevance)
    searchResult.totalCount should be(0)
  }

  test("that search for books in norwegian returns two books") {
    val searchResult = searchService.searchWithQuery(LanguageTag("nob"), None, Paging(1,10), Sort.ByRelevance)
    searchResult.totalCount should be(2)
    searchResult.results.head.language.code should be("nob")
  }

  test("that search for books in amharic returns one book") {
    val searchResult = searchService.searchWithQuery(LanguageTag("amh"), None, Paging(1,10), Sort.ByRelevance)
    searchResult.totalCount should be(1)
    searchResult.results.head.language.code should be("amh")
  }

  test("that search for title returns expected") {
    val searchResult = searchService.searchWithQuery(LanguageTag("nob"), Some("nonexisting"), Paging(1,10), Sort.ByRelevance)
    searchResult.totalCount should be(0)

    val searchByRelevance = searchService.searchWithQuery(LanguageTag("nob"), Some("title"), Paging(1,10), Sort.ByRelevance)
    searchByRelevance.totalCount should be(2)
    searchByRelevance.results.head.id should be(2)
    searchByRelevance.results.head.title should be("Different title")

    val searchByIdDesc = searchService.searchWithQuery(LanguageTag("nob"), Some("title"), Paging(1,10), Sort.ByIdAsc)
    searchByIdDesc.totalCount should be(2)
    searchByIdDesc.results.head.id should be(1)
    searchByIdDesc.results.head.title should be("Title")
  }

  test("that sort on title gives correct sorting") {
    val searchByTitleAsc = searchService.searchWithQuery(LanguageTag("nob"), Some("title"), Paging(1,10), Sort.ByTitleAsc)
    searchByTitleAsc.totalCount should be(2)
    searchByTitleAsc.results.head.id should be(2)
    searchByTitleAsc.results.head.title should be("Different title")

    val searchByTitleDesc = searchService.searchWithQuery(LanguageTag("nob"), Some("title"), Paging(1,10), Sort.ByTitleDesc)
    searchByTitleDesc.totalCount should be(2)
    searchByTitleDesc.results.head.id should be(1)
    searchByTitleDesc.results.head.title should be("Title")
  }


  test("that search for level returns expected") {
    val searchResult = searchService.searchWithLevel(LanguageTag("nob"), Some("2"), Paging(1,10), Sort.ByIdAsc)
    searchResult.totalCount should be(0)

    val searchAgain = searchService.searchWithLevel(LanguageTag("nob"), Some("1"), Paging(1,10), Sort.ByIdAsc)
    searchAgain.totalCount should be(2)
    searchAgain.results.head.readingLevel should be(Some("1"))
  }

  test("that search similar for norwegian returns one book") {
    when(translationRepository.forBookIdAndLanguage(1, LanguageTag("nob"))).thenReturn(Some(TestData.Domain.DefaultTranslation))
    val searchResult = searchService.searchSimilar(LanguageTag("nob"), 1, Paging(1,10), Sort.ByIdAsc)
    searchResult.totalCount should be(1)
  }

  test("that search similar for unknown language returns empty") {
    when(translationRepository.forBookIdAndLanguage(1, LanguageTag("aaa"))).thenReturn(Some(TestData.Domain.DefaultTranslation))
    val searchResult = searchService.searchSimilar(LanguageTag("aaa"), 1, Paging(1,10), Sort.ByIdAsc)
    searchResult.totalCount should be(0)
  }

  test("that filtering on source only returns book with given source") {
    when(translationRepository.forBookIdAndLanguage(3, LanguageTag("eng"))).thenReturn(Some(TestData.Domain.DefaultTranslation))
    val searchResult = searchService.searchWithQuery(LanguageTag("eng"), None, Paging(1,10), Sort.ByIdAsc, source = Some("some_unknown_source"))
    searchResult.totalCount should be (1)
  }

  test("that filtering on source and combined search only gives hit on correct book") {
    when(translationRepository.forBookIdAndLanguage(2, LanguageTag("nob"))).thenReturn(Some(TestData.Domain.DefaultTranslation))
    val searchResult = searchService.searchWithQuery(LanguageTag("nob"), Some("different"), Paging(1,10), Sort.ByIdAsc, source = Some("storyweaver"))
    searchResult.totalCount should be (1)
  }

  test("that filtering on source and combined search gives no hits if source is incorrect") {
    when(translationRepository.forBookIdAndLanguage(2, LanguageTag("nob"))).thenReturn(Some(TestData.Domain.DefaultTranslation))
    val searchResult = searchService.searchWithQuery(LanguageTag("nob"), Some("different"), Paging(1,10), Sort.ByIdAsc, source = Some("bookdash"))
    searchResult.totalCount should be (0)
  }
}
