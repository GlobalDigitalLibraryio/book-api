/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.integration

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.GdlClient
import io.digitallibrary.network.model.HttpRequestException
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.Domain
import com.netaporter.uri.dsl._
import io.digitallibrary.bookapi.model.api.NotFoundException

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpRequest}


trait ImageApiClient {
  this: GdlClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends LazyLogging {
    def downloadImage(id: Long): Try[DownloadedImage] = {
      imageMetaWithId(id) match {
        case None => Failure(new NotFoundException(s"Image with id $id was not found"))
        case Some(imageMetaInformation) =>
          gdlClient.fetchBytes(Http(imageMetaInformation.imageUrl))
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

case class ImageMetaInformation(id: String, metaUrl: String, imageUrl: String, size: Int, contentType: String)

case class DownloadedImage(metaInformation: ImageMetaInformation, bytes: Array[Byte])
