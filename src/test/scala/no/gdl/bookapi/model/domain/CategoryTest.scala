/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}

class CategoryTest extends IntegrationSuite with TestEnvironment {

  test("That category is added") {
    withRollback { implicit session =>
      val testName = "Some-test-name"

      Category.add(Category(None, None, testName))
      val category = Category.withName(testName)

      category.isDefined should be(true)
      category.head.id.isDefined should be(true)
      category.head.revision.isDefined should be(true)
      category.head.name should equal(testName)
    }
  }

  test("that withName returns None when no Categories with given name") {
    val withName = Category.withName(s"some-category-${System.currentTimeMillis()}")
    withName should be(None)
  }

  test("That withName returns the first occurence of category with that name") {
    withRollback { implicit session =>
      val testName = s"Some-category-${System.currentTimeMillis()}"
      val categoryDef = Category(None, None, testName)

      val cat1 = Category.add(categoryDef)
      val cat2 = Category.add(categoryDef)
      val cat3 = Category.add(categoryDef)

      val withName = Category.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.id.get should equal(cat1.id.get)
    }
  }
}
