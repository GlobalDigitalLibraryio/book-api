/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.service

import com.typesafe.scalalogging.LazyLogging
import no.gdl.readingmaterialsapi.model.api.{NewReadingMaterial, NewReadingMaterialInLanguage, ReadingMaterial}
import no.gdl.readingmaterialsapi.model.domain.{CoverPhoto, Downloads, ReadingMaterialInLanguage}
import no.gdl.readingmaterialsapi.repository.ReadingMaterialsRepository

import scala.util.{Failure, Success, Try}


trait WriteService {
  this: ReadingMaterialsRepository with ConverterService =>
  val writeService: WriteService

  class WriteService extends LazyLogging {

    def newReadingMaterialInLanguage(id: Long, rmInLanguage: NewReadingMaterialInLanguage): Try[ReadingMaterial] = {
      readingMaterialsRepository.insertReadingMaterialInLanguage(
        converterService.toDomainReadingMaterialInLanguage(rmInLanguage).copy(readingMaterialId = Some(id)))

      readingMaterialsRepository.withId(id).flatMap(a => converterService.toApiReadingMaterial(a, rmInLanguage.language)) match {
        case Some(x) => Success(x)
        case None => Failure(new RuntimeException(s"Could not read newly inserted reading material for langeuage ${rmInLanguage.language}"))
      }
    }

    def newReadingMaterial(newReadingMaterial: NewReadingMaterial): Try[ReadingMaterial] = {
      converterService.toApiReadingMaterial(readingMaterialsRepository.insertReadingMaterial(
        converterService.toDomainReadingMaterial(newReadingMaterial)), newReadingMaterial.language) match {
        case Some(x) => Success(x)
        case None => Failure(new RuntimeException(s"Could not read newly inserted reading material for langeuage ${newReadingMaterial.language}"))
      }
    }

    def updateReadingMaterialInLanguage(existing: ReadingMaterialInLanguage, newTranslation: NewReadingMaterialInLanguage): ReadingMaterialInLanguage = {
      val newDomainTranslation = converterService.toDomainReadingMaterialInLanguage(newTranslation)
      val toUpdate = existing.copy(
        title = newDomainTranslation.title,
        description = newDomainTranslation.description,
        coverPhoto = newDomainTranslation.coverPhoto,
        downloads = newDomainTranslation.downloads,
        dateCreated = newDomainTranslation.dateCreated,
        datePublished = newDomainTranslation.datePublished,
        tags = newDomainTranslation.tags,
        authors = newDomainTranslation.authors,
        language = newDomainTranslation.language)

      val updated = readingMaterialsRepository.updateReadingMaterialInLanguage(toUpdate)
      logger.info(s"Updated readingmaterial in language with id = ${updated.id}")
      updated
    }

    def updateReadingMaterial(existing: no.gdl.readingmaterialsapi.model.domain.ReadingMaterial, newReadingMaterial: NewReadingMaterial): ReadingMaterial = {
      val inLanguageToKeep = existing.readingMaterialInLanguage.filterNot(_.language == newReadingMaterial.language)
      val inLanguageToUpdate = existing.readingMaterialInLanguage.find(_.language == newReadingMaterial.language)
        .map(_.copy(title = newReadingMaterial.title,
          description = newReadingMaterial.description,
          language = newReadingMaterial.language,
          coverPhoto = CoverPhoto(newReadingMaterial.coverPhoto.large, newReadingMaterial.coverPhoto.small),
          downloads = Downloads(newReadingMaterial.downloads.epub),
          dateCreated = newReadingMaterial.dateCreated,
          datePublished = newReadingMaterial.datePublished,
          tags = newReadingMaterial.tags,
          authors = newReadingMaterial.authors))


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
