/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.api.internal

import java.util.Date



case class NewBook(license: String,
                   publisher: String)

case class BookId(id: Long)
case class TranslationId(id: Long)
case class ChapterId(id: Long)

case class NewPerson(name: String)

case class NewContributor(`type`: String,
                          person: NewPerson)

case class NewCategory(name: String)

case class NewEducationalAlignment(alignmentType: Option[String],
                                   educationalFramework: Option[String],
                                   targetDescription: Option[String],
                                   targetName: Option[String],
                                   targetUrl: Option[String])

case class NewTranslation(externalId: Option[String],
                          title: String,
                          about: String,
                          numPages: Option[Int],
                          language: String,
                          datePublished: Option[Date],
                          dateCreated: Option[Date],
                          coverphoto: Option[Long],
                          tags: Seq[String],
                          isBasedOnUrl: Option[String],
                          educationalUse: Option[String],
                          educationalRole: Option[String],
                          timeRequired: Option[String],
                          typicalAgeRange: Option[String],
                          readingLevel: Option[String],
                          interactivityType: Option[String],
                          learningResourceType: Option[String],
                          accessibilityApi: Option[String],
                          accessibilityControl: Option[String],
                          accessibilityFeature: Option[String],
                          accessibilityHazard: Option[String],
                          contributors: Seq[NewContributor],
                          categories: Seq[NewCategory],
                          educationalAlignment: Option[NewEducationalAlignment])

case class NewChapter(seqNo: Int,
                      title: Option[String],
                      content: String)
