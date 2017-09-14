/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.controller

import no.gdl.bookapi.model.api.Error
import no.gdl.bookapi.model.api.internal.{NewBook, NewChapter, NewTranslation}
import no.gdl.bookapi.service.{ConverterService, ReadService, ValidationService, WriteService}
import org.scalatra.NotFound

import scala.util.{Failure, Success}

trait InternController {
  this: WriteService with ReadService with ConverterService with ValidationService =>
  val internController: InternController

  class InternController extends GdlController {
    post("/book/") {
      val newBook = extract[NewBook](request.body)

      // TODO: Update book
      writeService.newBook(newBook) match {
        case Success(x) => x
        case Failure(ex) => throw ex
      }
    }

    post("/book/:id/translation") {
      val bookId = long("id")
      val newTranslation = extract[NewTranslation](request.body)

      // TODO: Update if same ID
      writeService.newTranslationForBook(bookId, newTranslation) match {
        case Success(x) => x
        case Failure(ex) => throw ex
      }
    }

    post("/book/:bookid/translation/:translationid/chapters") {
      val bookId = long("bookid")
      val translationId = long("translationid")
      val newChapter = extract[NewChapter](request.body)

      // TODO: update chapter
      writeService.newChapter(translationId, newChapter) match {
        case Success(x) => x
        case Failure(ex) => throw ex
      }
    }

    get("/:externalId") {
      val externalId = params.get("externalId")

      readService.withExternalId(externalId) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $externalId found"))
      }
    }
  }
}
