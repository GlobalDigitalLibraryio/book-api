/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.{DownloadedImage, ImageApiClient}
import io.digitallibrary.bookapi.model.api.NotFoundException
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.select.Elements
import org.jsoup.nodes.Document

trait ContentConverter {
  this: ImageApiClient with ConverterService =>
  val contentConverter: ContentConverter

  class ContentConverter extends LazyLogging {
    def toApiContent(content: String): String = {
      val document = Jsoup.parseBodyFragment(content)
      val images: Elements = document.select("embed[data-resource='image']")
      for (i <- 0 until images.size()) {
        val image = images.get(i)
        val nodeId = image.attr("data-resource_id")

        imageApiClient.imageUrlFor(nodeId.toLong) match {
          case Some(url) => {
            image.tagName("picture")

            // For devices larger than 768px
            val forLargeDevice = image.appendElement("source")
            forLargeDevice.attr("media", "(min-width: 768px)")
            forLargeDevice.attr("srcset", url)

            // For devices smaller than 768px
            val smallImage = s"$url?width=300" // 300 should be enough for most small devices
            val doubleImage = s"$url?width=600" // for retina devices (double number of pixels)

            val forSmallDevice = image.appendElement("img")
            forSmallDevice.attr("src", url)
            forSmallDevice.attr("srcset", s"$smallImage, $doubleImage 2x")

          }
          case None => {
            image.tagName("p")
            image.html("Image not found")
          }
        }
        image.removeAttr("data-resource")
        image.removeAttr("data-resource_id")

      }

      document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.select("body").html()
    }

    def toEPubContent(content: String, downloadedImages: Seq[DownloadedImage]): String = {
      import com.netaporter.uri.dsl._

      val document = Jsoup.parseBodyFragment(content)
      val images: Elements = document.select("embed[data-resource='image']")
      for (i <- 0 until images.size()) {
        val image = images.get(i)
        val nodeId = image.attr("data-resource_id")

        val url = downloadedImages.find(_.metaInformation.id == nodeId) match {
          case Some(x) => x.metaInformation.imageUrl.pathParts.last.part
          case None => throw new NotFoundException(s"Could not find image for id $nodeId")
        }

        image.tagName("img")
        image.attr("src", url)
        image.removeAttr("data-resource")
        image.removeAttr("data-resource_id")
      }

      document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.select("body").html()
    }
  }

}
