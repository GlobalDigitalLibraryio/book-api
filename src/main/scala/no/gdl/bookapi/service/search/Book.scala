/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service.search

import java.time.LocalDate

import no.gdl.bookapi.model.api._

case class Book(id: Long,
                revision: Long,
                externalId: Option[String],
                uuid: String,
                title: String,
                description: String,
                translatedFrom: Option[Language],
                language: Language,
                availableLanguages: Seq[Language],
                license: License,
                publisher: Publisher,
                readingLevel: Option[String],
                typicalAgeRange: Option[String],
                educationalUse: Option[String],
                educationalRole: Option[String],
                timeRequired: Option[String],
                datePublished: Option[LocalDate],
                dateCreated: Option[LocalDate],
                dateArrived: LocalDate,
                categories: Seq[Category],
                coverPhoto: Option[CoverPhoto],
                downloads: Downloads,
                tags: Seq[String],
                contributors: Seq[Contributor],
                chapters: Seq[Chapter],
                supportsTranslation: Boolean)
