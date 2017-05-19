/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.service

import com.typesafe.scalalogging.LazyLogging
import no.gdl.readingmaterialsapi.model.api.{NewReadingMaterial, NewReadingMaterialInLanguage, ReadingMaterial}
import no.gdl.readingmaterialsapi.model.domain.{CoverPhoto, Downloads, License}
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository


trait WriteService {
  this: ReadingMaterialsRepository with ConverterService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def newReadingMaterialInLanguage(id: Long, rmInLanguage: NewReadingMaterialInLanguage): Option[ReadingMaterial] = {
      readingMaterialsRepository.insertReadingMaterialInLanguage(
        converterService.toDomainReadingMaterialInLanguage(rmInLanguage).copy(readingMaterialId = Some(id)))

      readingMaterialsRepository.withId(id).flatMap(a => converterService.toApiReadingMaterial(a, rmInLanguage.language))
    }

    def newReadingMaterial(newReadingMaterial: NewReadingMaterial): ReadingMaterial = {
      val inserted = converterService.toApiReadingMaterial(
        readingMaterialsRepository.insertReadingMaterial(
          converterService.toDomainReadingMaterial(newReadingMaterial)), newReadingMaterial.language).get
      //TODO: Better handling of the option.get

      logger.info(s"Added reading material with id = ${inserted.id}")
      inserted
    }

    def updateReadingMaterial(existing: no.gdl.readingmaterialsapi.model.domain.ReadingMaterial, newReadingMaterial: NewReadingMaterial): ReadingMaterial = {
      val inLanguageToKeep = existing.readingMaterialInLanguage.filterNot(_.language == newReadingMaterial.language)
      val inLanguageToUpdate = existing.readingMaterialInLanguage.find(x => x.language == newReadingMaterial.language)
      inLanguageToUpdate match {
        case Some(x) =>
          x.copy(
            title = newReadingMaterial.title,
            description = newReadingMaterial.description,
            language = newReadingMaterial.language,
            coverPhoto = CoverPhoto(newReadingMaterial.coverPhoto.large, newReadingMaterial.coverPhoto.small),
            downloads = Downloads(newReadingMaterial.downloads.epub),
            dateCreated = newReadingMaterial.dateCreated,
            datePublished = newReadingMaterial.datePublished,
            tags = newReadingMaterial.tags,
            authors = newReadingMaterial.authors
          )
        case None => None
      }

      val toUpdate = existing.copy(
        title = newReadingMaterial.title,
        description = newReadingMaterial.description,
        language = newReadingMaterial.language,
        license = converterService.licenses.getOrElse(newReadingMaterial.license, converterService.DefaultLicense).license,
        publisher = newReadingMaterial.publisher,
        readingLevel = newReadingMaterial.readingLevel,
        typicalAgeRange = newReadingMaterial.typicalAgeRange,
        educationalUse = newReadingMaterial.educationalUse,
        educationalRole = newReadingMaterial.educationalRole,
        timeRequired = newReadingMaterial.timeRequired,
        categories = newReadingMaterial.categories,
        readingMaterialInLanguage = inLanguageToKeep ++ inLanguageToUpdate)

      logger.info(s"Updated reading material with id = ${existing.id}")
      converterService.toApiReadingMaterial(readingMaterialsRepository.updateReadingMaterial(toUpdate), newReadingMaterial.language).get
    }
  }
}
