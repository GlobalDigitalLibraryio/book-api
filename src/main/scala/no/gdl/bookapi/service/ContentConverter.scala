/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.integration.{DownloadedImage, ImageApiClient}
import no.gdl.bookapi.model.api.NotFoundException
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

        val url = imageApiClient.imageUrlFor(nodeId.toLong) match {
          case Some(x) => x
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
