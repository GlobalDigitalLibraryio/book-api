/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.api

import java.time.{LocalDate, ZonedDateTime}

import org.json4s.CustomSerializer
import org.json4s.JsonAST.{JNull, JString}
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about the licensing of the book")
case class License(@(ApiModelProperty@field)(description = "The name of the license") name: String,
                   @(ApiModelProperty@field)(description = "Description of the license") description: Option[String],
                   @(ApiModelProperty@field)(description = "Url to where the license can be found") url: Option[String])


@ApiModel(description = "Information about where to find the cover image for the book")
case class CoverImage(@(ApiModelProperty@field)(description = "URL to the cover-image") url: String,
                      @(ApiModelProperty@field)(description = "Alternate text for the cover image") alttext: Option[String],
                      @(ApiModelProperty@field)(description = "Image id of the cover-image") imageId: String)


@ApiModel(description = "Information about where to download a copy of the book")
case class Downloads(@(ApiModelProperty@field)(description = "URL to an epub-download") epub: Option[String],
                     @(ApiModelProperty@field)(description = "URL to a pdf-download") pdf: Option[String])

@ApiModel(description = "Information about the contributors of the current book")
case class Contributor(@(ApiModelProperty@field)(description = "Id of the contributor") id: Long,
                       @(ApiModelProperty@field)(description = "The revision of the contributor") revision: Int,
                       @(ApiModelProperty@field)(description = "The type of the contributor", allowableValues = "Author, Illustrator, Translator, Photographer, Contributor") `type`: String,
                       @(ApiModelProperty@field)(description = "Name of the contributor") name: String)

@ApiModel(description = "Information about the publisher of the current book")
case class Publisher(@(ApiModelProperty@field)(description = "Id of the publisher") id: Long,
                     @(ApiModelProperty@field)(description = "The revision of the publisher") revision: Int,
                     @(ApiModelProperty@field)(description = "Name of the publisher") name: String)

@ApiModel(description = "Information about a category of a book")
case class Category(@(ApiModelProperty@field)(description = "Id for the category") id: Long,
                    @(ApiModelProperty@field)(description = "The revision of the category") revision: Int,
                    @(ApiModelProperty@field)(description = "Name of the category") name: String)

@ApiModel(description = "Information about the reading levels available within a category")
case class ReadingLevels(@(ApiModelProperty@field)(description = "A list of reading levels available within the category") readingLevels: Set[String])

@ApiModel(description = "Information about a chapter in a book")
case class ChapterSummary(@(ApiModelProperty@field)(description = "Id for the chapter") id: Long,
                          @(ApiModelProperty@field)(description = "The sequence number of the chapter") seqNo: Int,
                          @(ApiModelProperty@field)(description = "Title of the chapter") title: Option[String],
                          @(ApiModelProperty@field)(description = "URL to where chapter can be found") url: String)

@ApiModel(description = "Information about a chapter in a book")
case class Chapter(@(ApiModelProperty@field)(description = "Id for the chapter") id: Long,
                   @(ApiModelProperty@field)(description = "The revision of the chapter") revision: Int,
                   @(ApiModelProperty@field)(description = "The sequence number of the chapter") seqNo: Int,
                   @(ApiModelProperty@field)(description = "Title of the chapter") title: Option[String],
                   @(ApiModelProperty@field)(description = "The HTML content of the chapter") content: String,
                   @(ApiModelProperty@field)(description = "Indicates type of chapter. One of Content, License, Cover, BackCover") chapterType: String,
                   @(ApiModelProperty@field)(description = "List of all images referenced in chapter") images: Seq[String])

@ApiModel(description = "Information about a language")
case class Language(@(ApiModelProperty@field)(description = "ISO 639-2 code") code: String,
                    @(ApiModelProperty@field)(description = "Human readable name of the language") name: String)

