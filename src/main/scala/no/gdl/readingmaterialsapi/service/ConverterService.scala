/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.service

import no.gdl.readingmaterialsapi.model
import no.gdl.readingmaterialsapi.model.api._


trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def toDomainReadingMaterialInLanguage(rmInLanguage: NewReadingMaterialInLanguage): model.domain.ReadingMaterialInLanguage = {
      model.domain.ReadingMaterialInLanguage(
        None,
        None,
        None,
        rmInLanguage.title,
        rmInLanguage.description,
        rmInLanguage.language,
        model.domain.CoverPhoto(rmInLanguage.coverPhoto.large, rmInLanguage.coverPhoto.small),
        model.domain.Downloads(rmInLanguage.downloads.epub), rmInLanguage.tags, rmInLanguage.authors.map(a => model.domain.Author(a.id, a.name))
      )
    }

    def toDomainReadingMaterial(newReadingMaterial: NewReadingMaterial): model.domain.ReadingMaterial = {
      val rmInLanguage = model.domain.ReadingMaterialInLanguage(
        None,
        None,
        None,
        newReadingMaterial.title,
        newReadingMaterial.description,
        newReadingMaterial.language,
        model.domain.CoverPhoto(newReadingMaterial.coverPhoto.large, newReadingMaterial.coverPhoto.small),
        model.domain.Downloads(newReadingMaterial.downloads.epub),
        newReadingMaterial.tags,
        newReadingMaterial.authors.map(a => model.domain.Author(a.id, a.name))
      )

      model.domain.ReadingMaterial(
        None,
        None,
        newReadingMaterial.title,
        newReadingMaterial.description,
        newReadingMaterial.language,
        model.domain.License(newReadingMaterial.license.license, newReadingMaterial.license.description, newReadingMaterial.license.url),
        newReadingMaterial.publisher,
        newReadingMaterial.readingLevel,
        newReadingMaterial.categories,
        Seq(rmInLanguage))
    }

    def toApiReadingMaterial(readingMaterial: model.domain.ReadingMaterial, language: String): Option[ReadingMaterial] = {
      readingMaterial.readingMaterialInLanguage.find(a => a.language == language).map(rmInLanguage => {
        ReadingMaterial(
          readingMaterial.id.get,
          readingMaterial.revision.get,
          rmInLanguage.title,
          rmInLanguage.description,
          language,
          readingMaterial.readingMaterialInLanguage.map(_.language),
          License(readingMaterial.license.license, readingMaterial.license.description, readingMaterial.license.url),
          readingMaterial.publisher,
          readingMaterial.readingLevel,
          readingMaterial.categories,
          CoverPhoto(rmInLanguage.coverPhoto.large, rmInLanguage.coverPhoto.small),
          Downloads(rmInLanguage.downloads.epub),
          rmInLanguage.tags,
          rmInLanguage.authors.map(a => Author(a.id, a.name)))
      })
    }
  }
}
