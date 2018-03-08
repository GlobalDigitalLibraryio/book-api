/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service.translation

import com.github.blemale.scaffeine.Scaffeine
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.integration.crowdin.CrowdinClientBuilder
import io.digitallibrary.bookapi.model.api.Language

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait SupportedLanguageService {
  this: CrowdinClientBuilder =>
  val supportedLanguageService: SupportedLanguageService

  class SupportedLanguageService extends LazyLogging {

    private val DefaultKey = "getSupportedLanguages"
    private val cache = Scaffeine()
      .expireAfterWrite(24.hours)
      .build[String, Seq[Language]]()

    def getSupportedLanguages: Seq[Language] = {
      cache.getIfPresent(DefaultKey) match {
        case Some(x) => x
        case None =>
          val supportedLanguages = loadSupportedLanguages.sortBy(_.name)
          cache.put(DefaultKey, supportedLanguages)
          supportedLanguages
      }
    }

    private def loadSupportedLanguages = crowdinClientBuilder.withGenericAccess.getSupportedLanguages match {
      case Success(supportedLanguages) => supportedLanguages.flatMap(supportedLanguage => {
        Try(LanguageTag(supportedLanguage.crowdinCode)) match {
          case Success(validLanguage) => Some(Language(supportedLanguage.crowdinCode, validLanguage.displayName))
          case Failure(_) => None
        }
      }).distinct
      case Failure(ex) => throw ex
    }
  }
}
