/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.controller

import no.gdl.readingmaterialsapi.{TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite

class HealthControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  lazy val controller = new HealthController
  addServlet(controller, "/")

  test("That /health returns 200 ok") {
    get("/") {
      status should equal (200)
    }
  }

}
