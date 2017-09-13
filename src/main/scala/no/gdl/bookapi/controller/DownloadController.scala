/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import no.gdl.bookapi.service.ReadService
import org.scalatra.Ok

trait DownloadController {
  this: ReadService =>
  val downloadController: DownloadController

  class DownloadController extends GdlController {

    private val pdfPattern = "(.+).pdf".r
    private val epubPattern = "(.+).epub".r

    get("/epub/:lang/:filename") {
      val language = params("lang")
      val filename = params("filename")

      filename match {
        case epubPattern(uuid) => Ok(body = s"Here you soon will find $uuid.epub in $language")
        case _ => halt(status = 400, body = "Invalid input")
      }
    }

    get("/pdf/:lang/:filename") {
      val language = params("lang")
      val filename = params("filename")

      filename match {
        case pdfPattern(uuid) => Ok(body = s"Here you soon will find $uuid.pdf in $language")
        case _ => halt(status = 400, body = "Invalid input")
      }
    }
  }
}
