package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.{BookSwagger, TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._



class SourcesControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val swagger: BookSwagger = new BookSwagger
  lazy val controller = new SourceController

  addServlet(controller, "/*")

  override def beforeEach: Unit = {
    reset(readService)
  }

  test("that GET /eng returns a list of sources for a valid language") {
    when(readService.listSourcesForLanguage(any[LanguageTag])).thenReturn(Seq(TestData.Api.DefaultSource))

    get("/eng", headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithAdminReadRole}"))) {
      status should equal(200)
      body should equal("""["storyweaver"]""")
    }
  }

  test("that GET /eng returns a list of sources for a valid language with multiple sources") {
    when(readService.listSourcesForLanguage(any[LanguageTag])).thenReturn(Seq(TestData.Api.DefaultSource, TestData.Api.SecondarySource))

    get("/eng" , headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithAdminReadRole}"))) {
      status should equal(200)
      body should equal("""["storyweaver","bookdash"]""")
    }
  }

  test("that GET /not-valid returns 400 for an invalid language") {
    get("/not-valid", headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithAdminReadRole}"))) {
      status should equal(400)
    }
  }


  test("that GET /eng returns empty list when there is no sources") {
    when(readService.listSourcesForLanguage(any[LanguageTag])).thenReturn(Seq())

    get("/eng", headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithAdminReadRole}"))) {
      status should equal(200)
      body should equal ("[]")
    }
  }

  test("that GET /eng returns 403 when not authorized") {
    when(readService.listSourcesForLanguage(any[LanguageTag])).thenReturn(Seq(TestData.Api.DefaultSource))

    get("/eng", headers = Seq(("Authorization", s"Bearer ${TestData.invalidTestToken}"))) {
      status should equal (403)
    }

  }




}