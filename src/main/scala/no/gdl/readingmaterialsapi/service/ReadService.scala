/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.service


import no.gdl.readingmaterialsapi.model.domain.{ReadingMaterial, ReadingMaterialInLanguage}
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository

trait ReadService {
  this: ReadingMaterialsRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def withTitle(title: String): Option[ReadingMaterial] = {
      readingMaterialsRepository.withTitle(title)
    }

    def withId(id: Long): Option[ReadingMaterial] = {
      readingMaterialsRepository.withId(id)
    }

    def withExternalId(externalId: Option[String]): Option[ReadingMaterial] = {
      externalId.flatMap(readingMaterialsRepository.withExternalId)
    }

    def readingMaterialInLanguageWithExternalId(externalId: Option[String]): Option[ReadingMaterialInLanguage] = {
      externalId.flatMap(readingMaterialsRepository.readingMaterialInLanguageWithExternalId)
    }

    def all(language: String): Seq[ReadingMaterial] = {
      readingMaterialsRepository.all()
    }
  }
}
