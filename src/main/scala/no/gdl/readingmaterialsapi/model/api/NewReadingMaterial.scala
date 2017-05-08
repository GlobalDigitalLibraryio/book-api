/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.model.api

case class NewReadingMaterial(title: String,
                              description: String,
                              language: String,
                              coverPhoto: CoverPhoto,
                              downloads: Downloads,
                              tags: Seq[String],
                              authors: Seq[Author],
                              license: License,
                              publisher: String,
                              readingLevel: String,
                              categories: Seq[String])

case class NewReadingMaterialInLanguage(title: String,
                                        description: String,
                                        language: String,
                                        coverPhoto: CoverPhoto,
                                        downloads: Downloads,
                                        tags: Seq[String],
                                        authors: Seq[Author])