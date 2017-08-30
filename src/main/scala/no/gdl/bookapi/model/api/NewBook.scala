/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.api

import java.util.Date

case class NewBook(externalId: Option[String],
                   title: String,
                   description: String,
                   language: String,
                   coverPhoto: CoverPhoto,
                   downloads: Downloads,
                   tags: Seq[String],
                   authors: Seq[String],
                   license: String,
                   publisher: String,
                   categories: Seq[String],
                   dateCreated: Option[Date],
                   datePublished: Option[Date],
                   readingLevel: Option[String],
                   typicalAgeRange: Option[String],
                   educationalUse: Option[String],
                   educationalRole: Option[String],
                   timeRequired: Option[String])

case class NewBookInLanguage(externalId: Option[String],
                             title: String,
                             description: String,
                             language: String,
                             dateCreated: Option[Date],
                             datePublished: Option[Date],
                             coverPhoto: CoverPhoto,
                             downloads: Downloads,
                             tags: Seq[String],
                             authors: Seq[String])