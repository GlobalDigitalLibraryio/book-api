/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.model.api.{ValidationException, ValidationMessage}
import no.gdl.bookapi.model._
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

import scala.util.{Failure, Success, Try}


trait ValidationService {
  val validationService: ValidationService

  class ValidationService extends LazyLogging {
    def validateLicense(license: Option[domain.License]): Try[domain.License] = {
      license match {
        case None => Failure(new ValidationException(errors = Seq(ValidationMessage("license", "Invalid license"))))
        case Some(x) => Success(x)
      }
    }

    def validatePublisher(publisher: Option[domain.Publisher]): Try[domain.Publisher] = {
      publisher match {
        case None => Failure(new ValidationException(errors = Seq(ValidationMessage("publisher", "Publisher must be defined"))))
        case Some(x) if x.name.isEmpty => Failure(new ValidationException(errors = Seq(ValidationMessage("publisher", "Publisher must be defined"))))
        case Some(y) => Success(y)
      }
    }

    def validateChapter(chapter: domain.Chapter): Try[domain.Chapter] = {
      val validTitle = chapter.title.flatMap(title => containsNoHtml("title", title))
      val validContent = if (chapter.content.isEmpty) Some(ValidationMessage("content", "Missing content")) else None
      val validSeqNo = if (chapter.seqNo < 1) Some(ValidationMessage("seqNo", "Invalid sequence number")) else None

      validTitle.toSeq ++ validContent ++ validSeqNo match {
        case head :: tail => Failure(new ValidationException(errors = head :: tail))
        case _ => Success(chapter)
      }
    }

    private def containsNoHtml(fieldPath: String, text: String): Option[ValidationMessage] = {
      if (Jsoup.isValid(text, Whitelist.none())) {
        None
      } else {
        Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed"))
      }
    }
  }
}
