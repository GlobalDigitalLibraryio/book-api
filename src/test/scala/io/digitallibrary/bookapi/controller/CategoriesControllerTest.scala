/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.{BookSwagger, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.mockito.Mockito.{verify, when}
import org.scalatra.test.scalatest.ScalatraFunSuite

class CategoriesControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new CategoriesController
  implicit val swagger: BookSwagger = new BookSwagger

  override def beforeEach: Unit = {
    resetMocks()
  }

  addServlet(controller, "/*")

  test("that / defaults to english language") {
    get("/") {}
    verify(readService).listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))
  }

  test("that /eng returns 200 ok with empty result set for language with no books") {
    when(readService.listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))).thenReturn(Map.empty[String, Set[String]])
    get("/eng") {
      status should equal(200)
      body should equal("{}")
    }
    verify(readService).listAvailablePublishedCategoriesForLanguage(LanguageTag("eng"))
  }

}
