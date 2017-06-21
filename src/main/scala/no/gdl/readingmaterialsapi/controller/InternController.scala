/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.readingmaterialsapi.controller

import no.gdl.readingmaterialsapi.ReadingMaterialsApiProperties.DefaultLanguage
import no.gdl.readingmaterialsapi.model.api.{Error, NewReadingMaterial, NewReadingMaterialInLanguage}
import no.gdl.readingmaterialsapi.service.{ConverterService, ReadService, WriteService}
import org.scalatra.NotFound

trait InternController {
  this: WriteService with ReadService with ConverterService =>
  val internController: InternController

  class InternController extends GdlController {
    post("/new") {
      val newReadingMaterial = extract[NewReadingMaterial](request.body)
      readService.withExternalId(newReadingMaterial.externalId) match {
        case Some(existing) => writeService.updateReadingMaterial(existing, newReadingMaterial)
        case None => writeService.newReadingMaterial(newReadingMaterial).get
      }
    }

    post("/:externalId/languages/") {
      val externalId = params("externalId")
      val newTranslation = extract[NewReadingMaterialInLanguage](request.body)
      readService.readingMaterialInLanguageWithExternalId(newTranslation.externalId) match {
        case Some(existing) => writeService.updateReadingMaterialInLanguage(existing, newTranslation)
        case None => {
          readService.withExternalId(Some(externalId)) match {
            case Some(existing) => writeService.newReadingMaterialInLanguage(existing.id.get, newTranslation).get
            case None => throw new RuntimeException(s"No reading material with external_id = $externalId")
          }
        }
      }
    }

    get("/:externalId") {
      val externalId = params.get("externalId")
      val language = paramOrDefault("language", DefaultLanguage)

      readService.withExternalId(externalId).flatMap(c => converterService.toApiReadingMaterial(c, language)) match {
        case Some(x) => x
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No reading material with id $externalId found"))
      }
    }
  }
}
