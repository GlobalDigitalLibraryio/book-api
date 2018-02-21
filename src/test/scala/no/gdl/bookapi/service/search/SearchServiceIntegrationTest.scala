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
  override val esClient: E4sClient = E4sClient(HttpClient("http://localhost:9200")) // Requires running elasticsearch

  override def beforeAll(): Unit = {
    when(translationRepository.languagesFor(1)).thenReturn(Seq(LanguageTag(TestData.DefaultLanguage)))
    when(bookRepository.withId(1)).thenReturn(Some(TestData.Domain.DefaultBook))
    when(converterService.toApiBook(Some(TestData.Domain.DefaultTranslation), Seq(LanguageTag(TestData.DefaultLanguage)), Some(TestData.Domain.DefaultBook)))
      .thenReturn(Some(TestData.Api.DefaultBook))
    indexService.indexDocument(TestData.Domain.DefaultTranslation)

    val additionalTranslation = TestData.Domain.DefaultTranslation.copy(id = Some(2), title = "Different")
    when(converterService.toApiBook(Some(additionalTranslation), Seq(LanguageTag(TestData.DefaultLanguage)), Some(TestData.Domain.DefaultBook)))
      .thenReturn(Some(TestData.Api.DefaultBook.copy(id = 2, title = "Different", description = "Title")))
    indexService.indexDocument(additionalTranslation)

    when(translationRepository.languagesFor(1)).thenReturn(Seq(LanguageTag(TestData.LanguageCodeAmharic)))
    when(converterService.toApiBook(Some(TestData.Domain.AmharicTranslation), Seq(LanguageTag(TestData.LanguageCodeAmharic)), Some(TestData.Domain.DefaultBook)))
      .thenReturn(Some(TestData.Api.BookInAmharic))
    indexService.indexDocument(TestData.Domain.AmharicTranslation)
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
    searchByRelevance.results.head.id should be(1)
    searchByRelevance.results.head.title should be("Title")

    val searchByIdDesc = searchService.searchWithQuery(LanguageTag("nob"), Some("title"), Paging(1,10), Sort.ByIdDesc)
    searchByIdDesc.totalCount should be(2)
    searchByIdDesc.results.head.id should be(2)
    searchByIdDesc.results.head.title should be("Different")
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
}
