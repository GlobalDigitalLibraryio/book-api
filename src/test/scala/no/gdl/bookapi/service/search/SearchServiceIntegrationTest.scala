/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.integration.{E4sClient, EsClientFactory}
import no.gdl.bookapi.model.domain.{Paging, Sort}
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import no.gdl.tag.IntegrationTest
import org.mockito.Mockito.when

@IntegrationTest
class SearchServiceIntegrationTest extends UnitSuite with TestEnvironment {

  override val searchService = new SearchService
  override val indexService = new IndexService
  override val esClient: E4sClient = E4sClient(EsClientFactory.nonSigningClient("http://localhost:9200")) // Requires running elasticsearch

  override def beforeAll(): Unit = {
    when(translationRepository.languagesFor(1)).thenReturn(Seq(LanguageTag(TestData.DefaultLanguage)))
    when(bookRepository.withId(1)).thenReturn(Some(TestData.Domain.DefaultBook))
    when(converterService.toApiBook(Some(TestData.Domain.DefaultTranslation), Seq(LanguageTag(TestData.DefaultLanguage)), Some(TestData.Domain.DefaultBook))).thenReturn(Some(TestData.Api.DefaultBook))
    val translation = TestData.Domain.DefaultTranslation
    indexService.indexDocument(translation)
  }

  test("that search for unknown language gives empty search result") {
    val searchResult = searchService.searchWithQuery(LanguageTag("aaa"), None, Paging(1,10), Sort.ByIdAsc)
    searchResult.totalCount should be(0)
  }

  test("that search for books in norwegian returns just that") {
    val searchResult = searchService.searchWithQuery(LanguageTag("nob"), None, Paging(1,10), Sort.ByIdAsc)
    searchResult.totalCount should be(1)
  }

}
