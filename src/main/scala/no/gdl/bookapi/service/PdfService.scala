/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import java.io.InputStream

import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.model.domain.PdfCss
import no.gdl.bookapi.repository.{BookRepository, TranslationRepository}


trait PdfService {
  this: TranslationRepository with BookRepository with ReadService =>
  val pdfService: PdfService

  class PdfService extends LazyLogging {
    case class FontDefinition(fontFile: String, fontName: String)
    class NotoFontSupplier(stream: InputStream) extends FSSupplier[InputStream] {
      override def supply(): InputStream = stream
    }

    val DefaultFont = FontDefinition("/NotoSans-Regular.ttf", "Noto Sans")
    val fontDefinitions = Map(
      "amh" -> FontDefinition("/NotoSansEthiopic.ttf", "Noto Sans Ethiopic"),
      "mar" -> FontDefinition("/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari"),
      "hin" -> FontDefinition("/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari"),
      "ben" -> FontDefinition("/NotoSansBengali-Regular.ttf", "Noto Sans Bengali"),
      "nep" -> FontDefinition("/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari")
    )

    def createPdf(language: String, uuid: String): Option[PdfRendererBuilder] = {
      translationRepository.withUuId(uuid).map(translation => {
        val publisher = bookRepository.withId(translation.bookId).map(_.publisher.name)

        val chapters = readService.chaptersForIdAndLanguage(translation.bookId, language).flatMap(ch => readService.chapterForBookWithLanguageAndId(translation.bookId, language, ch.id))
        val fonts = fontDefinitions.get(language).toSeq :+ DefaultFont

        val bookAsHtml =
          s"""
             |<html>
             |  <head>
             |    <style language='text/css'>${PdfCss(publisher, fonts.map(_.fontName)).asString}</style>
             |  </head>
             |  <body>
             |    ${chapters.map(c => s"<div class='page'>${c.content}</div>").mkString("\n")}
             |  </body>
             |</html>
           """.stripMargin

        val rendererBuilder = new PdfRendererBuilder().withHtmlContent(bookAsHtml, "/")
        fonts.foldLeft(rendererBuilder) {(builder, font) =>
          builder.useFont(new NotoFontSupplier(getClass.getResourceAsStream(font.fontFile)), font.fontName)
        }
      })
    }
  }
}


