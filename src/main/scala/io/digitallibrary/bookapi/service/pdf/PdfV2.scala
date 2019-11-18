package io.digitallibrary.bookapi.service.pdf

import java.io.InputStream

import com.openhtmltopdf.bidi.support.ICUBidiReorderer
import com.openhtmltopdf.bidi.support.ICUBidiSplitter.ICUBidiSplitterFactory
import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.TextDirection
import io.digitallibrary.bookapi.model.{api, domain}
import io.digitallibrary.language.model.LanguageTag
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities.EscapeMode

case class PdfV2(language: LanguageTag, chapters: Seq[api.ChapterV2], translation: domain.Translation, source: Option[String] = None) {

  case class FontDefinition(fontFile: String, fontName: String)

  class NotoFontSupplier(stream: InputStream) extends FSSupplier[InputStream] {
    override def supply(): InputStream = stream
  }

  val DefaultFont = FontDefinition("/NotoSans-Regular.ttf", "Noto Sans")
  val fonts = Map(
    "ethiopic" -> FontDefinition("/NotoSansEthiopic.ttf", "Noto Sans Ethiopic"),
    "devangari" -> FontDefinition("/NotoSansDevanagari-Regular.ttf", "Noto Sans Devanagari"),
    "bengali" -> FontDefinition("/NotoSansBengali-Regular.ttf", "Noto Sans Bengali"),
    "khmer" -> FontDefinition("/NotoSansKhmer-Regular.ttf", "Noto Sans Khmer"),
    "tamil" -> FontDefinition("/NotoSansTamil-Regular.ttf", "Noto Sans Tamil"),
    "arabic" -> FontDefinition("/NotoNaskhArabic-Regular.ttf", "Noto Naskh Arabic"),
    "newtailue" -> FontDefinition("/NotoSansNewTaiLue-Regular.ttf", "Noto Sans New Tai Lue")
  )
  val fontDefinitions = Map(
    LanguageTag("tir-et") -> fonts("ethiopic"),
    LanguageTag("tir") -> fonts("ethiopic"),
    LanguageTag("amh") -> fonts("ethiopic"),
    LanguageTag("mar") -> fonts("devangari"),
    LanguageTag("hin") -> fonts("devangari"),
    LanguageTag("ben") -> fonts("bengali"),
    LanguageTag("nep") -> fonts("devangari"),
    LanguageTag("khm") -> fonts("khmer"),
    LanguageTag("tam") -> fonts("tamil"),
    LanguageTag("ar-ae") -> fonts("arabic"),
    LanguageTag("ar") -> fonts("arabic"),
    LanguageTag("khb") -> fonts("newtailue"),
    LanguageTag("awa") -> fonts("devangari"),
    LanguageTag("thl") -> fonts("devangari"),
    LanguageTag("mai") -> fonts("devangari"),
    LanguageTag("bho") -> fonts("devangari"),
    LanguageTag("dty") -> fonts("devangari"),
    LanguageTag("new") -> fonts("devangari"),
    LanguageTag("ne-np") -> fonts("devangari")
  )

  def create(): PdfRendererBuilder = {
    val preprocessedChapters = preprocessChapters(source, chapters)
    val fonts = fontDefinitions.get(language).toSeq :+ DefaultFont

    val direction = if (language.isRightToLeft) "rtl" else "ltr"

    val bookAsHtml =
      s"""
         |<html>
         |  <head>
         |    <style language='text/css'>${domain.PdfCss(domain.PageOrientation.PORTRAIT, fonts.map(_.fontName)).asString}</style>
         |  </head>
         |  <body>
         |    ${preprocessedChapters.zipWithIndex.map { case (c, i) => s"<div class='page page_$i ${c.chapterType.toString.toLowerCase}' dir='$direction'>${c.content}</div>" }.mkString("\n")}
         |  </body>
         |</html>
           """.stripMargin

    val rendererBuilder = new PdfRendererBuilder().withHtmlContent(bookAsHtml, "/")
    if (language.isRightToLeft) {
      rendererBuilder
        .useUnicodeBidiSplitter(new ICUBidiSplitterFactory())
        .useUnicodeBidiReorderer(new ICUBidiReorderer())
        .defaultTextDirection(TextDirection.RTL)
    }

    fonts.foldLeft(rendererBuilder) { (builder, font) =>
      builder.useFont(new NotoFontSupplier(getClass.getResourceAsStream(font.fontFile)), font.fontName)
    }
    rendererBuilder
  }

  // TODO: V2 must rewrite this to get right chapter images...
  private def preprocessChapters(source: Option[String], chapters: Seq[api.ChapterV2]) = {
    def processStoryWeaverFirstPage(c: api.ChapterV2): api.ChapterV2 = {
      val document = Jsoup.parseBodyFragment(c.content)
      val images = document.select("img")
      for (i <- 0 until images.size()) {
        val image = images.get(i)
        if (i == 0) { // First image
          image.addClass("cover")
        } else {
          image.addClass("logo")
        }
      }
      document.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      c.copy(content = document.select("body").html())
    }

    source match {
      case Some("storyweaver") =>
        chapters match {
          case first :: rest => processStoryWeaverFirstPage(first) :: rest
          case _ => chapters
        }
      case _ => chapters
    }
  }

}
