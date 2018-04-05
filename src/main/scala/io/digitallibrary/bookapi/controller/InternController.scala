/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package io.digitallibrary.bookapi.controller

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api.internal.{Book, NewBook, NewChapter, NewTranslation}
import io.digitallibrary.bookapi.model.api.{Error, ValidationException, ValidationMessage}
import io.digitallibrary.bookapi.service._
import io.digitallibrary.bookapi.service.search.{IndexBuilderService, IndexService}
import org.scalatra.servlet.FileUploadSupport
import org.scalatra.{Conflict, InternalServerError, NotFound, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: WriteService with ReadService with ConverterService with ValidationService with FeedService with PdfService with IndexBuilderService with IndexService with ImportService =>
  val internController: InternController

  class InternController extends GdlController with FileUploadSupport{
    post("/book/") {
      val newBook = extract[NewBook](request.body)

      // TODO: Update book
      writeService.newBook(newBook) match {
        case Success(x) => x
        case Failure(ex) => throw ex
      }
    }

    get("/book/:lang/:id") {
      val language = LanguageTag(params("lang"))
      val bookId = long("id")

      readService.withIdAndLanguageForExport(bookId, language) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No book with id $bookId and language $language found"))
      }
    }

    post("/import/book/") {
      val newBook = extract[Book](request.body)
      importService.importBook(newBook)
    }

    post("/import/translation/:book_id/") {
      val newBook = extract[Book](request.body)
      importService.importBookAsTranslation(newBook, long("book_id"))
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
      val language = LanguageTag(newTranslation.language)

      readService.withIdAndLanguage(bookId, language) match {
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

    post("/translation/:translationid/pdf") {
      val translationId = long("translationid")
      val file = fileParams.getOrElse("file", throw new ValidationException(errors = Seq(ValidationMessage("file", "The request must contain a book file"))))

      readService.uuidWithTranslationId(Some(translationId)) match {
        case Some(uuid) => pdfService.uploadFromStream(file.getInputStream, uuid.uuid, file.contentType.get, file.size)
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No translation with id $translationId found"))
      }
    }

    get("/feeds/") {
      feedService.generateFeeds()
    }

    post("/index") {
      indexBuilderService.indexDocuments match {
        case Success(reindexResult) => {
          val result = s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    delete("/index") {
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"
      val deleteResults = indexService.findAllIndexes() match {
        case Failure(f) => halt(status = 500, body = f.getMessage)
        case Success(indexes) => indexes.map(index => {
          logger.info(s"Deleting index $index")
          indexService.deleteSearchIndex(Option(index))
        })
      }
      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
      }
    }

  }
}
