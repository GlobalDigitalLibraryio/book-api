/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.model.api.Error
import io.digitallibrary.bookapi.service._
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.{InternalServerError, NotFound}
import org.scalatra.util.UrlCodingUtils

import scala.util.{Failure, Success}

trait DownloadControllerV2 {
  this: ReadServiceV2 with EPubService with PdfServiceV2  =>
  val downloadControllerV2: DownloadControllerV2

  class DownloadControllerV2 extends GdlController {

    private val pdfPattern = "(.+).pdf".r
    private val epubPattern = "(.+).epub".r

    get("/epub/:lang/:filename") {
      val language = LanguageTag(params("lang"))
      val filename = params("filename")

      filename match {
        case epubPattern(uuid) =>
          ePubService.createEPub(language, uuid) match {
            case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
            case Some(Success(book)) =>
              contentType = "application/octet-stream"
              response.setHeader("Content-Disposition", s"""attachment; filename="${UrlCodingUtils.urlEncode(book.getTitle)}.epub" """)
              book.writeToStream(response.getOutputStream)
            case Some(Failure(ex)) =>
              logger.error("Could not generate epub", ex)
              InternalServerError(body=Error.GenericError)
          }
        case _ => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
      }
    }

    get("/pdf/:lang/:filename") {
      val language = LanguageTag(params("lang"))
      val filename = params("filename")

      filename match {
        case pdfPattern(uuid) =>
          pdfServiceV2.getPdf(language, uuid) match {
            case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
            case Some(pdf) =>
              contentType = "application/octet-stream"
              response.setHeader("Content-Disposition", s"""attachment; filename="${UrlCodingUtils.urlEncode(pdf.fileName)}" """)
              pdf.toOutputStream(response.getOutputStream)
          }
        case _ => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
      }
    }
  }
}
