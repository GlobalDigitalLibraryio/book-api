/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.bookapi.model.domain.Category
import io.digitallibrary.bookapi.{IntegrationSuite, TestEnvironment}

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

  test("That withName returns the first occurence of category with that name") {
    withRollback { implicit session =>
      val testName = s"Some-category-${System.currentTimeMillis()}"
      val categoryDef = Category(None, None, testName)

      val cat1 = categoryRepository.add(categoryDef)
      val cat2 = categoryRepository.add(categoryDef)
      val cat3 = categoryRepository.add(categoryDef)

      val withName = categoryRepository.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.id.get should equal(cat1.id.get)
    }
  }
}
