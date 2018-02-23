/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.admin.IndicesExists
import com.sksamuel.elastic4s.alias.GetAliasesDefinition
import com.sksamuel.elastic4s.http.index.admin.IndexExistsResponse
import com.sksamuel.elastic4s.http.index.alias.{Alias, IndexAliases}
import com.sksamuel.elastic4s.http.{HttpExecutable, RequestSuccess}
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.util.{Failure, Success}

class IndexServiceTest extends UnitSuite with TestEnvironment {
  val service = new IndexService

  test("that indexing translation returns same translation") {
    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(true))))

    val translation = service.indexDocument(TestData.Domain.DefaultTranslation)
    translation.get should equal(TestData.Domain.DefaultTranslation)
  }

  test("that indexing list of translations returns count of translations") {
    when(bookRepository.withId(1)).thenReturn(Some(TestData.Domain.DefaultBook))
    when(converterService.toApiBookHit(Some(TestData.Domain.DefaultTranslation), Some(TestData.Domain.DefaultBook))).thenReturn(Some(TestData.Api.DefaultBookHit))
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

  test("that index is correct for alias") {
    when(esClient.execute(any[GetAliasesDefinition])(any[HttpExecutable[GetAliasesDefinition,IndexAliases]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexAliases(Map(Index("books-nob_timestamp_uuid") -> Seq(Alias("books-nob")))))))

    val alias = service.aliasTarget(LanguageTag(TestData.DefaultLanguage))
    alias.get should be(Some("books-nob_timestamp_uuid"))
  }

  test("that alias for index is updated") {
    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(true))))

    val updated = service.updateAliasTarget(Some("index-nob_1"), "index-nob_2", LanguageTag(TestData.DefaultLanguage))
    updated should be(Success())
  }

  test("that index is deleted") {
    val deleted = service.deleteSearchIndex(Some("books-aaa"))
    deleted should be(Success())

    when(esClient.execute(any[IndicesExists])(any[HttpExecutable[IndicesExists,IndexExistsResponse]]))
      .thenReturn(Success(RequestSuccess(200, Some(""), Map(), IndexExistsResponse(false))))

    val deletedAgain = service.deleteSearchIndex(Some("books-aaa"))
    deletedAgain match {
      case Failure(_:IllegalArgumentException) => Success()
      case _ => fail("Wrong exception")
    }
  }

}
