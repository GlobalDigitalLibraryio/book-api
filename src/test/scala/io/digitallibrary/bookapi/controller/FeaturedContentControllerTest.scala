package io.digitallibrary.bookapi.controller

import java.text.SimpleDateFormat

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api.{FeaturedContent, FeaturedContentId, Language, LocalDateSerializer}
import io.digitallibrary.bookapi.{BookSwagger, TestEnvironment, UnitSuite}
import org.json4s.native.Serialization._
import org.json4s.{DefaultFormats, Formats}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.{Failure, Success}

class FeaturedContentControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite with BeforeAndAfterAll {

  implicit val formats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  } + LocalDateSerializer

  val validTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJzY29wZSI6ImJvb2tzLWxvY2FsOmZlYXR1cmVkIn0.lvUkAaez_uJzxFG4GJeXxKOdmMdqN3oNJttMYsozkzs"
  val invalidTestToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RpZ2l0YWxsaWJyYXJ5LmlvL2dkbF9pZCI6IjEyMyIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJzY29wZSI6ImJvb2tzLWxvY2FsOndyb25nS2luZE9mUm9sZSJ9.ixzLjZaS3EdTrbRk2UDDe-QTQpsy9hqLlgJnopsT0YI"

  val mockData = Seq(FeaturedContent(id = 1, revision = 1, language = Language(code = "eng", name = "English"), title = "", description = "", link = "", imageUrl = "", category = None))

  implicit val swagger: BookSwagger = new BookSwagger
  lazy val controller = new FeaturedContentController
  addServlet(controller, "/*")

  override def beforeEach: Unit = {
    reset(readService, writeService)
  }

  test("that GET /eng retrieves all featured content for language=eng") {
    when(readService.featuredContentForLanguage(LanguageTag("eng"))).thenReturn(mockData)
    get("/eng") {
      status should equal(200)
      verify(readService).featuredContentForLanguage(LanguageTag("eng"))
      read[Seq[FeaturedContent]](body) should equal(mockData)
    }
  }

  test("that GET /aaa retrieves all featured content for language=eng because language=aaa has no featured content") {
    when(readService.featuredContentForLanguage(LanguageTag("aaa"))).thenReturn(Nil)
    when(readService.featuredContentForLanguage(LanguageTag("eng"))).thenReturn(mockData)
    get("/aaa") {
      status should equal(200)
      verify(readService, Mockito.times(2)).featuredContentForLanguage(any[LanguageTag])
      read[Seq[FeaturedContent]](body) should equal(mockData)
    }
  }

  test("that POST / adds new featured content") {
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
    post("/", payload, headers = Seq(("Authorization", s"Bearer $validTestToken"))) {
      status should equal(201)
      read[FeaturedContentId](body) should equal(FeaturedContentId(1))
    }
  }

  test("that PUT / updates an existing featured content") {
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
    put("/", payload, headers = Seq(("Authorization", s"Bearer $validTestToken"))) {
      status should equal(200)
      read[FeaturedContentId](body) should equal(FeaturedContentId(1))
      verify(writeService).updateFeaturedContent(any[FeaturedContent])
    }

  }

  test("that DELETE /123 deletes existing featured content with id=123") {
    when(writeService.deleteFeaturedContent(123)).thenReturn(Success())
    delete("/123", headers = Seq(("Authorization", s"Bearer $validTestToken"))) {
      status should equal(200)
      verify(writeService).deleteFeaturedContent(123)
    }
  }

  test("that DELETE /1 returns 404 when content was not deleted") {
    when(writeService.deleteFeaturedContent(1)).thenReturn(Failure(new RuntimeException("")))
    delete("/1", headers = Seq(("Authorization", s"Bearer $validTestToken"))) {
      status should equal(404)
      verify(writeService).deleteFeaturedContent(1)
    }
  }

  test("that POST / isn't allowed if token is missing") {
    post("/", "".getBytes()) {
      status should equal(403)
    }
  }

  test("that POST / isn't allowed if token has incorrect role") {
    post("/", "".getBytes(), headers = Seq(("Authorization", s"Bearer $invalidTestToken"))) {
      status should equal(403)
    }
  }

}
