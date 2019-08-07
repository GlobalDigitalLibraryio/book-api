/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.api

import java.time.{LocalDate, ZonedDateTime}

import io.digitallibrary.bookapi.model.domain.PublishingStatus.Value
import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JNull, JString}
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field
import scala.util.{Failure, Success, Try}

@ApiModel(description = "Information about where to find the cover image for the book")
case class CoverImageV2(@(ApiModelProperty@field)(description = "URL to the details about the cover-image") url: String,
                      @(ApiModelProperty@field)(description = "Always set to 'image'") `type`: String,
                      @(ApiModelProperty@field)(description = "Image id of the cover-image") imageId: String)


@ApiModel(description = "Information about about a media element")
case class Media(@(ApiModelProperty@field)(description = "URL to the details about the media") url: String,
                        @(ApiModelProperty@field)(description = "Always set to 'image', 'audio' or 'video'") `type`: String,
                        @(ApiModelProperty@field)(description = "Image id of the media") id: String)

@ApiModel(description = "Information about a chapter in a book")
case class ChapterV2(@(ApiModelProperty@field)(description = "Id for the chapter") id: Long,
                   @(ApiModelProperty@field)(description = "The revision of the chapter") revision: Int,
                   @(ApiModelProperty@field)(description = "The sequence number of the chapter") seqNo: Int,
                   @(ApiModelProperty@field)(description = "Title of the chapter") title: Option[String],
                   @(ApiModelProperty@field)(description = "The HTML content of the chapter") content: String,
                   @(ApiModelProperty@field)(description = "Indicates type of chapter. One of Content, License, Cover, BackCover") chapterType: String,
                   @(ApiModelProperty@field)(description = "List of all images referenced in chapter") media: Seq[Media])

@ApiModel(description = "Information about book")
case class BookV2(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
                @(ApiModelProperty@field)(description = "The revision of the book") revision: Long,
                @(ApiModelProperty@field)(description = "User determined identifier") externalId: Option[String],
                @(ApiModelProperty@field)(description = "A UUID for this book") uuid: String,
                @(ApiModelProperty@field)(description = "The title of the book") title: String,
                @(ApiModelProperty@field)(description = "Description of the book") description: String,
                @(ApiModelProperty@field)(description = "Indicates the language this book has been translated from") translatedFrom: Option[Language],
                @(ApiModelProperty@field)(description = "Current language") language: Language,
                @(ApiModelProperty@field)(description = "Languages available") availableLanguages: Seq[Language],
                @(ApiModelProperty@field)(description = "Licensing information") license: License,
                @(ApiModelProperty@field)(description = "Information about publisher") publisher: Publisher,
                @(ApiModelProperty@field)(description = "Information about reading level") readingLevel: Option[String],
                @(ApiModelProperty@field)(description = "Information about the typical age range of the reader") typicalAgeRange: Option[String],
                @(ApiModelProperty@field)(description = "Information about which educational use the book is for") educationalUse: Option[String],
                @(ApiModelProperty@field)(description = "For which role is the book intended") educationalRole: Option[String],
                @(ApiModelProperty@field)(description = "The time required to read through the book. (e.g. PT10M)") timeRequired: Option[String],
                @(ApiModelProperty@field)(description = "The date when this book was first published (iso-format)") datePublished: Option[LocalDate],
                @(ApiModelProperty@field)(description = "The date when this book was created (iso-format)") dateCreated: Option[LocalDate],
                @(ApiModelProperty@field)(description = "The date when this book arrived to the Global Digital Library (iso-format)") dateArrived: LocalDate,
                @(ApiModelProperty@field)(description = "Information about categories") categories: Seq[Category],
                @(ApiModelProperty@field)(description = "Cover image information") coverImage: Option[CoverImageV2],
                @(ApiModelProperty@field)(description = "Information about downloads") downloads: Downloads,
                @(ApiModelProperty@field)(description = "Keywords for the book") tags: Seq[String],
                @(ApiModelProperty@field)(description = "Information about the contributors of this book") contributors: Seq[Contributor],
                @(ApiModelProperty@field)(description = "Information about the chapters in the book") chapters: Seq[ChapterSummary],
                @(ApiModelProperty@field)(description = "Indicates if this book can be translated or not") supportsTranslation: Boolean,
                @(ApiModelProperty@field)(description = "The format of the book. PDF or HTML") bookFormat: String,
                @(ApiModelProperty@field)(description = "Either portrait or landscape") pageOrientation: String,
                @(ApiModelProperty@field)(description = "The source of the book") source: String,
                @(ApiModelProperty@field)(description = "The publishing status of this book") publishingStatus: String,
                @(ApiModelProperty@field)(description = "The translation status of this book") translationStatus: Option[String],
                @(ApiModelProperty@field)(description = "Optional additional information about the book") additionalInformation: Option[String])

