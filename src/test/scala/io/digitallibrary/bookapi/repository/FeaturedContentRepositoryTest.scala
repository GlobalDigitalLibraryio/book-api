/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.domain._
import io.digitallibrary.bookapi.{IntegrationSuite, TestEnvironment}


class FeaturedContentRepositoryTest extends IntegrationSuite with TestEnvironment with RepositoryTestHelpers {

  override val featuredContentRepository = new FeaturedContentRepository
  override val bookRepository = new BookRepository
  override val publisherRepository = new PublisherRepository
  override val categoryRepository = new CategoryRepository
  override val unFlaggedTranslationsRepository = new TranslationRepository

  test("that forLanguage returns an empty list when no featured content for any languages") {
    val featuredContents = featuredContentRepository.forLanguage(LanguageTag("eng"))
    featuredContents should equal(Nil)
  }

  test("that forLanguage returns an empty list when no featured content for given language") {
    withRollback { implicit session =>
      featuredContentRepository.addContent(FeaturedContent(
        id = None,
        revision = None,
        language = LanguageTag("eng"),
        title = "Some title 1",
        description = "Some description",
        link = "http://example.com",
        imageUrl = "http://example.com/example.png",
        categoryId = None))
      val featuredContents = featuredContentRepository.forLanguage(LanguageTag("nob"))
      featuredContents should equal(Nil)
    }
  }

}
