/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

import javax.servlet.ServletContext

import io.digitallibrary.bookapi.{ComponentRegistry, BookApiProperties}
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.booksController, BookApiProperties.ApiPath, "books")
    context.mount(ComponentRegistry.booksControllerV2, BookApiProperties.ApiPathV2, "booksV2")
    context.mount(ComponentRegistry.searchController, BookApiProperties.SearchPath, "search")
    context.mount(ComponentRegistry.searchControllerV2, BookApiProperties.SearchPathV2, "searchV2")
    context.mount(ComponentRegistry.opdsController, BookApiProperties.OpdsPath, "opds")
    context.mount(ComponentRegistry.opdsControllerV2, BookApiProperties.OpdsPath, "opdsV2")
    context.mount(ComponentRegistry.downloadController, BookApiProperties.DownloadPath, "downloads")
    context.mount(ComponentRegistry.downloadControllerV2, BookApiProperties.DownloadPathV2, "downloadsV2")
    context.mount(ComponentRegistry.languageController, BookApiProperties.LanguagesPath, "languages")
    context.mount(ComponentRegistry.languageControllerV2, BookApiProperties.LanguagesPathV2, "languagesV2")
    context.mount(ComponentRegistry.levelController, BookApiProperties.LevelsPath, "levels")
    context.mount(ComponentRegistry.levelControllerV2, BookApiProperties.LevelsPathV2, "levelsV2")
    context.mount(ComponentRegistry.categoriesController, BookApiProperties.CategoriesPath, "categories")
    context.mount(ComponentRegistry.categoriesControllerV2, BookApiProperties.CategoriesPathV2, "categoriesV2")
    context.mount(ComponentRegistry.translationsController, BookApiProperties.TranslationsPath, "translations")
    context.mount(ComponentRegistry.translationsControllerV2, BookApiProperties.TranslationsPathV2, "translationsV2")
    context.mount(ComponentRegistry.featuredContentController, BookApiProperties.FeaturedContentPath, "featuredcontent")
    context.mount(ComponentRegistry.featuredContentControllerV2, BookApiProperties.FeaturedContentPathV2, "featuredcontentV2")
    context.mount(ComponentRegistry.exportController, BookApiProperties.ExportPath, "export")
    context.mount(ComponentRegistry.resourcesApp, BookApiProperties.ApiDocPath)
    context.mount(ComponentRegistry.sourceController, BookApiProperties.SourcePath, "sources")
    context.mount(ComponentRegistry.sourceControllerV2, BookApiProperties.SourcePathV2, "sourcesV2")
    context.mount(ComponentRegistry.healthController, "/health")
    context.mount(ComponentRegistry.internController, "/intern")
  }

}
