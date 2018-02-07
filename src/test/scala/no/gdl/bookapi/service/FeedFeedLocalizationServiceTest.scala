package no.gdl.bookapi.service

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.{TestEnvironment, UnitSuite}

class FeedFeedLocalizationServiceTest extends UnitSuite with TestEnvironment {

  override val feedLocalizationService = new FeedLocalizationService

  test("that Norwegian localization is read properly") {
    val l = feedLocalizationService.localizationFor(LanguageTag("nob"))
    l.rootTitle should equal("Global Digital Library - Bokkatalog")
    l.navTitle should equal("Global Digital Library - Bokkatalog")
    l.levelTitle("5") should equal("Nivå 5")
    l.levelDescription should equal("Bøker med angitt lesenivå")
  }

  test("that all supported languages are parsed") {
    feedLocalizationService.supportedLanguages should equal(feedLocalizationService.localizationMap.keySet)
  }

  test("that all level title functions can be run") {
    feedLocalizationService.localizationMap.foreach { case (language, localization) =>
      val levelTitle = localization.levelTitle("123")
      withClue(s"Level title for ${language.toString} should be nonempty") { levelTitle.nonEmpty should be(true) }
      withClue(s"Level title for ${language.toString} should contain 123") { levelTitle.contains("123") should be(true) }
    }
  }

}
