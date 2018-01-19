/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model

import com.sksamuel.elastic4s.analyzers._

object Language {
  val DefaultLanguage = "eng"
  val UnknownLanguage = "unknown"
  val AllLanguages = "all"
  val NoLanguage = ""

  val languageAnalyzers = Seq(
    LanguageAnalyzer(DefaultLanguage, EnglishLanguageAnalyzer),
    LanguageAnalyzer("nob", NorwegianLanguageAnalyzer),
    LanguageAnalyzer("eng", EnglishLanguageAnalyzer),
    LanguageAnalyzer("fra", FrenchLanguageAnalyzer),
    LanguageAnalyzer("deu", GermanLanguageAnalyzer),
    LanguageAnalyzer("spa", SpanishLanguageAnalyzer),
    LanguageAnalyzer("sme", StandardAnalyzer), // SAMI
    LanguageAnalyzer("zho", ChineseLanguageAnalyzer),
    LanguageAnalyzer("hin", HindiLanguageAnalyzer),
    LanguageAnalyzer(UnknownLanguage, StandardAnalyzer)
  )

  val supportedLanguages = languageAnalyzers.map(_.lang)

  /*def findByLanguageOrBestEffort[P <: LanguageField[_]](sequence: Seq[P], lang: Option[String]): Option[P] = {
    def findFirstLanguageMatching(sequence: Seq[P], lang: Seq[String]): Option[P] = {
      lang match {
        case Nil => sequence.headOption
        case head :: tail =>
          sequence.find(_.language == head) match {
            case Some(x) => Some(x)
            case None => findFirstLanguageMatching(sequence, tail)
          }
      }
    }

    findFirstLanguageMatching(sequence, lang.toList :+ DefaultLanguage)
  }*/

  def languageOrUnknown(language: Option[String]): String = {
    language.filter(_.nonEmpty) match {
      case Some(x) => x
      case None => UnknownLanguage
    }
  }
}

case class LanguageAnalyzer(lang: String, analyzer: Analyzer)
