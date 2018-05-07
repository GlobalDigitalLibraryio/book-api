/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller
import java.text.SimpleDateFormat

import io.digitallibrary.bookapi._
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.domain
import io.digitallibrary.bookapi.model.domain.{ChapterType, PageOrientation, Paging, Sort}
import io.digitallibrary.language.model.LanguageTag
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite


class BooksControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  } + LocalDateSerializer

  implicit val swagger = new BookSwagger

  lazy val controller = new BooksController

  addServlet(controller, "/*")

  val validTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UifQ.e3BKK_gLxWQwJhFX6SppNchM_eSwu82yKghVx2P3yMY"
  val validTestTokenWithWriteRole = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJzY29wZSI6ImJvb2tzLWxvY2FsOndyaXRlIn0.RNLeTpQogFoHRgwz5bJN2INvszK-YSgiJS4yatJvvFs"
  val invalidTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9hYmMiOiIxMjMiLCJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIn0.5rtcIdtPmH3AF1pwNbNvBMKmulyiEoWZfn1ip9aMzv4"

  test("that GET / will get books with default language") {
    val result = SearchResult(0, 1, 10, Language("eng", "English"), Seq(TestData.Api.DefaultBookHit))
    when(searchService.searchWithCategoryAndLevel(languageTag = LanguageTag(BookApiProperties.DefaultLanguage), category = None, readingLevel = Some("1"), source = None, paging = Paging(1, 10), sort = Sort.ByIdAsc)).thenReturn(result)

    get("/?reading-level=1&page-size=10&page=1") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (1)
      searchResult.results.head.language.code should equal(TestData.DefaultLanguage)
    }
  }

  test("that GET /:lang will get books with language") {
    val language = TestData.Api.norwegian_bokmal

    val result = SearchResult(0, 1, 10, language, Seq(TestData.Api.DefaultBookHit))
    when(searchService.searchWithCategoryAndLevel(languageTag = LanguageTag(language.code), category = None, readingLevel = Some("2"), source = None, paging = Paging(1, 10), sort = Sort.ByIdAsc)).thenReturn(result)

    get("/nob?reading-level=2&page-size=10&page=1") {
      status should equal (200)
      val searchResult = read[SearchResult](body)
      searchResult.results.length should be (1)
      searchResult.results.head.language.code should equal(TestData.DefaultLanguage)
    }
  }

  test("that GET /:lang will get books with language sorted by title descending") {
    val language = TestData.Api.norwegian_bokmal

    val firstBook = TestData.Api.DefaultBookHit.copy(id = 2, title = "This should be first")
    val secondBook = TestData.Api.DefaultBookHit.copy(id = 1, title = "This should be last")

    val result = SearchResult(2, 1, 10, language, Seq(firstBook, secondBook))
    when(searchService.searchWithCategoryAndLevel(languageTag = LanguageTag(language.code), category = None, readingLevel = Some("2"), source = None, paging = Paging(1, 10), sort = Sort.ByTitleDesc)).thenReturn(result)

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

  test("that PUT /:lang/:id updates book, with only certain fields modified") {
    when(readService.translationWithIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultTranslation))

    val payload = """{
      |	"id": 1,
      |	"revision": 1,
      |	"externalId": "1002-anaya-s-thumb",
      |	"uuid": "d251aa7a-bba7-47ca-9702-e829dc18cfb9",
      |	"title": "new title",
      |	"description": "new description",
      |	"language": {
      |		"code": "en",
      |		"name": "English"
      |	},
      |	"availableLanguages": [
      |		{
      |			"code": "en",
      |			"name": "English"
      |		}
      |	],
      |	"license": {
      |		"id": 1,
      |		"revision": 1,
      |		"name": "cc-by-4.0",
      |		"description": "Attribution 4.0 International (CC BY 4.0)",
      |		"url": "https://creativecommons.org/licenses/by/4.0/"
      |	},
      |	"publisher": {
      |		"id": 1,
      |		"revision": 1,
      |		"name": "Pratham Books"
      |	},
      |	"readingLevel": "2",
      |	"typicalAgeRange": "8-10",
      |	"educationalUse": "reading",
      |	"educationalRole": "learner",
      |	"timeRequired": "PT10M",
      |	"datePublished": "2015-11-13",
      |	"dateCreated": "2018-01-08",
      |	"dateArrived": "2018-02-08",
      |	"categories": [
      |		{
      |			"id": 4,
      |			"revision": 1,
      |			"name": "library_books"
      |		}
      |	],
      |	"coverImage": {
      |		"url": "http://local.digitallibrary.io/image-api/raw/f51fb826a78e25dd5910a610cfd4a2ce.jpg",
      |		"alttext": "Some alt text"
      |	},
      |	"downloads": {
      |		"epub": "http://local.digitallibrary.io/book-api/download/epub/en/d251aa7a-bba7-47ca-9702-e829dc18cfb9.epub",
      |		"pdf": "http://local.digitallibrary.io/book-api/download/pdf/en/d251aa7a-bba7-47ca-9702-e829dc18cfb9.pdf"
      |	},
      |	"tags": [],
      |	"contributors": [
      |	],
      |	"chapters": [
      |	],
      |	"supportsTranslation": false,
      |	"bookFormat": "HTML",
      |	"source": "storyweaver",
      | "pageOrientation": "LANDSCAPE"
      |}""".stripMargin.getBytes

    put("/eng/1", payload, headers = Seq(("Authorization", s"Bearer $validTestTokenWithWriteRole"))) {
      status should equal (200)
      verify(writeService).updateTranslation(TestData.Domain.DefaultTranslation.copy(title = "new title", about = "new description", pageOrientation = PageOrientation.LANDSCAPE))
    }
  }

  test("that downloads in book is correct") {
    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook))

    get("/eng/1") {
      status should equal (200)
      val book = read[Book](body)
      book.downloads.epub should be(Some("url-to-epub"))
      book.downloads.pdf should be(None)
    }

    when(readService.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Api.DefaultBook.copy(downloads = Downloads(None, Some("url-to-pdf")))))

    get("/eng/1") {
      status should equal (200)
      val book = read[Book](body)
      book.downloads.epub should be(None)
      book.downloads.pdf should be(Some("url-to-pdf"))
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

  test("that PUT /:lang/:bookid/chapters/:chapterid demands valid token") {
    val payload =
      """
        | { "id": 1,
        |   "revision": 1,
        |   "translationId": 1,
        |   "seqNo": 1,
        |   "title": "my title",
        |   "content": "my updated content",
        |   "chapterType": "CONTENT"
        | }
      """.stripMargin.getBytes
    put("/eng/1/chapters/1", payload, headers = Seq(("Authorization", s"Bearer $invalidTestToken"))) {
      status should equal (403)
    }
  }

  test("that PUT /:lang/:bookid/chapters/:chapterid demands write role") {
    val payload =
      """
        | { "id": 1,
        |   "revision": 1,
        |   "translationId": 1,
        |   "seqNo": 1,
        |   "title": "my title",
        |   "content": "my updated content",
        |   "chapterType": "CONTENT"
        | }
      """.stripMargin.getBytes
    put("/eng/1/chapters/1", payload, headers = Seq(("Authorization", s"Bearer $validTestToken"))) {
      status should equal (403)
    }
  }

  test("that PUT /:lang/:bookid/chapters/:chapterid changes only chapter content and chapterType") {
    when(readService.domainChapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(mergeService.mergeContents(TestData.Domain.DefaultChapter.content, "my updated content")).thenReturn("my updated content")
    when(writeService.updateChapter(any[domain.Chapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(contentConverter.toApiContent(any[String])).thenReturn("")
    val payload =
      """
        | { "id": 1,
        |   "revision": 1,
        |   "translationId": 1,
        |   "seqNo": 1,
        |   "title": "my title",
        |   "content": "my updated content",
        |   "chapterType": "LICENSE"
        | }
      """.stripMargin.getBytes
    put("/eng/1/chapters/1", payload, headers = Seq(("Authorization", s"Bearer $validTestTokenWithWriteRole"))) {
      status should equal (200)
      verify(writeService).updateChapter(TestData.Domain.DefaultChapter.copy(content = "my updated content", chapterType = ChapterType.License))
    }
  }

  test("that PUT /:lang/:bookid/chapters/:chapterid converts Picture-tags back to embed-tags") {
    when(readService.domainChapterForBookWithLanguageAndId(any[Long], any[LanguageTag], any[Long])).thenReturn(Some(TestData.Domain.DefaultChapter))
    when(mergeService.mergeContents(any[String], any[String])).thenReturn("<embed ...> <br /> Some content here")
    when(writeService.updateChapter(any[domain.Chapter])).thenReturn(TestData.Domain.DefaultChapter)
    when(contentConverter.toApiContent(any[String])).thenReturn("")
    val pictureContent =
      """<picture><source media=\"(min-width: 768px)\" srcset=\"https://images.test.digitallibrary.io/eB1RNSN8.jpg\" /><img src=\"https://images.test.digitallibrary.io/eB1RNSN8.jpg\" srcset=\"https://images.test.digitallibrary.io/eB1RNSN8.jpg?width=300, https://images.test.digitallibrary.io/eB1RNSN8.jpg?width=600 2x\" alt=\"\" /></picture>\n\n<picture><source media=\"(min-width: 768px)\" srcset=\"https://images.test.digitallibrary.io/bEvj3uIw.png\" /><img src=\"https://images.test.digitallibrary.io/bEvj3uIw.png\" srcset=\"https://images.test.digitallibrary.io/bEvj3uIw.png?width=300, https://images.test.digitallibrary.io/bEvj3uIw.png?width=600 2x\" alt=\"\" /></picture>\n\n<picture><source media=\"(min-width: 768px)\" srcset=\"https://images.test.digitallibrary.io/F4kwymDa.png\" /><img src=\"https://images.test.digitallibrary.io/F4kwymDa.png\" srcset=\"https://images.test.digitallibrary.io/F4kwymDa.png?width=300, https://images.test.digitallibrary.io/F4kwymDa.png?width=600 2x\" alt=\"\" /></picture>"""
    val payload =
      s"""
        | { "id": 1,
        |   "revision": 1,
        |   "translationId": 1,
        |   "seqNo": 1,
        |   "title": "my title",
        |   "content": "$pictureContent <br /> Some content here",
        |   "chapterType": "CONTENT"
        | }
      """.stripMargin.getBytes
    val expectedContent = "<embed ...> <br /> Some content here"
    put("/eng/1/chapters/1", payload, headers = Seq(("Authorization", s"Bearer $validTestTokenWithWriteRole"))) {
      status should equal (200)
      verify(writeService).updateChapter(TestData.Domain.DefaultChapter.copy(content = expectedContent))
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
}
