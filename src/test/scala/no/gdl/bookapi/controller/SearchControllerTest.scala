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
    when(searchService.search(Some("test"), LanguageTag(BookApiProperties.DefaultLanguage), 1, 1)).thenReturn(result)

    get("/?page=1&page-size=1&query=test") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (1)
    }
  }
}