@ApiModel(description = "Information about book")
case class Book(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
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
                @(ApiModelProperty@field)(description = "Cover image information") coverImage: Option[CoverImage],
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
case class BookHit(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
                   @(ApiModelProperty@field)(description = "The title of the book") title: String,
                   @(ApiModelProperty@field)(description = "Description of the book") description: String,
                   @(ApiModelProperty@field)(description = "Current language") language: Language,
                   @(ApiModelProperty@field)(description = "Information about reading level") readingLevel: Option[String],
                   @(ApiModelProperty@field)(description = "Information about categories") categories: Seq[Category],
                   @(ApiModelProperty@field)(description = "Cover image information") coverImage: Option[CoverImage],
                   @(ApiModelProperty@field)(description = "The date when this book arrived to the Global Digital Library (iso-format)") dateArrived: LocalDate,
                   @(ApiModelProperty@field)(description = "The source of the book") source: String,
                   @(ApiModelProperty@field)(description = "The highlighted title of the book") highlightTitle: Option[String],
                   @(ApiModelProperty@field)(description = "The Highlighted description of the book") highlightDescription: Option[String])

@ApiModel(description = "Information about search results")
case class SearchResult(@(ApiModelProperty@field)(description = "The total number of books matching this query") totalCount: Long,
                        @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                        @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                        @(ApiModelProperty@field)(description = "The chosen language") language: Option[Language],
                        @(ApiModelProperty@field)(description = "The results") results: Seq[BookHit])

@ApiModel(description = "Information about a book that is being translated by a user")
case class MyBook(@(ApiModelProperty@field)(description = "The id of the book") id: Long,
                  @(ApiModelProperty@field)(description = "The revision of the book") revision: Long,
                  @(ApiModelProperty@field)(description = "The title of the book") title: String,
                  @(ApiModelProperty@field)(description = "The language this book has been translated from") translatedFrom: Option[Language],
                  @(ApiModelProperty@field)(description = "The language this book has been translated to") translatedTo: Language,
                  @(ApiModelProperty@field)(description = "Information about publisher of the original book") publisher: Publisher,
                  @(ApiModelProperty@field)(description = "Cover image information") coverImage: Option[CoverImage],
                  @(ApiModelProperty@field)(description = "Url to call to fetch latest translations from crowdin") synchronizeUrl: String,
                  @(ApiModelProperty@field)(description = "Url to Crowdin to continue translation") crowdinUrl: String)

@ApiModel(description = "Information about the featured content")
case class FeaturedContent(@(ApiModelProperty@field)(description = "The id of the featured content") id: Long,
                           @(ApiModelProperty@field)(description = "The revision of the featured content") revision: Long,
                           @(ApiModelProperty@field)(description = "Language of the featured content") language: Language,
                           @(ApiModelProperty@field)(description = "Title of the featured content") title: String,
                           @(ApiModelProperty@field)(description = "Description of the featured content") description: String,
                           @(ApiModelProperty@field)(description = "Link to the featured content") link: String,
                           @(ApiModelProperty@field)(description = "Image URL to the featured content") imageUrl: String,
                           @(ApiModelProperty@field)(description = "Which category (if any) the featured content belongs to") category: Option[Category])

case class FeedDefinition(@(ApiModelProperty@field)(description = "The internal id of the opds feed") id: Long,
                          @(ApiModelProperty@field)(description = "The revision of the feed") revision: Int,
                          @(ApiModelProperty@field)(description = "The url of the feed") url: String,
                          @(ApiModelProperty@field)(description = "The uuid of the feed") uuid: String)

@ApiModel(description = "Information about an OPDS-Feed")
case class Feed(@(ApiModelProperty@field)(description = "Definitions of the feed") feedDefinition: FeedDefinition,
                @(ApiModelProperty@field)(description = "Title of the feed") title: String,
                @(ApiModelProperty@field)(description = "Description of the feed") description: Option[String],
                @(ApiModelProperty@field)(description = "rel attribute of each entry's link tag") rel: Option[String],
                @(ApiModelProperty@field)(description = "When the feed was last updated") updated: ZonedDateTime,
                @(ApiModelProperty@field)(description = "List of feed entries") content: Seq[FeedEntry],
                @(ApiModelProperty@field)(description = "List of facets, which contain links to other feeds or variants of the current one") facets: Seq[Facet])

@ApiModel(description = "Information about a facet in an OPDS-feed. A facet links to another feed")
case class Facet(@(ApiModelProperty@field)(description = "The location of the feed referred to, as a URL") href: String,
                 @(ApiModelProperty@field)(description = "The title of the other feed") title: String,
                 @(ApiModelProperty@field)(description = "Which group the facet belongs to") group: String,
                 @(ApiModelProperty@field)(description = "Indicates if this facet is the current feed. Only 1 facet should be set as active per group") isActive: Boolean)

@ApiModel(description = "Information about an Entry in an OPDS-Feed")
case class FeedEntry(@(ApiModelProperty@field)(description = "The book associated with this entry") book: Book,
                     @(ApiModelProperty@field)(description = "The revision of the feed") categories: Seq[FeedCategory] = Seq())

@ApiModel(description = "Information about a feed category in an opds-feed")
case class FeedCategory(@(ApiModelProperty@field)(description = "The url to the category feed") url: String,
                        @(ApiModelProperty@field)(description = "The title for this category feed") title: String,
                        @(ApiModelProperty@field)(description = "The sort order for this category") sortOrder: Int = 100)

@ApiModel(description = "Id of featured content")
case class FeaturedContentId(id: Long)

@ApiModel(description = "Information about a request to translate a book")
case class TranslateRequest(@(ApiModelProperty@field)(description = "The id of the book that is to be translated") bookId: Long,
                            @(ApiModelProperty@field)(description = "The language to translate from") fromLanguage: String,
                            @(ApiModelProperty@field)(description = "The language to translate to") toLanguage: String)

@ApiModel(description = "Information about the response from a translation request")
case class TranslateResponse(@(ApiModelProperty@field)(description = "The id of the book that has been sent to translation") bookId: Long,
                             @(ApiModelProperty@field)(description = "The url of where the book can be translated") crowdinUrl: String)

@ApiModel(description = "Information about the response from a synchronize request")
case class SynchronizeResponse(@(ApiModelProperty@field)(description = "The id of the book that has been sent to translation") bookId: Long,
                               @(ApiModelProperty@field)(description = "The url of where the book can be translated") crowdinUrl: String)

@ApiModel(description = "Information about a source and the number of books for this source")
case class Source(@(ApiModelProperty@field)(description = "The source of where a book was imported from") source: String,
                  @(ApiModelProperty@field)(description = "The number of books with this source") count: Long)

case object LocalDateSerializer extends CustomSerializer[LocalDate](format => ( {
  case JString(p) => LocalDate.parse(p)
  case JNull => null
}, {
  case ld: LocalDate => JString(ld.toString)
}))