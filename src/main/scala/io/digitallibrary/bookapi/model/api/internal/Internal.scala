/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.api.internal

import java.time.LocalDate



case class NewBook(license: String,
                   publisher: String)

case class BookId(id: Long)
case class TranslationId(id: Long)
case class ChapterId(id: Long)
case class UUID(uuid: String)

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
                          numPages: Option[String],
                          language: String,
                          translatedFrom: Option[String],
                          datePublished: Option[LocalDate],
                          dateCreated: Option[LocalDate],
                          dateArrived: Option[LocalDate],
                          coverphoto: Option[String],
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
                          bookFormat: String,
                          contributors: Seq[NewContributor],
                          categories: Seq[NewCategory],
                          educationalAlignment: Option[NewEducationalAlignment])

case class NewChapter(seqNo: Int,
                      title: Option[String],
                      content: String,
                      chapterType: Option[String])

case class NewTranslatedChapter(seqNo: Int, title: Option[String], content: String, originalChapterId: Long)
