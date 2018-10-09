/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import io.digitallibrary.bookapi.integration.{Alttext, DownloadedImage, ImageMetaInformation, ImageUrl}
import io.digitallibrary.bookapi.model.api.NotFoundException
import io.digitallibrary.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.mockito.ArgumentMatchers.{eq => eqTo}

class ContentConverterTest extends UnitSuite with TestEnvironment {

  val service = new ContentConverter

  test("that toApiContent converts embed-tags to image-tags correctly") {
    when(imageApiClient.imageUrlFor(eqTo(1L), any[Option[Int]])).thenReturn(Some(ImageUrl("1", "image-url-1", None)))
    when(imageApiClient.imageUrlFor(eqTo(2L), any[Option[Int]])).thenReturn(Some(ImageUrl("2", "image-url-2", Some("Some alt text"))))
    when(imageApiClient.imageUrlFor(eqTo(3L), any[Option[Int]])).thenReturn(None)

    val content =
      """
        |<p>
        |<embed data-resource="image" data-resource_id="1"/>
        |<embed data-resource="image" data-resource_id="2"/>
        |<embed data-resource="image" data-resource_id="3"/>
        |</p>
      """.stripMargin

    val expectedApiContent =
      """
        |<p>
        |<img src="image-url-1" crossorigin="anonymous" />
        |<img src="image-url-2" crossorigin="anonymous" alt="Some alt text" />
        |<p>Image not found</p>
        |</p>
      """.stripMargin

    val apiContent = service.toApiContent(content).content
    apiContent should equal (expectedApiContent)
  }

  test("that toEPubContent throws exception if an image is missing") {
    val dummyBytes = "Hello, I am bytes".getBytes
    val imageMeta1 = ImageMetaInformation("1", "some-url", "some-url", 123, "image/jpeg", None, None)
    val imageMeta2 = ImageMetaInformation("2", "some-url", "some-url", 123, "image/jpeg", None, None)

    val content =
      """
        |<p>
        |<embed data-resource="image" data-resource_id="1"/>
        |<embed data-resource="image" data-resource_id="2"/>
        |<embed data-resource="image" data-resource_id="3"/>
        |</p>
      """.stripMargin

    val images = Seq(DownloadedImage(1, "image/jpeg", "some-url.jpg", dummyBytes), DownloadedImage(2, "image/jpeg", "some-url.jpg", dummyBytes))
    intercept[NotFoundException] {
      service.toEPubContent(content, images)
    }.getMessage should equal ("Could not find image for id 3")
  }

  test("that to EPubContent converts embed-tags to image-tags correctly") {
    val dummyBytes = "Hello, I am bytes".getBytes
    val imageMeta1 = ImageMetaInformation("1", "meta-url", "some-url-1", 123, "image/jpeg", None, None)
    val imageMeta2 = ImageMetaInformation("2", "meta-url", "some-url-2", 123, "image/jpeg", None, None)

    val content =
      """
        |<p>
        |<embed data-resource="image" data-resource_id="1"/>
        |<embed data-resource="image" data-resource_id="2"/>
        |</p>
      """.stripMargin

    val expectedEpubContent =
      """
        |<p>
        |<img src="some-url-1.jpg" />
        |<img src="some-url-2.jpg" />
        |</p>
      """.stripMargin

    val images = Seq(DownloadedImage(1, "image/jpeg", "some-url-1.jpg", dummyBytes), DownloadedImage(2, "image/jpeg", "some-url-2.jpg", dummyBytes))

    val epubContent = service.toEPubContent(content, images)
    epubContent should equal (expectedEpubContent)

  }

}
