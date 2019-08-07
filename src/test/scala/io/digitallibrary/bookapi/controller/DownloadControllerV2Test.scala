package io.digitallibrary.bookapi.controller

import java.text.SimpleDateFormat

import coza.opencollab.epub.creator.model.EpubBook
import io.digitallibrary.bookapi.{TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.model.api.PdfStream
import io.digitallibrary.language.model.LanguageTag
import org.json4s.{DefaultFormats, Formats}
import org.mockito.Mockito.when
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class DownloadControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  }


  lazy val controllerV2 = new DownloadControllerV2
  addServlet(controllerV2, "/*")

  test("v2: that get /epub/nob/123.txt returns 404 not found") {
    get("/epub/nob/123.txt") {
      status should equal (404)
    }
  }

  test("v2: that get /epub/nob/123.epub returns 404 not found, when 123.epub does not exist") {
    when(ePubService.createEPub(LanguageTag("nob"), "123")).thenReturn(None)
    get("/epub/nob/123.epub") {
      status should equal (404)
    }
  }

  test("v2: that get /epub/nob/123.epub returns 500 internal server error when epub generating fails") {
    when(ePubService.createEPub(LanguageTag("nob"), "123")).thenReturn(Some(Failure(new RuntimeException("Something went wrong"))))
    get("/epub/nob/123.epub") {
      status should equal (500)
    }
  }

  test("v2: that get /epub/nob/123.epub returns with an application/octet-stream when generating is ok") {
    val book = mock[EpubBook]
    when(book.getTitle).thenReturn(TestData.ApiV2.DefaultBook.title)
    when(ePubService.createEPub(LanguageTag("nob"), "123")).thenReturn(Some(Success(book)))
    get("/epub/nob/123.epub") {
      status should equal (200)
      header.get("Content-Type") should equal (Some("application/octet-stream;charset=utf-8"))
    }
  }

  test("v2: that get /pdf/nob/123.txt returns 404 not found") {
    get("/pdf/nob/123.txt") {
      status should equal (404)
    }
  }

  test("v2: that get /pdf/nob/123.pdf returns 200 ok") {
    val pdfStream = mock[PdfStream]
    when(pdfStream.fileName).thenReturn(TestData.ApiV2.DefaultBook.title)
    when(pdfServiceV2.getPdf(LanguageTag("nob"), "123")).thenReturn(Some(pdfStream))
    get("/pdf/nob/123.pdf") {
      status should equal (200)
      header.get("Content-Type") should equal (Some("application/octet-stream;charset=utf-8"))
    }
  }

  test("v2: that get /pdf/nob/123.pdf returns 404 when no content found") {
    when(pdfServiceV2.getPdf(LanguageTag("nob"), "123")).thenReturn(None)
    get("/pdf/nob/123.pdf") {
      status should equal (404)
    }
  }
}

