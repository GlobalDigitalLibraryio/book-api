/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.model._
import io.digitallibrary.bookapi.model.api.internal.NewTranslation
import io.digitallibrary.bookapi.model.api.{ValidationException, ValidationMessage}
import io.digitallibrary.bookapi.model.domain.{ChapterType, ContributorType}
import org.apache.commons.validator.routines.UrlValidator
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

import scala.util.{Failure, Success, Try}


trait ValidationService {
  val validationService: ValidationService

  class ValidationService extends LazyLogging {
    def validateNewTranslation(newTranslation: NewTranslation): Try[NewTranslation] = {
      collectValidations(newTranslation, newTranslation.contributors.map(x => validContributorType(x.`type`)))
    }

    def validContributorType(contributorType: String): Option[ValidationMessage] = {
      asValidation("contributor.type", s"The contributor.type is not valid. Value must be one of ${ContributorType.values}", ContributorType.valueOf(contributorType).isSuccess)
    }

    def validateFeaturedContent(content: domain.FeaturedContent): Try[domain.FeaturedContent] = {
      collectValidations(content, Seq(
        nonEmpty("title", content.title),
        nonEmpty("description", content.description),
        nonEmpty("link", content.link),
        nonEmpty("imageUrl", content.imageUrl),
        containsNoHtml("title", content.title),
        containsNoHtml("description", content.description),
        containsNoHtml("link", content.link),
        containsNoHtml("imageUrl", content.imageUrl),
        validUrl("link", content.link),
        validUrl("imageUrl", content.imageUrl)
      ))
    }

    def validateUpdatedFeaturedContent(content: api.FeaturedContent): Try[api.FeaturedContent] = {
      collectValidations(content, Seq(
        nonEmpty("title", content.title),
        nonEmpty("description", content.description),
        nonEmpty("link", content.link),
        nonEmpty("imageUrl", content.imageUrl),
        containsNoHtml("title", content.title),
        containsNoHtml("description", content.description),
        containsNoHtml("link", content.link),
        containsNoHtml("imageUrl", content.imageUrl),
        validUrl("link", content.link),
        validUrl("imageUrl", content.imageUrl)
      ))
    }

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

    private def nonEmpty(fieldPath: String, text: String): Option[ValidationMessage] = {
      asValidation(fieldPath, "The content must be non-empty", text.nonEmpty)
    }

    private def validUrl(fieldPath: String, url: String): Option[ValidationMessage] = {
      asValidation(fieldPath, "The content is not a valid URL", new UrlValidator().isValid(url))
    }

    private def containsNoHtml(fieldPath: String, text: String): Option[ValidationMessage] = {
      asValidation(fieldPath, "The content contains illegal html-characters. No HTML is allowed",
        Jsoup.isValid(text, Whitelist.none()))
    }

    private def asValidation(fieldPath: String, errorMessage: String, result: Boolean): Option[ValidationMessage] = {
      if (result) {
        None
      } else {
        Some(ValidationMessage(fieldPath, errorMessage))
      }
    }

    private def collectValidations[A](input: A, validations: Seq[Option[ValidationMessage]]): Try[A] = {
      validations.flatten match {
        case Nil => Success(input)
        case errors => Failure(new ValidationException(errors = errors))
      }
    }
  }

}
