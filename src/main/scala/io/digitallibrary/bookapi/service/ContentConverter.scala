/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.integration.{DownloadedImage, ImageApiClient}
import io.digitallibrary.bookapi.model.api.NotFoundException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.select.Elements

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
        val imgSize = if (image.hasAttr("data-resource_size")) Some(image.attr("data-resource_size").toInt) else None

        imageApiClient.imageUrlFor(nodeId.toLong) match {
          case Some(url) =>
            image.tagName("picture")

            val fullSizeUrl = if (imgSize.isDefined) s"$url?width=${imgSize.get}" else url
            val halfSize = imgSize.map(size => size / 2)

            // For devices larger than 768px
            val forLargeDevice = image.appendElement("source")
            forLargeDevice.attr("media", "(min-width: 768px)")
            forLargeDevice.attr("srcset", fullSizeUrl)

            // For devices smaller than 768px
            val smallImage = s"$url?width=${halfSize.getOrElse(300)}" // 300 should be enough for most small devices

            val forSmallDevice = image.appendElement("img")
            forSmallDevice.attr("src", fullSizeUrl)
            forSmallDevice.attr("srcset", s"$smallImage")

            imageApiClient.imageMetaWithId(nodeId.toLong).flatMap(_.alttext).map(_.alttext).map(
              text => forSmallDevice.attr("alt", text))

          case None =>
            image.tagName("p")
            image.html("Image not found")
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
        val imgSize = if (image.hasAttr("data-resource_size")) Some(image.attr("data-resource_size").toInt) else None

        val url = downloadedImages.find(_.id.toString == nodeId) match {
          case Some(x) => x.filename
          case None => throw new NotFoundException(s"Could not find image for id $nodeId")
        }

        imgSize.foreach(size => image.attr("width", size.toString))
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
