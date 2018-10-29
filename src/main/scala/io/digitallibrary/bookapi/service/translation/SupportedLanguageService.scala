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

    def getSupportedLanguages(fromLanguage: Option[LanguageTag] = None): Seq[Language] = {
      val cacheKey = s"$DefaultKey${fromLanguage.getOrElse("")}"

      cache.getIfPresent(cacheKey) match {
        case Some(x) => x
        case None =>
          val supportedLanguages = loadSupportedLanguages(fromLanguage).sortBy(_.name)
          cache.put(cacheKey, supportedLanguages)
          supportedLanguages
      }
    }

    private def loadSupportedLanguages(fromLanguage: Option[LanguageTag]) = {
      val defaultLanguages = crowdinClientBuilder.withGenericAccess.getSupportedLanguages match {
        case Success(supportedLanguages) => supportedLanguages.flatMap(supportedLanguage => {
          Try(LanguageTag(supportedLanguage.crowdinCode)) match {
            case Success(validLanguage) => Some(Language(supportedLanguage.crowdinCode, validLanguage.displayName, if (validLanguage.isRightToLeft) Some(validLanguage.isRightToLeft) else None))
            case Failure(_) => None
          }
        }).distinct
        case Failure(ex) => throw ex
      }

      val targetLanguages = fromLanguage.map(languageTag => {
        crowdinClientBuilder.forSourceLanguage(languageTag).flatMap(client => {
          client.getTargetLanguages.map(noe => noe.flatMap(targetLanguage => {
            Try(LanguageTag(targetLanguage.code)) match {
              case Success(validLanguage) => Some(Language(targetLanguage.code, validLanguage.displayName, if (validLanguage.isRightToLeft) Some(validLanguage.isRightToLeft) else None))
              case Failure(_) => None
            }
          }))})
        })


      val validTargetLanguages = targetLanguages match {
        case None => Seq()
        case Some(noe) => noe.getOrElse(Seq())
      }

      (defaultLanguages ++ validTargetLanguages).distinct
    }
  }
}
