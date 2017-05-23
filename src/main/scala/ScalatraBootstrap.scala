/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

import javax.servlet.ServletContext

import no.gdl.readingmaterialsapi.{ComponentRegistry, ReadingMaterialsApiProperties}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.readingMaterialsController, ReadingMaterialsApiProperties.ApiPath, "reading-materials")
    context.mount(ComponentRegistry.opdsController, ReadingMaterialsApiProperties.OpdsPath, "opds")
    context.mount(ComponentRegistry.resourcesApp, ReadingMaterialsApiProperties.ApiDocPath)
    context.mount(ComponentRegistry.healthController, "/health")
    context.mount(ComponentRegistry.internController, "/intern")
  }

}
