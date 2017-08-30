/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.integration

import java.time.{LocalDateTime, ZoneOffset}

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.google.common.base.Supplier
import io.searchbox.action.Action
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.client.{JestClient, JestResult}
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.GdlSearchException
import org.apache.http.impl.client.{DefaultHttpRequestRetryHandler, HttpClientBuilder}
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import vc.inreach.aws.request.{AWSSigner, AWSSigningRequestInterceptor}

import scala.util.{Failure, Success, Try}

trait ElasticClient {
  val jestClient: NdlaJestClient
}

class NdlaJestClient(jestClient: JestClient) {
  def execute[T <: JestResult](clientRequest: Action[T]): Try[T] = {
    for {
      jestResponse <- Try(jestClient.execute(clientRequest))
      elasticResult <- if (jestResponse.isSucceeded) Success(jestResponse) else Failure(new GdlSearchException(jestResponse))
    } yield elasticResult
  }
}

object JestClientFactory {
  def getClient(searchServer: String = BookApiProperties.SearchServer): NdlaJestClient = {
    BookApiProperties.RunWithSignedSearchRequests match {
      case true => getSigningClient(searchServer)
      case false => getNonSigningClient(searchServer)
    }
  }

  private def getNonSigningClient(searchServer: String): NdlaJestClient = {
    val factory = new io.searchbox.client.JestClientFactory()
    factory.setHttpClientConfig(new HttpClientConfig.Builder(searchServer).readTimeout(60000).multiThreaded(true).build())
    new NdlaJestClient(factory.getObject)
  }

  private def getSigningClient(searchServer: String): NdlaJestClient = {
    val clock: Supplier[LocalDateTime] = new Supplier[LocalDateTime] {
      override def get(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
    }

    val awsSigner = new AWSSigner(new DefaultAWSCredentialsProviderChain(), BookApiProperties.SearchRegion, "es", clock)
    val requestInterceptor = new AWSSigningRequestInterceptor(awsSigner)

    val factory = new io.searchbox.client.JestClientFactory() {
      override def configureHttpClient(builder: HttpClientBuilder): HttpClientBuilder = {
        builder.addInterceptorLast(requestInterceptor)
        builder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
        builder
      }

      override def configureHttpClient(builder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
        builder.addInterceptorLast(requestInterceptor)
        builder
      }
    }

    factory.setHttpClientConfig(new HttpClientConfig.Builder(searchServer).readTimeout(60000).multiThreaded(true).build())
    new NdlaJestClient(factory.getObject)
  }
}
