package io.digitallibrary.bookapi.integration

import io.digitallibrary.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import scalaj.http.HttpResponse

import scala.util.Success

class ImageApiClientTest extends UnitSuite with TestEnvironment {

  val client = new ImageApiClient

  test("parsing ContentType works with only contentType") {
    val responseMock = mock[HttpResponse[Unit]]
    when(responseMock.contentType).thenReturn(Some("image/png"))
    val contentType = client.extractContentType(responseMock)
    contentType should equal (Success("image/png"))
  }

  test("that parsing ContentType returns contentType without charset and boundary when defined") {
    val responseMock = mock[HttpResponse[Unit]]
    when(responseMock.contentType).thenReturn(Some("image/jpeg; charset=UTF8; boundary=something"))
    val contentType = client.extractContentType(responseMock)
    contentType should equal (Success("image/jpeg"))
  }

}
