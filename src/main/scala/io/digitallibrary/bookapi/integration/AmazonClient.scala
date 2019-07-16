/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.integration

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.apigateway.{AmazonApiGateway, AmazonApiGatewayClient}

trait AmazonClient {
  val amazonS3Client: AmazonS3
  val awsApiGatewayClient: AmazonApiGateway

}
