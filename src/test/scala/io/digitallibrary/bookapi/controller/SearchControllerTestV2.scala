package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.model.api.{Language, LocalDateSerializer, SearchResultV2}
import io.digitallibrary.bookapi.model.domain.{Paging, Sort}
import io.digitallibrary.bookapi.{BookSwagger, TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.mockito.Mockito.{verify, when}

class SearchControllerTestV2 extends UnitSuite with TestEnvironment with ScalatraFunSuite {
    implicit val formats: Formats = DefaultFormats + LocalDateSerializer
    implicit val swagger = new BookSwagger
    lazy val controllerV2 = new SearchControllerV2

    addServlet(controllerV2, "/*")

    test("that search without lang searches for all books") {
      val result = SearchResultV2(0, 1, 10, None, Seq(TestData.Api.DefaultBookHitV2))
      when(searchServiceV2.searchWithQueryForAllLanguages(None, None, Paging(1,10), Sort.ByRelevance)).thenReturn(result)

      get("/") {
        status should equal (200)
        val searchResult = read[SearchResultV2](body)
        searchResult should equal(result)
        verify(searchServiceV2).searchWithQueryForAllLanguages(None, None, Paging(1, 10), Sort.ByRelevance)
      }
    }

    test("that search with lang as query param searches for books in correct lang") {
      val result = SearchResultV2(0, 1, 10, Some(Language("om", "Afaan Oromoo")), Seq(TestData.Api.DefaultBookHitV2))
      when(searchServiceV2.searchWithQuery(LanguageTag("om"), None, None, Paging(1,10), Sort.ByRelevance)).thenReturn(result)

      get("/?language=om") {
        status should equal (200)
        val searchResult = read[SearchResultV2](body)
        searchResult should equal(result)
        verify(searchServiceV2).searchWithQuery(LanguageTag("om"), None, None, Paging(1, 10), Sort.ByRelevance)
      }
    }

    test("that search with lang searches for books in correct lang") {
      val result = SearchResultV2(0, 1, 10, Some(Language("amh", "Amharic")), Seq(TestData.Api.DefaultBookHitV2))
      when(searchServiceV2.searchWithQuery(LanguageTag("amh"), None, None, Paging(1,1), Sort.ByRelevance)).thenReturn(result)

      get("/amh/?page-size=1") {
        status should equal (200)
        val searchResult = read[SearchResultV2](body)
        searchResult should equal(result)
      }
    }
  }
