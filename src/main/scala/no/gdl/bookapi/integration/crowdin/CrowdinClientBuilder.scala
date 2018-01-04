/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration.crowdin

import com.typesafe.scalalogging.LazyLogging

trait CrowdinClientBuilder {
  val crowdinClientBuilder: CrowdinClientBuilder

  class CrowdinClientBuilder extends LazyLogging {
    def withGenericAccess: CrowdinClient = {
      new CrowdinClient
    }
  }
}
