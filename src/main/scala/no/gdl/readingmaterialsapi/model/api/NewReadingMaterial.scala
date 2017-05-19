/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.model.api

import java.util.Date

case class NewReadingMaterial(title: String,
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

case class NewReadingMaterialInLanguage(title: String,
                                        description: String,
                                        language: String,
                                        dateCreated: Option[Date],
                                        datePublished: Option[Date],
                                        coverPhoto: CoverPhoto,
                                        downloads: Downloads,
                                        tags: Seq[String],
                                        authors: Seq[String])