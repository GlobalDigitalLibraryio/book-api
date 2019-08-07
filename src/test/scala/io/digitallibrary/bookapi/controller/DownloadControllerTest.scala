/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import java.text.SimpleDateFormat

import coza.opencollab.epub.creator.model.EpubBook
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api.{LocalDateSerializer, PdfStream}
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import org.json4s.{DefaultFormats, Formats}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class DownloadControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  } + LocalDateSerializer


  lazy val controller = new DownloadController
  addServlet(controller, "/*")

  test("that get /epub/nob/123.txt returns 404 not found") {
    get("/epub/nob/123.txt") {
      status should equal (404)
    }
  }

  test("that get /epub/nob/123.epub returns 404 not found, when 123.epub does not exist") {
    when(ePubService.createEPub(LanguageTag("nob"), "123")).thenReturn(None)
    get("/epub/nob/123.epub") {
      status should equal (404)
    }
  }

  test("that get /epub/nob/123.epub returns 500 internal server error when epub generating fails") {
    when(ePubService.createEPub(LanguageTag("nob"), "123")).thenReturn(Some(Failure(new RuntimeException("Something went wrong"))))
    get("/epub/nob/123.epub") {
      status should equal (500)
    }
  }

  test("that get /epub/nob/123.epub returns with an application/octet-stream when generating is ok") {
    val book = mock[EpubBook]
    when(book.getTitle).thenReturn(TestData.Api.DefaultBook.title)
    when(ePubService.createEPub(LanguageTag("nob"), "123")).thenReturn(Some(Success(book)))
    get("/epub/nob/123.epub") {
      status should equal (200)
      header.get("Content-Type") should equal (Some("application/octet-stream;charset=utf-8"))
    }
  }

  test("that get /pdf/nob/123.txt returns 404 not found") {
    get("/pdf/nob/123.txt") {
      status should equal (404)
    }
  }

  test("that get /pdf/nob/123.pdf returns 200 ok") {
    val pdfStream = mock[PdfStream]
    when(pdfStream.fileName).thenReturn(TestData.ApiV2.DefaultBook.title)
    when(pdfServiceV2.getPdf(LanguageTag("nob"), "123")).thenReturn(Some(pdfStream))
    get("/pdf/nob/123.pdf") {
      status should equal (200)
      header.get("Content-Type") should equal (Some("application/octet-stream;charset=utf-8"))
    }
  }

  test("that get /pdf/nob/123.pdf returns 404 when no content found") {
    when(pdfServiceV2.getPdf(LanguageTag("nob"), "123")).thenReturn(None)
    get("/pdf/nob/123.pdf") {
      status should equal (404)
    }
  }
}
