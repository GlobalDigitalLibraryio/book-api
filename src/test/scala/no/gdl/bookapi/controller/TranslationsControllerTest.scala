/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import no.gdl.bookapi.model.api.Language
import no.gdl.bookapi.{BookSwagger, TestEnvironment, UnitSuite}
import org.mockito.Mockito.{times, verify, when}
import org.scalatra.swagger.Swagger
import org.scalatra.test.scalatest.ScalatraFunSuite

class TranslationsControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite{
  implicit val swagger: Swagger = new BookSwagger
  lazy val controller = new TranslationsController

  addServlet(controller, "/*")

  test("that controller returns 200 ok") {
    when(supportedLanguageService.getSupportedLanguages).thenReturn(Seq(Language("amh", "Amharic")))

    get("/supported-languages") {
      status should be (200)
      body should be ("""[{"code":"amh","name":"Amharic"}]""")
    }

    verify(supportedLanguageService, times(1)).getSupportedLanguages
  }

}
