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
      readService.withTitle(newReadingMaterial.title) match {
        case Some(existing) => writeService.updateReadingMaterial(existing, newReadingMaterial)
        case None => writeService.newReadingMaterial(newReadingMaterial).get
      }
    }

    post("/:id/languages/") {
      val bookId = long("id")
      val newTranslation = extract[NewReadingMaterialInLanguage](request.body)
      readService.readingMaterialInLanguageFor(bookId, newTranslation.language) match {
        case Some(existing) => writeService.updateReadingMaterialInLanguage(existing, newTranslation)
        case None => writeService.newReadingMaterialInLanguage(bookId, newTranslation).get
      }
    }
  }
}
