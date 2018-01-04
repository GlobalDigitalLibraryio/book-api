/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller
import java.text.SimpleDateFormat

import io.digitallibrary.language.model.LanguageTag
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

  val validTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UifQ.e3BKK_gLxWQwJhFX6SppNchM_eSwu82yKghVx2P3yMY"
  val invalidTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9hYmMiOiIxMjMiLCJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.5rtcIdtPmH3AF1pwNbNvBMKmulyiEoWZfn1ip9aMzv4"

  test("that GET / will get books with default language") {
    val result = SearchResult(0, 1, 10, Language("eng", "English"), Seq(TestData.Api.DefaultBook))
    when(readService.withLanguageAndLevel(LanguageTag(BookApiProperties.DefaultLanguage), Some("1"), 10, 1, Sort.ByIdAsc)).thenReturn(result)

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
    when(readService.withLanguageAndLevel(LanguageTag(language.code), Some("2"), 10, 1, Sort.ByIdAsc)).thenReturn(result)

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
    when(readService.withLanguageAndLevel(LanguageTag(language.code), Some("2"), 10, 1, Sort.ByTitleDesc)).thenReturn(result)

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
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(None)

    get("/eng/1") {
      status should equal (404)
    }
  }

  test("that GET /:lang/:id returns book when found") {
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))

    get("/eng/1") {
      status should equal (200)
      val book = read[Book](body)
      book.uuid should equal (TestData.Api.DefaultBook.uuid)
    }
  }

  test("that GET /:lang/:bookid/chapters/:chapterid returns 404 when not found") {
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(None)
    get("/eng/1/chapters/1") {
      status should equal (404)
    }
  }

  test("that GET /:lang/:bookid/chapters/:chapterid returns chapter when found") {
    when(readService.chapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Api.Chapter1))
    get("/eng/1/chapters/1") {
      status should equal (200)
      val chapter = read[Chapter](body)
      chapter.title should equal (TestData.Api.Chapter1.title)
    }
  }

  test("that GET /mine returns AccessDenied for no user") {
    get("/mine") {
      status should equal (403)
    }
  }

  test("that GET /mine returns AccessDenied for an invalid token") {
    get("/mine", headers = Seq(("Authorization", s"Bearer $invalidTestToken"))) {
      status should equal (403)
      val error = read[Error](body)
      error.code should equal ("ACCESS DENIED")
    }
  }

  test("that GET /mine returns 200 ok for a valid user") {
    get("/mine", headers = Seq(("Authorization", s"Bearer $validTestToken"))) {
      status should equal (200)
    }
  }

  test("that GET /mine/1 returns AccessDenied for no user") {
    get("/mine/123") {
      status should equal (403)
      val error = read[Error](body)
      error.code should equal ("ACCESS DENIED")
    }
  }

  test("that GET /mine/1 returns AccessDenied for invalid user") {
    get("/mine/123", headers = Seq(("Authorization", s"Bearer $invalidTestToken"))) {
      status should equal (403)
      val error = read[Error](body)
      error.code should equal ("ACCESS DENIED")
    }
  }

  test("that GET /mine/1 returns 200 ok for a valid user") {
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))

    get("/mine/123", headers = Seq(("Authorization", s"Bearer $validTestToken"))) {
      status should equal (200)
      val book = read[Book](body)
      book.uuid should equal (TestData.Api.DefaultBook.uuid)
    }
  }
}
