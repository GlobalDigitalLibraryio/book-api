/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api.Error
import io.digitallibrary.bookapi.service.{EPubService, PdfService, ReadService}
import org.scalatra.util.UrlCodingUtils
import org.scalatra.{InternalServerError, NotFound}

import scala.util.{Failure, Success}

trait DownloadController {
  this: ReadService with EPubService with PdfService  =>
  val downloadController: DownloadController

  class DownloadController extends GdlController {

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
          pdfService.getPdf(language, uuid) match {
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
