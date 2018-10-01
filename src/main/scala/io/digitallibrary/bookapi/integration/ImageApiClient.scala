/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.integration

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.model.api.NotFoundException
import io.digitallibrary.network.GdlClient

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpRequest}


trait ImageApiClient {
  this: GdlClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends LazyLogging {
    def downloadImage(id: Long, width: Option[Int] = None): Try[DownloadedImage] = {
      imageMetaWithId(id) match {
        case None => Failure(new NotFoundException(s"Image with id $id was not found"))
        case Some(imageMetaInformation) =>
          val imageUrl = if (width.isDefined) s"${imageMetaInformation.imageUrl}?width=${width.get}" else imageMetaInformation.imageUrl
          gdlClient.fetchBytes(Http(imageUrl))
            .map(bytes => DownloadedImage(imageMetaInformation, bytes))
      }
    }

    def imageUrlFor(id: Long): Option[String] = {
      imageMetaWithId(id).map(_.imageUrl)
    }

    def imageMetaWithId(id: Long): Option[ImageMetaInformation] = doRequest(
      Http(s"http://${BookApiProperties.InternalImageApiUrl}/$id"))

    private def doRequest(httpRequest: HttpRequest): Option[ImageMetaInformation] = {
      gdlClient.fetch[ImageMetaInformation](httpRequest) match {
        case Success(metaInfo) => Some(metaInfo)
        case Failure(ex: Throwable) =>  {
          logger.error(s"Got ${ex.getClass.getSimpleName} when calling ${httpRequest.url}: ${ex.getMessage}")
          None
        }
      }
    }
  }

}

case class ImageMetaInformation(id: String, metaUrl: String, imageUrl: String, size: Int, contentType: String, alttext: Option[Alttext], imageVariants: Option[Map[String, ImageVariant]])
case class ImageVariant(ratio: String, revision: Option[Int], topLeftX: Int, topLeftY: Int, width: Int, height: Int)
case class Alttext(alttext: String, language: String)

case class DownloadedImage(metaInformation: ImageMetaInformation, bytes: Array[Byte])
