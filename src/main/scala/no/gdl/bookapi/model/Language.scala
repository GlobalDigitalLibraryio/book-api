/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model

import com.sksamuel.elastic4s.analyzers._
import io.digitallibrary.language.model.LanguageTag

object Language {
  val DefaultLanguage = "eng"
  val UnknownLanguage = "unknown"
  val AllLanguages = "all"
  val NoLanguage = ""

  val languageAnalyzers = Seq(
    GdlLanguageAnalyzer(DefaultLanguage, EnglishLanguageAnalyzer),
    GdlLanguageAnalyzer("ara", ArabicLanguageAnalyzer),
    GdlLanguageAnalyzer("axm", ArmenianLanguageAnalyzer),
    GdlLanguageAnalyzer("baq", BasqueLanguageAnalyzer),
    GdlLanguageAnalyzer("bra", BrazilianLanguageAnalyzer),
    GdlLanguageAnalyzer("bul", BulgarianLanguageAnalyzer),
    GdlLanguageAnalyzer("cat", CatalanLanguageAnalyzer),
    GdlLanguageAnalyzer("zho", ChineseLanguageAnalyzer),
    GdlLanguageAnalyzer("cjk", CjkLanguageAnalyzer),
    GdlLanguageAnalyzer("cze", CzechLanguageAnalyzer),
    GdlLanguageAnalyzer("dan", DanishLanguageAnalyzer),
    GdlLanguageAnalyzer("dut", DutchLanguageAnalyzer),
    GdlLanguageAnalyzer("eng", EnglishLanguageAnalyzer),
    GdlLanguageAnalyzer("fin", FinnishLanguageAnalyzer),
    GdlLanguageAnalyzer("fra", FrenchLanguageAnalyzer),
    GdlLanguageAnalyzer("glg", GalicianLanguageAnalyzer),
    GdlLanguageAnalyzer("ger", GermanLanguageAnalyzer),
    GdlLanguageAnalyzer("gre", GreekLanguageAnalyzer),
    GdlLanguageAnalyzer("hin", HindiLanguageAnalyzer),
    GdlLanguageAnalyzer("hun", HungarianLanguageAnalyzer),
    GdlLanguageAnalyzer("ind", IndonesianLanguageAnalyzer),
    GdlLanguageAnalyzer("gle", IrishLanguageAnalyzer),
    GdlLanguageAnalyzer("ita", ItalianLanguageAnalyzer),
    GdlLanguageAnalyzer("lav", LatvianLanguageAnalyzer),
    GdlLanguageAnalyzer("lit", LithuanianLanguageAnalyzer),
    GdlLanguageAnalyzer("nob", NorwegianLanguageAnalyzer),
    GdlLanguageAnalyzer("per", PersianLanguageAnalyzer),
    GdlLanguageAnalyzer("por", PortugueseLanguageAnalyzer),
    GdlLanguageAnalyzer("rum", RomanianLanguageAnalyzer),
    GdlLanguageAnalyzer("rus", RussianLanguageAnalyzer),
    GdlLanguageAnalyzer("kur", SoraniLanguageAnalyzer),
    GdlLanguageAnalyzer("spa", SpanishLanguageAnalyzer),
    GdlLanguageAnalyzer("swe", SwedishLanguageAnalyzer),
    GdlLanguageAnalyzer("tur", TurkishLanguageAnalyzer),
    GdlLanguageAnalyzer("tha", ThaiLanguageAnalyzer),
    GdlLanguageAnalyzer(UnknownLanguage, BabelAnalyzer)
  )

  val supportedLanguages = languageAnalyzers.map(_.lang)

  def findByLanguage(lang: Option[String]): Option[GdlLanguageAnalyzer] = {
    def findFirstLanguageMatching(sequence: Seq[GdlLanguageAnalyzer], lang: Seq[String]): Option[GdlLanguageAnalyzer] = {
      lang match {
        case Nil => sequence.headOption
        case head :: tail =>
          sequence.find(_.lang == head) match {
            case Some(x) => Some(x)
            case None => findFirstLanguageMatching(sequence, tail)
          }
      }
    }

    findFirstLanguageMatching(languageAnalyzers, lang.toList :+ UnknownLanguage)
  }

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None => UnknownLanguage
    }
  }
}

case object BabelAnalyzer extends LanguageAnalyzer("babel")
case class GdlLanguageAnalyzer(lang: String, analyzer: Analyzer)
