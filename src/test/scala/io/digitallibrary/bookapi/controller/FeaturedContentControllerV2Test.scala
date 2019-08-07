package io.digitallibrary.bookapi.controller

import java.text.SimpleDateFormat

import io.digitallibrary.bookapi.{BookSwagger, TestData, TestEnvironment, UnitSuite}
import io.digitallibrary.bookapi.model.api.{FeaturedContent, FeaturedContentId, FeaturedContentV2, Language}
import io.digitallibrary.language.model.LanguageTag
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization.read
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class FeaturedContentControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite with BeforeAndAfterAll {

  implicit val formats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  }

  val mockData = Seq(FeaturedContentV2(id = 1, revision = 1, language = Language(code = "eng", name = "English"), title = "", description = "", link = "", imageUrl = "", category = None))

  implicit val swagger: BookSwagger = new BookSwagger
  lazy val controllerV2 = new FeaturedContentControllerV2
  addServlet(controllerV2, "/*")

  override def beforeEach: Unit = {
    reset(readServiceV2, writeService)
  }

  test("v2: that GET /eng retrieves all featured content for language=eng") {
    when(readServiceV2.featuredContentForLanguage(LanguageTag("eng"))).thenReturn(mockData)
    get("/eng") {
      status should equal(200)
      verify(readServiceV2).featuredContentForLanguage(LanguageTag("eng"))
      read[Seq[FeaturedContentV2]](body) should equal(mockData)
    }
  }

  test("v2: that GET /aaa retrieves all featured content for language=eng because language=aaa has no featured content") {
    when(readServiceV2.featuredContentForLanguage(LanguageTag("aaa"))).thenReturn(Nil)
    when(readServiceV2.featuredContentForLanguage(LanguageTag("eng"))).thenReturn(mockData)
    get("/aaa") {
      status should equal(200)
      verify(readServiceV2, Mockito.times(2)).featuredContentForLanguage(any[LanguageTag])
      read[Seq[FeaturedContentV2]](body) should equal(mockData)
    }
  }

  test("v2: that POST / adds new featured content") {
    when(writeService.newFeaturedContent(any[NewFeaturedContent])).thenReturn(Success(FeaturedContentId(1)))
    val payload =
      """
        | { "language": "eng",
        |   "title": "my title",
        |   "description": "my description",
        |   "link": "http://example.com/",
        |   "imageUrl": "https://www.iana.org/_img/2013.1/iana-logo-header.svg"
        | }
      """.stripMargin.getBytes
    post("/", payload, headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithFeaturedRole}"))) {
      status should equal(201)
      read[FeaturedContentId](body) should equal(FeaturedContentId(1))
    }
  }

  test("v2: that PUT / updates an existing featured content") {
    when(writeService.updateFeaturedContent(any[FeaturedContent])).thenReturn(Success(FeaturedContentId(1)))
    val payload =
      """
        | { "id": 1,
        |   "revision": 1,
        |   "language": {
        |       "code": "eng",
        |       "name": "English"
        |   },
        |   "title": "my title",
        |   "description": "my updated description",
        |   "link": "http://example.com/",
        |   "imageUrl": "https://www.iana.org/_img/2013.1/iana-logo-header.svg"
        | }
      """.stripMargin.getBytes
    put("/", payload, headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithFeaturedRole}"))) {
      status should equal(200)
      read[FeaturedContentId](body) should equal(FeaturedContentId(1))
      verify(writeService).updateFeaturedContent(any[FeaturedContent])
    }

  }

  test("v2: that DELETE /123 deletes existing featured content with id=123 and content-type text/plain") {
    when(writeService.deleteFeaturedContent(123)).thenReturn(Success())
    delete("/123", headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithFeaturedRole}"))) {
      status should equal(200)
      verify(writeService).deleteFeaturedContent(123)
      header("Content-Type").contains("text/plain") should be(true)
    }
  }

  test("v2: that DELETE /1 returns 404 when content was not deleted") {
    when(writeService.deleteFeaturedContent(1)).thenReturn(Failure(new RuntimeException("")))
    delete("/1", headers = Seq(("Authorization", s"Bearer ${TestData.validTestTokenWithFeaturedRole}"))) {
      status should equal(404)
      verify(writeService).deleteFeaturedContent(1)
    }
  }

  test("v2: that POST / isn't allowed if token is missing") {
    post("/", "".getBytes()) {
      status should equal(403)
    }
  }

  test("v2: that POST / isn't allowed if token has incorrect role") {
    post("/", "".getBytes(), headers = Seq(("Authorization", s"Bearer ${TestData.invalidTestToken}"))) {
      status should equal(403)
    }
  }

}
