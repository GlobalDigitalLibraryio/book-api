/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration.crowdin

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.SourceLanguageNotSupportedException

import scala.util.{Failure, Success, Try}

trait CrowdinClientBuilder {
  val crowdinClientBuilder: CrowdinClientBuilder

  class CrowdinClientBuilder extends LazyLogging {
    def forSourceLanguage(sourceLanguage: LanguageTag): Try[CrowdinClient] = {
      BookApiProperties.CrowdinProjects.find(_.sourceLanguage == sourceLanguage.toString) match {
        case Some(project) =>
          Success(new CrowdinClient(project.sourceLanguage, project.projectIdentifier, project.projectKey))

        case None =>
          Failure(new SourceLanguageNotSupportedException(sourceLanguage))
      }
    }

    def withGenericAccess: LimitedCrowdinClient = {
      new LimitedCrowdinClient
    }
  }
}
