/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import com.sksamuel.elastic4s.admin.IndicesExists
import com.sksamuel.elastic4s.http.index.admin.IndexExistsResponse
import com.sksamuel.elastic4s.http.{HttpExecutable, RequestSuccess}
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers.any
import org.mockito.Mockito.when

import scala.util.Success

class IndexServiceTest extends UnitSuite with TestEnvironment {
  val service = new IndexService

  test("that indexing translation returns same translation") {
    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(true))))

    val translation = service.indexDocument(TestData.Domain.DefaultTranslation)
    translation.get should equal(TestData.Domain.DefaultTranslation)
  }

  test("that indexing list of translations returns count of translations") {
    when(translationRepository.languagesFor(1)).thenReturn(Seq(LanguageTag(TestData.DefaultLanguage)))
    when(bookRepository.withId(1)).thenReturn(Some(TestData.Domain.DefaultBook))
    when(converterService.toApiBook(Some(TestData.Domain.DefaultTranslation), Seq(LanguageTag(TestData.DefaultLanguage)), Some(TestData.Domain.DefaultBook))).thenReturn(Some(TestData.Api.DefaultBook))
    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(true))))

    val count = service.indexDocuments(List(TestData.Domain.DefaultTranslation), "books-nob")
    count should be(Success(1))
  }

  test("that index created for language is correctly named") {
    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(false))))

    val indexName = service.createSearchIndex(LanguageTag(TestData.DefaultLanguage))
    indexName.get should startWith("books-nob")
  }

  test("that index does and does not exist") {
    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(true))))

    val exists = service.indexExisting("books-nob")
    exists should be(Success(true))

    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(false))))

    val notexists = service.indexExisting("books-nob")
    notexists should be(Success(false))
  }

  test("that alias for index is updated") {
    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(true))))

    val updated = service.updateAliasTarget(Some("index-nob_1"), "index-nob_2", LanguageTag(TestData.DefaultLanguage))
    updated should be(Success())
  }

}
