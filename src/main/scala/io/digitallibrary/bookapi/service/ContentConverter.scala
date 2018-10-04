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
import io.digitallibrary.bookapi.model.domain.ApiContent
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.select.Elements

trait ContentConverter {
  this: ImageApiClient with ConverterService =>
  val contentConverter: ContentConverter

  class ContentConverter extends LazyLogging {
    def toApiContent(content: String): ApiContent = {
      val document = Jsoup.parseBodyFragment(content)
      val images: Elements = document.select("embed[data-resource='image']")
      var imageList: Seq[String] = Seq()
      for (i <- 0 until images.size()) {
        val image = images.get(i)
        val nodeId = image.attr("data-resource_id")
        val imgSize = if (image.hasAttr("data-resource_size")) Some(image.attr("data-resource_size").toInt) else None

        imageApiClient.imageUrlFor(nodeId.toLong, imgSize) match {
          case Some(url) =>
            imageList = imageList :+ url.url
            image.tagName("img")
            image.attr("src", url.url)
            image.attr("crossorigin", "anonymous")
            url.alttext.foreach(alttext => image.attr("alt", alttext))

          case None =>
            image.tagName("p")
            image.html("Image not found")
        }
        image.removeAttr("data-resource")
        image.removeAttr("data-resource_id")
        image.removeAttr("data-resource_size")
      }

      document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.select("body").html()

      ApiContent(document.select("body").html(), imageList)
    }

    def toEPubContent(content: String, downloadedImages: Seq[DownloadedImage]): String = {
      import com.netaporter.uri.dsl._

      val document = Jsoup.parseBodyFragment(content)
      val images: Elements = document.select("embed[data-resource='image']")
      for (i <- 0 until images.size()) {
        val image = images.get(i)
        val nodeId = image.attr("data-resource_id")
        val imgSize = if (image.hasAttr("data-resource_size")) Some(image.attr("data-resource_size").toInt) else None

        val url = downloadedImages.find(_.metaInformation.id == nodeId) match {
          case Some(x) => x.metaInformation.imageUrl.pathParts.last.part
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
