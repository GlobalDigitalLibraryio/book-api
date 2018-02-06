/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.aws.{Aws4ElasticClient, Aws4ElasticConfig}
import com.sksamuel.elastic4s.http.{HttpClient, HttpExecutable, RequestSuccess}
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.GdlSearchException

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait ElasticClient {
  val esClient: E4sClient
}

case class E4sClient(httpClient: HttpClient) {
  def execute[T, U](request: T)(implicit exec: HttpExecutable[T, U]): Try[RequestSuccess[U]] = {
    val response = Await.ready(httpClient.execute {
      request
    }, Duration.Inf).value.get

    response match {
      case Success(either) => either match {
        case Right(result) => Success(result)
        case Left(requestFailure) => Failure(new GdlSearchException(requestFailure))
      }
      case Failure(ex) => Failure(ex)
    }
  }
}

object EsClientFactory {
  def getClient(searchServer: String = BookApiProperties.SearchServer): E4sClient = {
    BookApiProperties.RunWithSignedSearchRequests match {
      case true => E4sClient(signingClient(searchServer))
      case false => E4sClient(nonSigningClient(searchServer))
    }
  }

  private def nonSigningClient(searchServer: String): HttpClient = {
    HttpClient(ElasticsearchClientUri(searchServer))
  }

  private def signingClient(searchServer: String): HttpClient = {
    val awsRegion = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_CENTRAL_1)).toString
    val provider = new DefaultAWSCredentialsProviderChain
    val config = Aws4ElasticConfig(searchServer, provider.getCredentials.getAWSAccessKeyId, provider.getCredentials.getAWSSecretKey, awsRegion)

    Aws4ElasticClient(config)
  }
}