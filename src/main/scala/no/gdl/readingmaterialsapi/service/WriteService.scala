/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.service

import no.gdl.readingmaterialsapi.model.api.{NewReadingMaterial, NewReadingMaterialInLanguage, ReadingMaterial}
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository


trait WriteService {
  this: ReadingMaterialsRepository with ConverterService =>
  val writeService: WriteService

  class WriteService {
    def newReadingMaterialInLanguage(id: Long, rmInLanguage: NewReadingMaterialInLanguage): Option[ReadingMaterial] = {
      readingMaterialsRepository.insertReadingMaterialInLanguage(
        converterService.toDomainReadingMaterialInLanguage(rmInLanguage).copy(readingMaterialId = Some(id)))

      readingMaterialsRepository.withId(id).flatMap(a => converterService.toApiReadingMaterial(a, rmInLanguage.language))
    }

    def newReadingMaterial(newReadingMaterial: NewReadingMaterial): Option[ReadingMaterial] = {
      converterService.toApiReadingMaterial(
        readingMaterialsRepository.insertReadingMaterial(
          converterService.toDomainReadingMaterial(newReadingMaterial)), newReadingMaterial.language)
    }
  }
}
