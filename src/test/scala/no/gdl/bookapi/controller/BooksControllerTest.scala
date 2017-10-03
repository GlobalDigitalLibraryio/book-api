/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller
import no.gdl.bookapi._
import no.gdl.bookapi.model.api.{Book, Chapter, Language, SearchResult}
import org.json4s.native.Serialization._
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatra.test.scalatest.ScalatraFunSuite


class BooksControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new BookSwagger

  lazy val controller = new BooksController
  addServlet(controller, "/*")

  test("that GET / will get books with default language") {
    val result = SearchResult(0, 1, 10, Language("eng", "Englist"), Seq(TestData.DefaultBook))
    when(readService.withLanguageAndLevel(BookApiProperties.DefaultLanguage, Some("1"), 10, 1)).thenReturn(result)

    get("/?reading-level=1&page-size=10&page=1") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (1)
      searchResult.results.head.uuid should equal(TestData.DefaultUuid)
    }
  }

  test("that GET /:lang will get books with language") {
    val language = TestData.norwegian_bokmal

    val result = SearchResult(0, 1, 10, language, Seq(TestData.DefaultBook))
    when(readService.withLanguageAndLevel(language.code, Some("2"), 10, 1)).thenReturn(result)

    get("/nob?reading-level=2&page-size=10&page=1") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (1)
      searchResult.results.head.uuid should equal(TestData.DefaultBook.uuid)
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
    when(readService.withIdAndLanguage(any[Long], any[String])).thenReturn(Some(TestData.DefaultBook))

    get("/eng/1") {
      status should equal (200)
      val book = read[Book](body)
      book.uuid should equal (TestData.DefaultBook.uuid)
    }
  }

  test("that GET /:lang/:bookid/chapters/:chapterid returns 404 when not found") {
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[String], any[Long])).thenReturn(None)
    get("/eng/1/chapters/1") {
      status should equal (404)
    }
  }

  test("that GET /:lang/:bookid/chapters/:chapterid returns chapter when found") {
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[String], any[Long])).thenReturn(Some(TestData.Chapter1))
    get("/eng/1/chapters/1") {
      status should equal (200)
      val chapter = read[Chapter](body)
      chapter.title should equal (TestData.Chapter1.title)
    }
  }
}
