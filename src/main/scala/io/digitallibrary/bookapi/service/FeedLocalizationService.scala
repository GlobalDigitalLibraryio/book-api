package io.digitallibrary.bookapi.service

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties

trait FeedLocalizationService {
  val feedLocalizationService: FeedLocalizationService

  case class FeedLocalization(rootTitle: String,
                              navTitle: String,
                              levelTitle: String => String,
                              levelDescription: String)

  class FeedLocalizationService extends LazyLogging {

    private val defaultLanguage: LanguageTag = LanguageTag(BookApiProperties.DefaultLanguage)

    private[service] val supportedLanguages: Set[LanguageTag] = Seq(
      defaultLanguage.toString,
      "amh",
      "nob"
    ).map(LanguageTag(_)).toSet

    private def configToLocalization(language: LanguageTag, config: Config) = {
      def getNonEmptyString(name: String): String = {
        val value = config.getString(name)
        if (value.isEmpty)
          throw new RuntimeException(s"Value $name is empty for language ${language.displayName} (${language.toString})")
        else
          value
      }

      FeedLocalization(
        rootTitle = getNonEmptyString("opds_root_title"),
        navTitle = getNonEmptyString("opds_nav_title"),
        levelTitle = level => getNonEmptyString(s"level_feed_title_$level"),
        levelDescription = getNonEmptyString("level_feed_description"))
    }

    private[service] val localizationMap: Map[LanguageTag, FeedLocalization] = (for {
      language <- supportedLanguages
      config = ConfigFactory.load(s"feedLocalization/${language.toString}.properties")
    } yield language -> configToLocalization(language, config)).toMap

    private val localizationForDefaultLanguage = localizationMap(defaultLanguage)

    def localizationFor(language: LanguageTag): FeedLocalization =
      localizationMap.getOrElse(language, localizationForDefaultLanguage)

  }


}

