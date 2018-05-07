/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */
package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.{BookSwagger, TestData, TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite

class ExportControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val swagger = new BookSwagger

  lazy val controller = new ExportController
  addServlet(controller, "/*")

  test("that /export/en/all with unauthorized gives 403") {
    get("/en/all", headers = Seq (("Authorization", s"Bearer ${TestData.invalidTestToken}") ) ) {
      status should equal (403)
    }
  }

  test("that /export/en/all with authorized gives 200") {
    get("/en/all", headers = Seq (("Authorization", s"Bearer ${TestData.validTestTokenWithWriteRole}") ) ) {
      status should equal (200)
    }
  }


}
