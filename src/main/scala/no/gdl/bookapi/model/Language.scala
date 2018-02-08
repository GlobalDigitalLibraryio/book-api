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
  val DefaultLanguage = LanguageTag("eng")
  val UnknownLanguage = LanguageTag("und")//Undetermined
  val AllLanguages = "all"
  val NoLanguage = ""

  val languageAnalyzers = Seq(
    GdlLanguageAnalyzer(DefaultLanguage, EnglishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("ara"), ArabicLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("axm"), ArmenianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("baq"), BasqueLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("bra"), BrazilianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("bul"), BulgarianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("cat"), CatalanLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("zho"), ChineseLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("cjk"), CjkLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("cze"), CzechLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("dan"), DanishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("dut"), DutchLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("eng"), EnglishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("fin"), FinnishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("fra"), FrenchLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("glg"), GalicianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("ger"), GermanLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("gre"), GreekLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("hin"), HindiLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("hun"), HungarianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("ind"), IndonesianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("gle"), IrishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("ita"), ItalianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("lav"), LatvianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("lit"), LithuanianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("nob"), NorwegianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("per"), PersianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("por"), PortugueseLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("rum"), RomanianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("rus"), RussianLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("kur"), SoraniLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("spa"), SpanishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("swe"), SwedishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("tur"), TurkishLanguageAnalyzer),
    GdlLanguageAnalyzer(LanguageTag("tha"), ThaiLanguageAnalyzer),
    GdlLanguageAnalyzer(UnknownLanguage, BabelAnalyzer)
  )

  val supportedLanguages = languageAnalyzers.map(_.languageTag)

  def findByLanguage(languageTag: LanguageTag): Option[GdlLanguageAnalyzer] = {
    def findFirstLanguageMatching(sequence: Seq[GdlLanguageAnalyzer], lang: Seq[LanguageTag]): Option[GdlLanguageAnalyzer] = {
      lang match {
        case Nil => sequence.headOption
        case head :: tail =>
          sequence.find(_.languageTag == head) match {
            case Some(x) => Some(x)
            case None => findFirstLanguageMatching(sequence, tail)
          }
      }
    }
    findFirstLanguageMatching(languageAnalyzers, List(languageTag,UnknownLanguage))
  }
}

case class GdlLanguageAnalyzer(languageTag: LanguageTag, analyzer: Analyzer)
case object BabelAnalyzer extends LanguageAnalyzer("babel")
case object IcuTokenizer extends Tokenizer("icu_tokenizer")
case object IcuNormalizer extends CharFilter {
  val name = "icu_normalizer"
}
case object IcuFolding extends TokenFilter {
  val name = "icu_folding"
}
case object IcuCollation extends TokenFilter {
  val name = "icu_collation"
}
