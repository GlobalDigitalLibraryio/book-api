package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.model.api.{Language, LocalDateSerializer}
import io.digitallibrary.bookapi.{BookSwagger, TestEnvironment, UnitSuite}
import io.digitallibrary.language.model.LanguageTag
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

class LanguageControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  implicit val formats: Formats = DefaultFormats + LocalDateSerializer

  implicit val swagger: BookSwagger = new BookSwagger

  lazy val controller = new LanguageController

  addServlet(controller, "/*")

  test("that GET /en will get information about English") {
    when(converterService.toApiLanguage(languageTag = LanguageTag("en"))).thenCallRealMethod()

    get("/en") {
      status should equal (200)
      val language = read[Language](body)
      language.name should be("English")
    }
  }

  test("that GET /sgn-kh will get information about Cambodian Sign Language") {
    when(converterService.toApiLanguage(languageTag = LanguageTag("sgn-kh"))).thenCallRealMethod()

    get("/sgn-kh") {
      status should equal (200)
      val language = read[Language](body)
      language.name should be("Sign Languages (Cambodia)")
    }
  }

}
