/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.Category
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

class CategoryRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val categoryRepository = new CategoryRepository

  test("That category is added") {
    withRollback { implicit session =>
      val testName = "Some-test-name"

      categoryRepository.add(Category(None, None, testName))
      val category = categoryRepository.withName(testName)

      category.isDefined should be(true)
      category.head.id.isDefined should be(true)
      category.head.revision.isDefined should be(true)
      category.head.name should equal(testName)
    }
  }

  test("that withName returns None when no Categories with given name") {
    val withName = categoryRepository.withName(s"some-category-${System.currentTimeMillis()}")
    withName should be(None)
  }

}
