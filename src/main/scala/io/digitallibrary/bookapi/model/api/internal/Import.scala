package io.digitallibrary.bookapi.model.api.internal

import java.time.LocalDate

import io.digitallibrary.bookapi.model.api._
import io.digitallibrary.bookapi.model.domain.PageOrientation


case class CoverPhoto(imageApiId: Long)
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
                supportsTranslation: Boolean,
                bookFormat: String,
                source: String,
                pageOrientation: String,
                additionalInformation: Option[String],
                officiallyApproved: Option[Boolean])
