/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration

import com.amazonaws.services.s3.AmazonS3

trait AmazonClient {
  val amazonClient: AmazonS3
}
