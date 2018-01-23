/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.model.api

import java.util.Date

import io.digitallibrary.language.model.LanguageTag
import io.searchbox.client.JestResult
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model._
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.GENERIC,
                 @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
                 @(ApiModelProperty@field)(description = "When the error occurred") occurredAt: Date = new Date())

@ApiModel(description = "Information about validation errors")
case class ValidationError(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.VALIDATION,
                           @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.VALIDATION_DESCRIPTION,
                           @(ApiModelProperty@field)(description = "List of validation messages") messages: Seq[ValidationMessage],
                           @(ApiModelProperty@field)(description = "When the error occurred") occurredAt: Date = new Date())

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val VALIDATION = "VALIDATION"
  val INDEX_MISSING = "INDEX_MISSING"
  val RESOURCE_OUTDATED = "RESOURCE_OUTDATED"
  val ACCESS_DENIED = "ACCESS DENIED"
  val ALREADY_EXISTS = "ALREADY EXISTS"
  val TRANSLATE_ERROR = "TRANSLATE ERROR"

  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${BookApiProperties.ContactEmail} if the error persists."
  val VALIDATION_DESCRIPTION = "Validation Error"
  val INDEX_MISSING_DESCRIPTION = s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${BookApiProperties.ContactEmail} if the error persists."
  val RESOURCE_OUTDATED_DESCRIPTION = "The resource is outdated. Please try fetching before submitting again."
  val TRANSLATE_DESCRIPTION = "Communication with the translation system failed. Please try again."

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
  val IndexMissingError = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
  val TranslationError = Error(TRANSLATE_ERROR, TRANSLATE_DESCRIPTION)
}

class NotFoundException(message: String = "The book was not found") extends RuntimeException(message)
class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)
class OptimisticLockException(message: String = Error.RESOURCE_OUTDATED_DESCRIPTION) extends RuntimeException(message)
class GdlSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}
class SourceLanguageNotSupportedException(sourceLanguage: LanguageTag) extends RuntimeException(s"The source language '${sourceLanguage.toString}' is not currently supported.")

class CrowdinException(message: String,
                       errors: Seq[crowdin.Error],
                       causes: Seq[Throwable]) extends RuntimeException(message) {

  def this(causes: Seq[Throwable]) = this("Received Crowdin Exceptions", Seq(), causes)
  def getErrors: Seq[crowdin.Error] = errors
  def getCauses: Seq[Throwable] = causes
}

object CrowdinException {
  def apply(errors: Seq[crowdin.Error], causes: Seq[Throwable]): CrowdinException =
    new CrowdinException("Received exceptions and errors from Crowdin", errors, causes)

  def apply(errors: Seq[crowdin.Error]): CrowdinException = new CrowdinException("Received Crowdin Errors", errors, Seq())
  def apply(error: crowdin.Error): CrowdinException = apply(Seq(error))

  def apply(cause: Throwable): CrowdinException = new CrowdinException(Seq(cause))
  def apply(message: String): CrowdinException = new CrowdinException(message, Seq(), Seq())
}

class DBException(message: String,
                  causes: Seq[Throwable]) extends RuntimeException(message) {
  def this(message: String, cause: Throwable) = this(message, Seq(cause))
  def this(message: String) = this(message, Seq())
  def this(causes: Seq[Throwable]) = this("DBException", causes)
  def this(cause: Throwable) = this(Seq(cause))

  def getCauses: Seq[Throwable] = causes
}
