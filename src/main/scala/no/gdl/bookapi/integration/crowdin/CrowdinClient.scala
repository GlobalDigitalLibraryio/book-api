/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration.crowdin

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.GdlClient
import no.gdl.bookapi.model.crowdin.SupportedLanguage

import scala.util.Try
import scalaj.http.Http

class CrowdinClient extends GdlClient with LazyLogging {
  override val gdlClient = new GdlClient

  protected val CrowdinBaseUrl = "https://api.crowdin.com/api"
  protected val SupportedLanguagesUrl = s"$CrowdinBaseUrl/supported-languages?json"

  def getSupportedLanguages: Try[Seq[SupportedLanguage]] =
    gdlClient.fetch[Seq[SupportedLanguage]](Http(SupportedLanguagesUrl))
}

