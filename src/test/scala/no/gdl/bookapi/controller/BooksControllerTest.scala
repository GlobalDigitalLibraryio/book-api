/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller
import java.text.SimpleDateFormat

import no.gdl.bookapi._
import no.gdl.bookapi.model.api._
import no.gdl.bookapi.model.domain.Sort
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatra.test.scalatest.ScalatraFunSuite


class BooksControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  } + LocalDateSerializer

  implicit val swagger = new BookSwagger

  lazy val controller = new BooksController
  addServlet(controller, "/*")

  test("that GET / will get books with default language") {
    val result = SearchResult(0, 1, 10, Language("eng", "Englist"), Seq(TestData.Api.DefaultBook))
    when(readService.withLanguageAndLevel(BookApiProperties.DefaultLanguage, Some("1"), 10, 1, Sort.ByIdAsc)).thenReturn(result)

    get("/?reading-level=1&page-size=10&page=1") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (1)
      searchResult.results.head.uuid should equal(TestData.DefaultUuid)
    }
  }

  test("that GET /:lang will get books with language") {
    val language = TestData.Api.norwegian_bokmal

    val result = SearchResult(0, 1, 10, language, Seq(TestData.Api.DefaultBook))
    when(readService.withLanguageAndLevel(language.code, Some("2"), 10, 1, Sort.ByIdAsc)).thenReturn(result)

    get("/nob?reading-level=2&page-size=10&page=1") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (1)
      searchResult.results.head.uuid should equal(TestData.Api.DefaultBook.uuid)
    }
  }

  test("that GET /:lang will get books with language sorted by title descending") {
    val language = TestData.Api.norwegian_bokmal

    val firstBook = TestData.Api.DefaultBook.copy(id = 2, title = "This should be first")
    val secondBook = TestData.Api.DefaultBook.copy(id = 1, title = "This should be last")

    val result = SearchResult(2, 1, 10, language, Seq(firstBook, secondBook))

    when(readService.withLanguageAndLevel(language.code, Some("2"), 10, 1, Sort.ByTitleDesc)).thenReturn(result)

    get("/nob?reading-level=2&page-size=10&page=1&sort=-title") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (2)
      searchResult.results should equal (Seq(firstBook, secondBook))
    }
  }

  test("that GET/:lang/:id returns 400 when id is not numberic") {
    get("/eng/abc") {
      status should equal (400)
    }
  }

  test("that GET /:lang/:id returns 404 when not found") {
    when(readService.withIdAndLanguage(any[Long], any[String])).thenReturn(None)

    get("/eng/1") {
      status should equal (404)
    }
  }

  test("that GET /:lang/:id returns book when found") {
    when(readService.withIdAndLanguage(any[Long], any[String])).thenReturn(Some(TestData.Api.DefaultBook))

    get("/eng/1") {
      status should equal (200)
      val book = read[Book](body)
      book.uuid should equal (TestData.Api.DefaultBook.uuid)
    }
  }

  test("that GET /:lang/:bookid/chapters/:chapterid returns 404 when not found") {
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[String], any[Long])).thenReturn(None)
    get("/eng/1/chapters/1") {
      status should equal (404)
    }
  }

  test("that GET /:lang/:bookid/chapters/:chapterid returns chapter when found") {
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[String], any[Long])).thenReturn(Some(TestData.Api.Chapter1))
    get("/eng/1/chapters/1") {
      status should equal (200)
      val chapter = read[Chapter](body)
      chapter.title should equal (TestData.Api.Chapter1.title)
    }
  }
}
