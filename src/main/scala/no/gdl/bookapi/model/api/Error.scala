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

  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occurred. We have logged the error, and will look into it. But feel free to contact ${BookApiProperties.ContactEmail} if the error persists."
  val VALIDATION_DESCRIPTION = "Validation Error"
  val INDEX_MISSING_DESCRIPTION = s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${BookApiProperties.ContactEmail} if the error persists."
  val RESOURCE_OUTDATED_DESCRIPTION = "The resource is outdated. Please try fetching before submitting again."

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
  val IndexMissingError = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)
}

class NotFoundException(message: String = "The book was not found") extends RuntimeException(message)
class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage]) extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)
class OptimisticLockException(message: String = Error.RESOURCE_OUTDATED_DESCRIPTION) extends RuntimeException(message)
class GdlSearchException(jestResponse: JestResult) extends RuntimeException(jestResponse.getErrorMessage) {
  def getResponse: JestResult = jestResponse
}
class SourceLanguageNotSupportedException(sourceLanguage: LanguageTag) extends RuntimeException(s"The source language '${sourceLanguage.toString}' is not currently supported.")
class CrowdinException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(code: Long, message: String, cause: Throwable = null) = {
    this(s"Crowdin Error - Code: $code, Message: $message", cause)
  }

  def this(cause: Throwable) = {
    this(cause.toString, cause)
  }
}

class DBException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(cause: Throwable) = {
    this(cause.toString, cause)
  }
}