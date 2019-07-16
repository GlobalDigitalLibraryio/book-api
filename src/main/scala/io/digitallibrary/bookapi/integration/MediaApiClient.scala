/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.integration

import com.amazonaws.services.apigateway.model.{GetRestApiRequest, GetRestApisRequest, GetRestApisResult}
import com.amazonaws.services.s3.transfer.Download
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.model.api.NotFoundException
import io.digitallibrary.network.GdlClient

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpRequest, HttpResponse}

import scala.collection.JavaConverters._


trait MediaApiClient {
  this: GdlClient with AmazonClient =>
  val mediaApiClient: MediaApiClient

  class MediaApiClient extends LazyLogging {
    def imageMetaWithId(id: Long, width: Option[Int] = None, format: Option[String] = None): Option[ImageMeta] = {
      val q_params = Map("width" -> width, "format" -> format)
        .collect { case (key, Some(value)) => (key, value.toString) }

      val imageMeta = for {
        endpoint <- endpoint()
        response <- doRequest(Http(s"$endpoint${BookApiProperties.MediaServicePath}/images/$id").params(q_params))
      } yield response

      imageMeta match {
        case Success(x) => Option(x)
        case Failure(ex: Throwable) => {
          logger.error(s"Got ${ex.getClass.getSimpleName} when calling: ${ex.getMessage}")
          None
        }
      }
    }

    def downloadImage(id: Long, width: Option[Int] = None, format: Option[String] = Some("png")): Try[DownloadedImage] = {
      import com.netaporter.uri.dsl._

      imageMetaWithId(id, width, format) match {
        case None => Failure(new NotFoundException(s"Image with id $id was not found"))
        case Some(imageMeta) => for {
          response <- Try(Http(imageMeta.resourceUrl).asBytes)
          contentType <- extractContentType(response)
          fileEnding <- MediaType.fileEndingFor(contentType).map(Success(_)).getOrElse(Failure(new RuntimeException("ContentType not supported")))
          filename <- Success(s"${imageMeta.resourceUrl.pathParts.last.part}")
        } yield DownloadedImage(id, contentType, filename, fileEnding, response.body)
      }
    }

    def extractContentType[A](response: HttpResponse[A]): Try[String] = {
      response.contentType.flatMap(_.split(';').headOption).map(x => Success(x.trim())).getOrElse(Failure(new RuntimeException("Missing content-type for image")))
    }

    private def doRequest(httpRequest: HttpRequest): Try[ImageMeta] = {
      gdlClient.fetch[ImageMeta](httpRequest)
    }

    private def mediaServiceId(): Try[String] = {
      val response: GetRestApisResult = awsApiGatewayClient.getRestApis(new GetRestApisRequest())
      val apis = response.getItems.asScala
      apis.find(_.getName.contains("media-service")).map(_.getId) match {
        case None => Failure(new RuntimeException("Could not find media-service"))
        case Some(x) => Success(x)
      }
    }

    def endpoint(): Try[String] =
      BookApiProperties.Environment match {
        case a if a == "local" => Success("https://api.test.digitallibrary.io")
        case env => mediaServiceId().map(id => s"https://$id.execute-api.eu-central-1.amazonaws.com/$env")
      }
  }


}

case class ImageMeta(id: String, resourceUrl: String, size: Int, contentType: String, alttext: Option[String], imageVariants: Seq[ImageMediaVariant])
case class ImageMediaVariant(revision: Int, ratio: String, x: Int, y: Int, width: Int, height: Int)
object MediaMediaType {
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
