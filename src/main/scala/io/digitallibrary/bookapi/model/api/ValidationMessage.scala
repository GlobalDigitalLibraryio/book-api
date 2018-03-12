package io.digitallibrary.bookapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "A message describing a validation error on a specific field")
case class ValidationMessage(@(ApiModelProperty@field)(description = "The field the error occurred in") field: String,
                             @(ApiModelProperty@field)(description = "The validation message") message: String)
