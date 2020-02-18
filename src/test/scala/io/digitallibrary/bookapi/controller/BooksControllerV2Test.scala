/*
 * Part of GDL book_api.
 * Copyright (C) 2019 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import java.text.SimpleDateFormat

import io.digitallibrary.bookapi._
import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.domain
import io.digitallibrary.bookapi.model.domain.{PageOrientation, Paging, PublishingStatus, Sort}
import io.digitallibrary.language.model.LanguageTag
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success

class BooksControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite {

    implicit val formats: Formats = new DefaultFormats {
        override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    } + LocalDateSerializer

    implicit val swagger: BookSwagger = new BookSwagger

    lazy val controller = new BooksControllerV2

    addServlet(controller, "/*")


    test("v2: that GET / will get books with default language") {
        val result = SearchResultV2(0, 1, 10, Some(Language("eng", "English")), Seq(TestData.ApiV2.DefaultBookHit))
        when(searchServiceV2.searchWithCategoryAndLevel(languageTag = LanguageTag(BookApiProperties.DefaultLanguage), category = None, readingLevel = Some("1"), source = None, paging = Paging(1, 10), sort = Sort.ByIdAsc)).thenReturn(result)

        get("/?reading-level=1&page-size=10&page=1") {
            status should equal (200)
            val searchResult = read[SearchResultV2](body)
            searchResult.results.length should be (1)
            searchResult.results.head.language.code should equal(TestData.DefaultLanguage)
        }
    }

    test("v2: that GET /:lang will get books with language") {
        val language = TestData.Api.norwegian_bokmal

        val result = SearchResultV2(0, 1, 10, Some(language), Seq(TestData.ApiV2.DefaultBookHit))
        when(searchServiceV2.searchWithCategoryAndLevel(languageTag = LanguageTag(language.code), category = None, readingLevel = Some("2"), source = None, paging = Paging(1, 10), sort = Sort.ByIdAsc)).thenReturn(result)

        get("/nob?reading-level=2&page-size=10&page=1") {
            status should equal (200)
            val searchResult = read[SearchResultV2](body)
            searchResult.results.length should be (1)
            searchResult.results.head.language.code should equal(TestData.DefaultLanguage)
        }
    }

    test("v2: that GET /:lang will get books with language sorted by title descending") {
        val language = TestData.Api.norwegian_bokmal

        val firstBook = TestData.ApiV2.DefaultBookHit.copy(id = 2, title = "This should be first")
        val secondBook = TestData.ApiV2.DefaultBookHit.copy(id = 1, title = "This should be last")

        val result = SearchResultV2(2, 1, 10, Some(language), Seq(firstBook, secondBook))
        when(searchServiceV2.searchWithCategoryAndLevel(languageTag = LanguageTag(language.code), category = None, readingLevel = Some("2"), source = None, paging = Paging(1, 10), sort = Sort.ByTitleDesc)).thenReturn(result)

        get("/nob?reading-level=2&page-size=10&page=1&sort=-title") {
            status should equal (200)
            val searchResult = read[SearchResultV2](body)
            searchResult.results.length should be (2)
            searchResult.results should equal (Seq(firstBook, secondBook))
        }
    }

    test("v2: that GET /:lang/:id returns 404 when not found") {
        when(readServiceV2.withIdAndLanguageListingAllBooksIfAdmin(any[Long], any[LanguageTag])).thenReturn(None)

        get("/eng/1") {
            status should equal (404)
        }
    }

    test("v2: that GET /:lang/:id returns book when found") {
        when(readServiceV2.withIdAndLanguageListingAllBooksIfAdmin(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))

        get("/eng/1") {
            status should equal (200)
            val book = read[BookV2](body)
            book.uuid should equal (TestData.ApiV2.DefaultBook.uuid)
        }
    }

    test("v2: that PUT /:lang/:id updates book, with only certain fields modified") {
        when(readServiceV2.translationWithIdAndLanguageListingAllTranslationsIfAdmin(any[Long], any[LanguageTag])).thenReturn(Some(TestData.Domain.DefaultTranslation))
        when(readServiceV2.withIdAndLanguage(any[Long], any[LanguageTag])).thenReturn(Some(TestData.ApiV2.DefaultBook))
        when(writeServiceV2.updateTranslation(any[domain.Translation])).thenReturn(Success(TestData.Domain.DefaultTranslation))

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
                        |		"type": "IMAGE",
                        |   "id": "1"
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
                        | "pageOrientation": "LANDSCAPE",
                        | "publishingStatus": "FLAGGED",
                        | "bookType": "BOOK"
                        |}""".stripMargin.getBytes

        put("/eng/1", payload, headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithWriteRole}"))) {
            status should equal (200)
            // TODO: update this to verify coverphoto?
            verify(writeServiceV2).updateTranslation(TestData.Domain.DefaultTranslation.copy(title = "new title", about = "new description", pageOrientation = PageOrientation.LANDSCAPE, publishingStatus = PublishingStatus.FLAGGED))
        }
    }
}
