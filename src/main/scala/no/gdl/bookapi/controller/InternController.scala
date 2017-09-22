/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.controller

import no.gdl.bookapi.model.api.Error
import no.gdl.bookapi.model.api.internal.{NewBook, NewChapter, NewTranslation, TranslationId}
import no.gdl.bookapi.service.{ConverterService, ReadService, ValidationService, WriteService}
import org.scalatra.{Conflict, NotFound, Ok}

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

    put("/book/:id") {
      val bookId = long("id")
      val bookReplacement = extract[NewBook](request.body)
      writeService.updateBook(bookId, bookReplacement) match {
        case Success(x) => x
        case Failure(ex) => throw ex
      }
    }

    post("/book/:id/translation") {
      val bookId = long("id")
      val newTranslation = extract[NewTranslation](request.body)

      readService.withIdAndLanguage(bookId, newTranslation.language) match {
        case Some(_) => Conflict(body = Error(Error.ALREADY_EXISTS, s"A translation with language '${newTranslation.language}' already exists for book with id '$bookId'. Updating is not supported yet"))
        case None => writeService.newTranslationForBook(bookId, newTranslation) match {
          case Success(x) => x
          case Failure(ex) => throw ex
        }
      }
    }

    put("/book/:bookid/translation/:translationid") {
      val bookId = long("bookid")
      val translationId = long("translationid")
      val translationReplacement = extract[NewTranslation](request.body)

      writeService.updateTranslationForBook(bookId, translationId, translationReplacement) match {
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No translation with id $translationId for book with id $bookId found"))
        case Some(Success(x)) => x
        case Some(Failure(ex)) => throw ex
      }
    }

    post("/book/:bookid/translation/:translationid/chapters") {
      val bookId = long("bookid")
      val translationId = long("translationid")
      val newChapter = extract[NewChapter](request.body)

      writeService.newChapter(translationId, newChapter) match {
        case Success(x) => x
        case Failure(ex) => throw ex
      }
    }

    get("/book/:bookid/translation/:translationid/chapters/:seqno") {
      val bookId = long("bookid")
      val translationId = long("translationid")
      val seqno = long("seqno")

      readService.chapterWithSeqNoForTranslation(translationId, seqno) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No chapter with seq_no $seqno for translation with id $translationId found"))
      }
    }

    put("/book/:bookid/translation/:translationid/chapters/:chapterid") {
      val bookId = long("bookid")
      val translationId = long("translationid")
      val chapterid = long("chapterid")
      val replacementChapter = extract[NewChapter](request.body)

      writeService.updateChapter(chapterid, replacementChapter) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No chapter with id $chapterid for translation with id $translationId found"))
      }
    }

    get("/:externalId") {
      val externalId = params.get("externalId")

      readService.withExternalId(externalId) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $externalId found"))
      }
    }

    get("/book/:bookid/translation/:externalid") {
      val bookId = long("bookid")
      val externalId = params("externalid")

      readService.translationWithExternalId(Some(externalId)) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No translation for book $bookId with external_id $externalId found"))
      }
    }


  }
}
