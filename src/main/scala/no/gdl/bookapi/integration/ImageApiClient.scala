/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.GdlClient
import io.digitallibrary.network.model.HttpRequestException
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.BookApiProperties.Domain
import com.netaporter.uri.dsl._
import scala.util.{Failure, Success}
import scalaj.http.{Http, HttpRequest}


trait ImageApiClient {
  this: GdlClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends LazyLogging {
    def imageUrlFor(id: Long): Option[String] = {
      imageMetaWithId(id).map(x => s"$Domain${x.imageUrl.path}")
    }

    def imageMetaWithId(id: Long): Option[ImageMetaInformation] = doRequest(
      Http(s"http://${BookApiProperties.InternalImageApiUrl}/$id"))

    private def doRequest(httpRequest: HttpRequest): Option[ImageMetaInformation] = {
      gdlClient.fetch[ImageMetaInformation](httpRequest) match {
        case Success(metaInfo) => Some(metaInfo)
        case Failure(hre: HttpRequestException) => if (hre.is404) None else throw hre
        case Failure(ex: Throwable) => throw ex
      }
    }
  }
}
case class ImageMetaInformation(id: String, metaUrl: String, imageUrl: String, size: Int, contentType: String)
