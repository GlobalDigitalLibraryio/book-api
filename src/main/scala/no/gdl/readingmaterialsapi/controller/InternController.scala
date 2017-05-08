/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.readingmaterialsapi.controller

import no.gdl.readingmaterialsapi.model.api.{NewReadingMaterial, NewReadingMaterialInLanguage}
import no.gdl.readingmaterialsapi.service.WriteService
import org.scalatra.NotFound

trait InternController {
  this: WriteService =>
  val internController: InternController

  class InternController extends GdlController {
    post("/new") {
      writeService.newReadingMaterial(extract[NewReadingMaterial](request.body)) match {
        case Some(x) => x
        case None => NotFound("TODO: Denne kan nesten ikke være 404")
      }
    }

    post("/:id/languages/") {
      writeService.newReadingMaterialInLanguage(long("id"), extract[NewReadingMaterialInLanguage](request.body)) match {
        case Some(x) => x
        case None => NotFound("TODO: Denne kan nesten ikke være 404")
      }
    }
  }
}
