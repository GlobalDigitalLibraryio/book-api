/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import io.digitallibrary.bookapi.integration.{Alttext, DownloadedImage, ImageMetaInformation}
import io.digitallibrary.bookapi.model.api.NotFoundException
import io.digitallibrary.bookapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class ContentConverterTest extends UnitSuite with TestEnvironment {

  val service = new ContentConverter

  test("that toApiContent converts embed-tags to image-tags correctly") {
    when(imageApiClient.imageUrlFor(1)).thenReturn(Some("image-url-1"))
    when(imageApiClient.imageUrlFor(2)).thenReturn(Some("image-url-2"))
    when(imageApiClient.imageUrlFor(3)).thenReturn(None)
    when(imageApiClient.imageMetaWithId(1)).thenReturn(None)
    when(imageApiClient.imageMetaWithId(2)).thenReturn(Some(ImageMetaInformation("2", "", "", 0, "", Some(Alttext("Some alt text", "en")), None)))
    when(imageApiClient.imageMetaWithId(3)).thenReturn(None)

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
        |<picture><source media="(min-width: 768px)" srcset="image-url-1" /><img src="image-url-1" crossorigin="anonymous" srcset="image-url-1?width=300" /></picture>
        |<picture><source media="(min-width: 768px)" srcset="image-url-2" /><img src="image-url-2" crossorigin="anonymous" srcset="image-url-2?width=300" alt="Some alt text" /></picture>
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

    val images = Seq(DownloadedImage(imageMeta1, dummyBytes), DownloadedImage(imageMeta2, dummyBytes))
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
        |<img src="some-url-1" />
        |<img src="some-url-2" />
        |</p>
      """.stripMargin

    val images = Seq(DownloadedImage(imageMeta1, dummyBytes), DownloadedImage(imageMeta2, dummyBytes))

    val epubContent = service.toEPubContent(content, images)
    epubContent should equal (expectedEpubContent)

  }

}
