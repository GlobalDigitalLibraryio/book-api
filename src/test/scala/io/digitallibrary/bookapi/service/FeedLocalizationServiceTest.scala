package io.digitallibrary.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.{TestEnvironment, UnitSuite}

class FeedLocalizationServiceTest extends UnitSuite with TestEnvironment {

  override val feedLocalizationService = new FeedLocalizationService

  test("that Norwegian localization is read properly") {
    val l = feedLocalizationService.localizationFor(LanguageTag("nob"))
    l.rootTitle should equal("Global Digital Library - Bokkatalog")
    l.levelTitle("4") should equal("Nivå 4")
    l.levelDescription should equal("Bøker med angitt lesenivå")
  }

  test("that all supported languages are parsed") {
    feedLocalizationService.supportedLanguages should equal(feedLocalizationService.localizationMap.keySet)
  }

  test("that all level title functions can be run") {
    def assert(language: LanguageTag, localization: FeedLocalization, level: String) = {
      val title = localization.levelTitle(level)
      withClue(s"Level title for ${language.toString} should be nonempty") { title.nonEmpty should be(true) }
    }
    for {
      (language, localization) <- feedLocalizationService.localizationMap
      level <- Seq("1", "2", "3", "4", "read-aloud", "decodable")
    } yield assert(language, localization, level)
  }

  test("that all category title functions can be run") {
    def assert(language: LanguageTag, localization: FeedLocalization, category: String) = {
      val title = localization.categoryTitle(category)
      withClue(s"Category title for ${language.toString} should be nonempty") { title.nonEmpty should be(true) }
    }
    for {
      (language, localization) <- feedLocalizationService.localizationMap
      category <- Seq("library_books", "classroom_books")
    } yield assert(language, localization, category)
  }

}
