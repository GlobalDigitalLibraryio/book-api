/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import no.gdl.bookapi.model.api.Error
import no.gdl.bookapi.service.{EPubService, PdfService, ReadService}
import org.scalatra.{InternalServerError, NotFound, Ok}

import scala.util.{Failure, Success}

trait DownloadController {
  this: ReadService with EPubService with PdfService =>
  val downloadController: DownloadController

  class DownloadController extends GdlController {

    private val pdfPattern = "(.+).pdf".r
    private val epubPattern = "(.+).epub".r

    get("/epub/:lang/:filename") {
      val language = params("lang")
      val filename = params("filename")

      filename match {
        case epubPattern(uuid) =>
          ePubService.createEPub(language, uuid) match {
            case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
            case Some(Success(book)) =>
              contentType = "application/octet-stream"
              book.writeToStream(response.getOutputStream)
            case Some(Failure(ex)) =>
              logger.error("Could not generate epub", ex)
              InternalServerError(body=Error.GenericError)
          }
        case _ => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
      }
    }

    get("/pdf/:lang/:filename") {
      val language = params("lang")
      val filename = params("filename")

      filename match {
        case pdfPattern(uuid) =>
          pdfService.createPdf(language, uuid) match {
            case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
            case Some(Success(pdf)) =>
              contentType = "application/octet-stream"
              pdf.createPDF(response.getOutputStream)
            case Some(Failure(ex)) =>
              logger.error("Could not generate epub", ex)
              InternalServerError(body=Error.GenericError)
          }

        case _ => NotFound(body = Error(Error.NOT_FOUND, s"No book with filename $filename found."))
      }
    }
  }
}
