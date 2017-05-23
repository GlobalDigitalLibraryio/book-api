/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.readingmaterialsapi.controller

import no.gdl.readingmaterialsapi.model.api.{NewReadingMaterial, NewReadingMaterialInLanguage}
import no.gdl.readingmaterialsapi.service.{ReadService, WriteService}

trait InternController {
  this: WriteService with ReadService =>
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
  }
}
