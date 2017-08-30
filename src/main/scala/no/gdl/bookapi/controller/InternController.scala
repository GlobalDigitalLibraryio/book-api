/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.controller

import no.gdl.bookapi.BookApiProperties.DefaultLanguage
import no.gdl.bookapi.model.api.{Error, NewBook, NewBookInLanguage}
import no.gdl.bookapi.service.{ConverterService, ReadService, WriteService}
import org.scalatra.NotFound

trait InternController {
  this: WriteService with ReadService with ConverterService =>
  val internController: InternController

  class InternController extends GdlController {
    post("/new") {
      val newBook = extract[NewBook](request.body)

      logger.info(s"NEW ${newBook.externalId} --> ${newBook.title}")
      readService.withExternalId(newBook.externalId) match {
        case Some(existing) => writeService.updateBook(existing, newBook)
        case None => writeService.newBook(newBook).get
      }
    }

    post("/:externalId/languages/") {
      val externalId = params("externalId")
      val newTranslation = extract[NewBookInLanguage](request.body)
      logger.info(s"LANG $externalId --> ${newTranslation.title}")

      readService.bookInLanguageWithExternalId(newTranslation.externalId) match {
        case Some(existing) => writeService.updateBookInLanguage(existing, newTranslation)
        case None => {
          readService.withExternalId(Some(externalId)) match {
            case Some(existing) => writeService.newBookInLanguage(existing.id.get, newTranslation).get
            case None => throw new RuntimeException(s"No book with external_id = $externalId")
          }
        }
      }
    }

    get("/:externalId") {
      val externalId = params.get("externalId")
      val language = paramOrDefault("language", DefaultLanguage)

      readService.withExternalId(externalId).flatMap(c => converterService.toApiBook(c, language)) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $externalId found"))
      }
    }
  }
}
