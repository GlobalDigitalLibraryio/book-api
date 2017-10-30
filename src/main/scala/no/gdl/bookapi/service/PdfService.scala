/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import com.lowagie.text.pdf.BaseFont
import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.repository.{ChapterRepository, TranslationRepository}
import org.xhtmlrenderer.pdf.ITextRenderer

import scala.util.Try




trait PdfService {
  this: TranslationRepository with ReadService =>
  val pdfService: PdfService

  class PdfService extends LazyLogging {

    val fontDefinitions = Map(
      "amh" -> FontDefinition("NotoSansEthiopic.ttf", "Noto Sans Ethiopic"),
      "mar" -> FontDefinition("NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari"),
      "hin" -> FontDefinition("NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari"),
      "ben" -> FontDefinition("NotoSansBengali-Regular.ttf", "Noto Sans Bengali"),
      "nep" -> FontDefinition("NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari")
    )

    val pdfCss =
      """
        |div.page {
        | page-break-after: always;
        |}
        |
        |body {
        |    margin: 0;
        |    text-align: center;
        |    font-family: '{FONT-NAME}';
        |}
        |
        |img {
        |    max-width: 100%;
        |    max-height: 50vh;
        |}
      """.stripMargin


    def createPdf(language: String, uuid: String): Option[Try[ITextRenderer]] = {
      translationRepository.withUuId(uuid).map(translation => {
          Try {
            val chapters = readService.chaptersForIdAndLanguage(translation.bookId, language).flatMap(ch => readService.chapterForBookWithLanguageAndId(translation.bookId, language, ch.id))

            val renderer = new ITextRenderer

            val css = fontDefinitions.get(language) match {
              case None => pdfCss
              case Some(font) =>
                renderer.getFontResolver.addFont(font.fontFile, BaseFont.IDENTITY_H, true)
                pdfCss.replace("{FONT-NAME}", font.fontName)
            }

            val bookAsHtml = s"<html><head><style language='text/css'>$css</style></head><body><div class='page'>${chapters.map(_.content).mkString("</div><div class='page'>")}</div></body></html>"

            renderer.setDocumentFromString(bookAsHtml)
            renderer.layout()
            renderer
          }
        })
      }
    }
}

case class FontDefinition(fontFile: String, fontName: String)
