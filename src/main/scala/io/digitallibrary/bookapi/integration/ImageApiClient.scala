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
import scalaj.http.{Http, HttpRequest, HttpResponse}


trait ImageApiClient {
  this: GdlClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient extends LazyLogging {

    def downloadImage(id: Long, width: Option[Int] = None): Try[DownloadedImage] = {
      import com.netaporter.uri.dsl._

      imageUrlFor(id, width) match {
        case None => Failure(new NotFoundException(s"Image with id $id was not found"))
        case Some(url) => for {
          response <- Try(Http(url.url).asBytes)
          contentType <- extractContentType(response)
          fileEnding <- MediaType.fileEndingFor(contentType).map(Success(_)).getOrElse(Failure(new RuntimeException("ContentType not supported")))
          fileName <- Success(s"${url.url.pathParts.last.part}.$fileEnding")
        } yield DownloadedImage(id, contentType, fileName, response.body)
      }
    }

    def extractContentType[A](response: HttpResponse[A]): Try[String] = {
      response.contentType.flatMap(_.split(';').headOption).map(x => Success(x.trim())).getOrElse(Failure(new RuntimeException("Missing content-type for image")))
    }

    def imageUrlFor(id: Long, widthOpt: Option[Int] = None): Option[ImageUrl] = {
      val widthAppendix = widthOpt match {
        case None => ""
        case Some(width) => s"?width=$width"
      }

      val request = Http(s"http://${BookApiProperties.InternalImageApiUrl}/$id/imageUrl$widthAppendix")
      gdlClient.fetch[ImageUrl](request) match {
        case Success(imageUrl) => Some(imageUrl)
        case Failure(ex: Throwable) =>
          logger.error(s"Got ${ex.getClass.getSimpleName} when calling ${request.url}: ${ex.getMessage}")
          None
      }
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
case class ImageUrl(id: String, url: String, alttext: Option[String])
case class DownloadedImage(id: Long, contentType: String, filename: String, bytes: Array[Byte])
object MediaType {
  private val mediaTypeToFileEnding = Map(
    "application/xhtml+xml" -> "xhtml",
    "image/jpeg" -> "jpg",
    "image/jpg" -> "jpg",
    "image/gif" -> "gif",
    "image/png" -> "png",
    "image/webp" -> "webp",
    "text/css" -> "css",
    "application/epub+zip" -> "epub"
  )

  def fileEndingFor(mediaType: String): Option[String] = {
    mediaTypeToFileEnding.find(_._1.equalsIgnoreCase(mediaType)).map(_._2)
  }
}