@ApiModel(description = "Information about book search hit")
case class BookHitV2(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
                   @(ApiModelProperty@field)(description = "The title of the book") title: String,
                   @(ApiModelProperty@field)(description = "Description of the book") description: String,
                   @(ApiModelProperty@field)(description = "Current language") language: Language,
                   @(ApiModelProperty@field)(description = "Information about reading level") readingLevel: Option[String],
                   @(ApiModelProperty@field)(description = "Information about categories") categories: Seq[Category],
                   @(ApiModelProperty@field)(description = "Cover image information") coverImage: Option[CoverImageV2],
                   @(ApiModelProperty@field)(description = "The date when this book arrived to the Global Digital Library (iso-format)") dateArrived: LocalDate,
                   @(ApiModelProperty@field)(description = "The source of the book") source: String,
                   @(ApiModelProperty@field)(description = "The highlighted title of the book") highlightTitle: Option[String],
                   @(ApiModelProperty@field)(description = "The Highlighted description of the book") highlightDescription: Option[String])

@ApiModel(description = "Information about search results")
case class SearchResultV2(@(ApiModelProperty@field)(description = "The total number of books matching this query") totalCount: Long,
                        @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                        @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                        @(ApiModelProperty@field)(description = "The chosen language") language: Option[Language],
                        @(ApiModelProperty@field)(description = "The results") results: Seq[BookHitV2])

@ApiModel(description = "Information about a book that is being translated by a user")
case class MyBookV2(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
                  @(ApiModelProperty@field)(description = "The revision of the book") revision: Long,
                  @(ApiModelProperty@field)(description = "The title of the book") title: String,
                  @(ApiModelProperty@field)(description = "The language this book has been translated from") translatedFrom: Option[Language],
                  @(ApiModelProperty@field)(description = "The language this book has been translated to") translatedTo: Language,
                  @(ApiModelProperty@field)(description = "Information about publisher of the original book") publisher: Publisher,
                  @(ApiModelProperty@field)(description = "Cover image information") coverImage: Option[CoverImageV2],
                  @(ApiModelProperty@field)(description = "Information about reading level") readingLevel: Option[String],
                  @(ApiModelProperty@field)(description = "Url to call to fetch latest translations from crowdin") synchronizeUrl: String,
                  @(ApiModelProperty@field)(description = "Url to Crowdin to continue translation") crowdinUrl: String)

@ApiModel(description = "Information about the featured content")
case class FeaturedContentV2(@(ApiModelProperty@field)(description = "The id of the featured content") id: Long,
                           @(ApiModelProperty@field)(description = "The revision of the featured content") revision: Long,
                           @(ApiModelProperty@field)(description = "Language of the featured content") language: Language,
                           @(ApiModelProperty@field)(description = "Title of the featured content") title: String,
                           @(ApiModelProperty@field)(description = "Description of the featured content") description: String,
                           @(ApiModelProperty@field)(description = "Link to the featured content") link: String,
                           @(ApiModelProperty@field)(description = "Image URL to the featured content") imageUrl: String,
                           @(ApiModelProperty@field)(description = "Which category (if any) the featured content belongs to") category: Option[Category])

@ApiModel(description = "Information about the metadata of a book to be translated. Contains placeholder content")
case class BookForTranslationV2(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
                              @(ApiModelProperty@field)(description = "The title of the book") title: String,
                              @(ApiModelProperty@field)(description = "Description of the book") description: String,
                              @(ApiModelProperty@field)(description = "Cover image information") coverImage: Option[CoverImageV2],
                              @(ApiModelProperty@field)(description = "Information about the chapters in the book") chapters: Seq[ChapterSummary])

@ApiModel(description = "Information about an Entry in an OPDS-Feed")
case class FeedEntryV2(@(ApiModelProperty@field)(description = "The book associated with this entry") book: BookV2,
                     @(ApiModelProperty@field)(description = "The revision of the feed") categories: Seq[FeedCategory] = Seq())

@ApiModel(description = "Information about an OPDS-Feed")
case class FeedV2(@(ApiModelProperty@field)(description = "Definitions of the feed") feedDefinition: FeedDefinition,
                @(ApiModelProperty@field)(description = "Title of the feed") title: String,
                @(ApiModelProperty@field)(description = "Description of the feed") description: Option[String],
                @(ApiModelProperty@field)(description = "rel attribute of each entry's link tag") rel: Option[String],
                @(ApiModelProperty@field)(description = "When the feed was last updated") updated: ZonedDateTime,
                @(ApiModelProperty@field)(description = "List of feed entries") content: Seq[FeedEntryV2],
                @(ApiModelProperty@field)(description = "List of facets, which contain links to other feeds or variants of the current one") facets: Seq[Facet])

object MediaType extends Enumeration {
  val IMAGE, AUDIO, VIDEO = Value

  def valueOf(s: String): Try[MediaType.Value] = {
    MediaType.values.find(_.toString.equalsIgnoreCase(s)) match {
      case Some(x) => Success(x)
      case None => Failure(new RuntimeException(s"Unknown MediaType $s."))
    }
  }
}