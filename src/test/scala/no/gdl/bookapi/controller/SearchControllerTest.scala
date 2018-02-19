/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi._
import no.gdl.bookapi.model.api.{Language, LocalDateSerializer, SearchResult}
import no.gdl.bookapi.model.domain.{Paging, Sort}
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.mockito.Mockito.when
import org.scalatra.test.scalatest.ScalatraFunSuite

class SearchControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite{
  implicit val formats: Formats = DefaultFormats + LocalDateSerializer
  implicit val swagger = new BookSwagger
  lazy val controller = new SearchController

  addServlet(controller, "/*")

  test("that search without lang searches for english books") {
    val result = SearchResult(0, 1, 10, Language("eng", "English"), Seq(TestData.Api.DefaultBook))
    when(searchService.searchWithQuery(LanguageTag(BookApiProperties.DefaultLanguage), None, Paging(1,10))).thenReturn(result)

    get("/") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult should equal(result)
    }
  }

  test("that search with lang searches for books in correct lang") {
    val result = SearchResult(0, 1, 10, Language("amh", "Amharic"), Seq(TestData.Api.DefaultBook))
    when(searchService.searchWithQuery(LanguageTag("amh"), None, Paging(1,1))).thenReturn(result)

    get("/amh/?page-size=1") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult should equal(result)
    }
  }
}
