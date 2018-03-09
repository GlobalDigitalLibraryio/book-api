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
  override val licenseRepository = new LicenseRepository
  override val categoryRepository = new CategoryRepository
  override val translationRepository = new TranslationRepository

  test("that forLanguage returns an empty list when no featured content for any languages") {
    val featuredContents = featuredContentRepository.forLanguage(LanguageTag("eng"))
    featuredContents should equal(Nil)
  }

  test("that forLanguage returns an empty list when no featured content for given language") {
    withRollback { implicit session =>
      featuredContentRepository.addContent(FeaturedContent(None, None, LanguageTag("eng"), "Some title 1", "Some description", "http://example.com", "http://example.com/example.png"))
      val featuredContents = featuredContentRepository.forLanguage(LanguageTag("nob"))
      featuredContents should equal(Nil)
    }
  }

}
