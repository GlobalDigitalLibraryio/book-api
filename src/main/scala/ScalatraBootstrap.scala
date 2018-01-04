/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

import javax.servlet.ServletContext

import no.gdl.bookapi.{ComponentRegistry, BookApiProperties}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.booksController, BookApiProperties.ApiPath, "books")
    context.mount(ComponentRegistry.opdsController, BookApiProperties.OpdsPath, "opds")
    context.mount(ComponentRegistry.downloadController, BookApiProperties.DownloadPath, "downloads")
    context.mount(ComponentRegistry.languageController, BookApiProperties.LanguagesPath, "languages")
    context.mount(ComponentRegistry.levelController, BookApiProperties.LevelsPath, "levels")
    context.mount(ComponentRegistry.translationsController, BookApiProperties.TranslationsPath, "translations")
    context.mount(ComponentRegistry.editorsPickController, BookApiProperties.EditorPicksPath, "editorspicks")
    context.mount(ComponentRegistry.resourcesApp, BookApiProperties.ApiDocPath)
    context.mount(ComponentRegistry.healthController, "/health")
    context.mount(ComponentRegistry.internController, "/intern")
  }

}
