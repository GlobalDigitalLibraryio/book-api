/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.integration

import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.aws.Aws4ElasticClient
import com.sksamuel.elastic4s.http.HttpClient
import no.gdl.bookapi.BookApiProperties

trait ElasticClient {
  val esClient: HttpClient
}

object EsClientFactory {
  def getClient(searchServer: String = BookApiProperties.SearchServer): HttpClient = {
    BookApiProperties.RunWithSignedSearchRequests match {
      case true => HttpClient(ElasticsearchClientUri(searchServer))
      case false => nonSigningClient(searchServer)
    }
  }

  private def nonSigningClient(searchServer: String): HttpClient = {
    HttpClient(ElasticsearchClientUri(searchServer))
  }

  private def signingClient(searchServer: String): HttpClient = {
    Aws4ElasticClient(searchServer)
  }
}