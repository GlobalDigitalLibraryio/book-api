/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.service

import no.gdl.readingmaterialsapi.model.api.ReadingMaterial
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository

trait ReadService {
  this: ReadingMaterialsRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def withTitle(title: String): Option[no.gdl.readingmaterialsapi.model.domain.ReadingMaterial] = {
      readingMaterialsRepository.withTitle(title)
    }

    def withId(id: Long, language: String): Option[ReadingMaterial] = {
      readingMaterialsRepository.withId(id).flatMap(c => converterService.toApiReadingMaterial(c, language))
    }

    def all(language: String): Seq[ReadingMaterial] = {
      readingMaterialsRepository.all().flatMap(c => converterService.toApiReadingMaterial(c, language))
    }
  }
}
