/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.integration.ImageApiClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.select.Elements


trait ContentConverter {
  this: ImageApiClient with ConverterService =>
  val contentConverter: ContentConverter

  class ContentConverter extends LazyLogging {
    def toApiContent(content: String): String = {
      val document = Jsoup.parseBodyFragment(content)
      val images: Elements = document.select("embed[data-resource='image']")
      for(i <- 0 until images.size() ) {
        val image = images.get(i)
        val nodeId = image.attr("data-resource_id")

        val url = imageApiClient.imageUrlFor(nodeId.toLong) match {
          case Some(x) => x
          case None => "missing image"
        }

        image.tagName("img")
        image.attr("src", url)
        image.removeAttr("data-resource")
        image.removeAttr("data-resource_id")
      }

      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.select("body").html()
    }
  }
}
