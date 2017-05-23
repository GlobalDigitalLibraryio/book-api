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
    val DefaultLicense = License("cc-by-4.0", Some("Creative Commons Attribution 4.0 International"), Some("https://creativecommons.org/licenses/by/4.0/"))
    val licenses = Map(
      "cc-by-4.0" -> DefaultLicense,
      "cc-by-sa-4.0" -> License("cc-by-sa-4.0", Some("Creative Commons Attribution-ShareAlike 4.0 International"), Some("https://creativecommons.org/licenses/by-sa/4.0/")),
      "cc-by-nc-4.0" -> License("cc-by-nc-4.0", Some("Creative Commons Attribution-NonCommercial 4.0 International"), Some("https://creativecommons.org/licenses/by-nc/4.0/")),
      "cc-by-nd-4.0" -> License("cc-by-nd-4.0", Some("Creative Commons Attribution-NoDerivatives 4.0 International"), Some("https://creativecommons.org/licenses/by-nd/4.0/")),
      "cc-by-nc-sa-4.0" -> License("cc-by-nc-sa-4.0", Some("Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International"), Some("https://creativecommons.org/licenses/by-nd/4.0/"))
    )

    def toDomainReadingMaterialInLanguage(rmInLanguage: NewReadingMaterialInLanguage): model.domain.ReadingMaterialInLanguage = {
      model.domain.ReadingMaterialInLanguage(
        None,
        None,
        None,
        rmInLanguage.externalId,
        rmInLanguage.title,
        rmInLanguage.description,
        rmInLanguage.language,
        model.domain.CoverPhoto(rmInLanguage.coverPhoto.large, rmInLanguage.coverPhoto.small),
        model.domain.Downloads(rmInLanguage.downloads.epub),
        rmInLanguage.dateCreated,
        rmInLanguage.datePublished,
        rmInLanguage.tags,
        rmInLanguage.authors
      )
    }

    def toDomainReadingMaterial(newReadingMaterial: NewReadingMaterial): model.domain.ReadingMaterial = {
      val rmInLanguage = model.domain.ReadingMaterialInLanguage(
        None,
        None,
        None,
        newReadingMaterial.externalId,
        newReadingMaterial.title,
        newReadingMaterial.description,
        newReadingMaterial.language,
        model.domain.CoverPhoto(newReadingMaterial.coverPhoto.large, newReadingMaterial.coverPhoto.small),
        model.domain.Downloads(newReadingMaterial.downloads.epub),
        newReadingMaterial.dateCreated,
        newReadingMaterial.datePublished,
        newReadingMaterial.tags,
        newReadingMaterial.authors
      )

      model.domain.ReadingMaterial(
        None,
        None,
        newReadingMaterial.externalId,
        newReadingMaterial.title,
        newReadingMaterial.description,
        newReadingMaterial.language,
        licenses.getOrElse(newReadingMaterial.license, DefaultLicense).license,
        newReadingMaterial.publisher,
        newReadingMaterial.readingLevel,
        newReadingMaterial.typicalAgeRange,
        newReadingMaterial.educationalUse,
        newReadingMaterial.educationalRole,
        newReadingMaterial.timeRequired,
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
          licenses.getOrElse(readingMaterial.license, DefaultLicense),
          readingMaterial.publisher,
          readingMaterial.readingLevel,
          readingMaterial.typicalAgeRange,
          readingMaterial.educationalUse,
          readingMaterial.educationalRole,
          readingMaterial.timeRequired,
          rmInLanguage.datePublished,
          rmInLanguage.dateCreated,
          readingMaterial.categories,
          CoverPhoto(rmInLanguage.coverPhoto.large, rmInLanguage.coverPhoto.small),
          Downloads(rmInLanguage.downloads.epub),
          rmInLanguage.tags,
          rmInLanguage.authors)
      })
    }
  }
}
