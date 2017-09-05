/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about the licensing of the book")
case class License(@(ApiModelProperty@field)(description = "The name of the license") license: String,
                   @(ApiModelProperty@field)(description = "Description of the license") description: Option[String],
                   @(ApiModelProperty@field)(description = "Url to where the license can be found") url: Option[String])

@ApiModel(description = "Information about the current and available language for book")
case class Language(@(ApiModelProperty@field)(description = "Current language") current: String,
                    @(ApiModelProperty@field)(description = "List of available languages") available: Seq[String])

@ApiModel(description = "Information about where to find the cover-photo for the book")
case class CoverPhoto(@(ApiModelProperty@field)(description = "URL to a large version of the cover-photo") large: String,
                      @(ApiModelProperty@field)(description = "URL to a small version of the cover-photo") small: String)

@ApiModel(description = "Information about where to download a copy of the book")
case class Downloads(@(ApiModelProperty@field)(description = "URL to an epub-download") epub: String,
                     @(ApiModelProperty@field)(description = "URL to a pdf-download") pdf: String)

@ApiModel(description = "Information about the authors of the current book")
case class Contributor(@(ApiModelProperty@field)(description = "Id of the author") id: Long,
                       @(ApiModelProperty@field)(description = "The type of the contributor") `type`: String,
                       @(ApiModelProperty@field)(description = "Name of the author") name: String)

@ApiModel(description = "Information about the publisher of the current book")
case class Publisher(@(ApiModelProperty@field)(description = "Id of the publisher") id: Long,
                     @(ApiModelProperty@field)(description = "Name of the publisher") name: String)

@ApiModel(description = "Information about a category of a current book")
case class Category(@(ApiModelProperty@field)(description = "Id for the category") id: Long,
                    @(ApiModelProperty@field)(description = "Name of the category") name: String)

@ApiModel(description = "Information about a chapter in a book")
case class ChapterSummary(@(ApiModelProperty@field)(description = "Id for the chapter") id: Long,
                          @(ApiModelProperty@field)(description = "Title of the chapter") title: Option[String],
                          @(ApiModelProperty@field)(description = "URL to where chapter can be found") url: String)

@ApiModel(description = "Information about book")
case class Book(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
                @(ApiModelProperty@field)(description = "The revision of the book") revision: Long,
                @(ApiModelProperty@field)(description = "User determined identifier") externalId: Option[String],
                @(ApiModelProperty@field)(description = "A UUID for this book") uuid: String,
                @(ApiModelProperty@field)(description = "The title of the book") title: String,
                @(ApiModelProperty@field)(description = "Description of the book") description: String,
                @(ApiModelProperty@field)(description = "Current language") language: String,
                @(ApiModelProperty@field)(description = "Languages available") availableLanguages: Seq[String],
                @(ApiModelProperty@field)(description = "Licensing information") license: License,
                @(ApiModelProperty@field)(description = "Information about publisher") publisher: Publisher,
                @(ApiModelProperty@field)(description = "Information about reading level") readingLevel: Option[String],
                @(ApiModelProperty@field)(description = "Information about the typical age range of the reader") typicalAgeRange: Option[String],
                @(ApiModelProperty@field)(description = "Information about which educational use the book is for") educationalUse: Option[String],
                @(ApiModelProperty@field)(description = "For which role is the book intended") educationalRole: Option[String],
                @(ApiModelProperty@field)(description = "The time required to read through the book. (e.g. PT10M)") timeRequired: Option[String],
                @(ApiModelProperty@field)(description = "The date when this book was first published (iso-format)") datePublished: Option[Date],
                @(ApiModelProperty@field)(description = "The date when this book was created (iso-format)") dateCreated: Option[Date],
                @(ApiModelProperty@field)(description = "Information about categories") categories: Seq[Category],
                @(ApiModelProperty@field)(description = "Cover Photo information") coverPhoto: CoverPhoto,
                @(ApiModelProperty@field)(description = "Information about downloads") downloads: Downloads,
                @(ApiModelProperty@field)(description = "Keywords for the book") tags: Seq[String],
                @(ApiModelProperty@field)(description = "Information about the contributors of this book") contributors: Seq[Contributor],
                @(ApiModelProperty@field)(description = "Information about the chapters in the book") chapters: Seq[ChapterSummary])