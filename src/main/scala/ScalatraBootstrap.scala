/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

import javax.servlet.ServletContext

import no.gdl.readingmaterialsapi.ComponentRegistry
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.readingMaterialsController, "/reading-materials-api/v1/reading-materials", "reading-materials")
    context.mount(ComponentRegistry.opdsController, "/reading-materials-api/opds", "opds")
    context.mount(ComponentRegistry.healthController, "/health")
    context.mount(ComponentRegistry.internController, "/intern")
    context.mount(ComponentRegistry.resourcesApp, "/reading-materials-api/api-docs")
  }

}
