/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.service

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest, S3Object}
import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.AmazonClient
import io.digitallibrary.bookapi.model.api.{Chapter, NotFoundException, PdfStream}
import io.digitallibrary.bookapi.model.domain.{BookFormat, PdfCss}
import io.digitallibrary.bookapi.repository.{BookRepository, TranslationRepository}
import io.digitallibrary.language.model.LanguageTag
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities.EscapeMode

import scala.util.{Failure, Success, Try}


trait PdfService {
  this: TranslationRepository with BookRepository with TranslationRepository with ReadService with AmazonClient =>
  val pdfService: PdfService

  class PdfService extends LazyLogging {

    case class S3Pdf(s3Object: S3Object, fileName: String) extends PdfStream {
      override def stream: InputStream = {
        s3Object.getObjectContent
      }
    }

    case class RBPdf(pdfRendererBuilder: PdfRendererBuilder, fileName: String) extends PdfStream {
      override def stream: InputStream = {
        val out = new ByteArrayOutputStream()
        pdfRendererBuilder.toStream(out).run()
        new ByteArrayInputStream(out.toByteArray)
      }
    }

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
      "arabic" -> FontDefinition("/NotoSansArabic-Regular.ttf", "Noto Sans Arabic")
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
      LanguageTag("ar") -> fonts("arabic")
    )

    def getPdf(language: LanguageTag, uuid: String): Option[PdfStream] = {
      unFlaggedTranslationsRepository.withUuId(uuid) match {
        case Some(translation) => translation.bookFormat match {
          case BookFormat.HTML =>
            Some(RBPdf(createPdf(language, uuid).get, s"${translation.title}.pdf"))
          case BookFormat.PDF => getFromS3(uuid) match {
            case Success(s3Object) => Some(S3Pdf(s3Object, s"${translation.title}.pdf"))
            case Failure(_) => None
          }
        }
        case _ => None
      }
    }

    def createPdf(language: LanguageTag, uuid: String): Option[PdfRendererBuilder] = {
      unFlaggedTranslationsRepository.withUuId(uuid).map(translation => {
        val source = bookRepository.withId(translation.bookId).map(_.source)

        val chapters = readService.chaptersForIdAndLanguage(translation.bookId, language).flatMap(ch => readService.chapterForBookWithLanguageAndId(translation.bookId, language, ch.id))
        val preprocessedChapters = preprocessChapters(source, chapters)
        val fonts = fontDefinitions.get(language).toSeq :+ DefaultFont

        val bookAsHtml =
          s"""
             |<html>
             |  <head>
             |    <style language='text/css'>${PdfCss(source, translation.pageOrientation, fonts.map(_.fontName)).asString}</style>
             |  </head>
             |  <body>
             |    ${preprocessedChapters.zipWithIndex.map { case (c, i) => s"<div class='page page_$i ${c.chapterType.toString.toLowerCase}'>${c.content}</div>" }.mkString("\n")}
             |  </body>
             |</html>
           """.stripMargin

        val rendererBuilder = new PdfRendererBuilder().withHtmlContent(bookAsHtml, "/")
        fonts.foldLeft(rendererBuilder) { (builder, font) =>
          builder.useFont(new NotoFontSupplier(getClass.getResourceAsStream(font.fontFile)), font.fontName)
        }
      })
    }

    private def preprocessChapters(source: Option[String], chapters: Seq[Chapter]) = {
      def processStoryWeaverFirstPage(c: Chapter): Chapter = {
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

      def process3AsafeerImages(c: Chapter): Chapter = {
        val document = Jsoup.parseBodyFragment(c.content)
        val images = document.select("img")
        for (i <- 0 until images.size()) {
          val image = images.get(i)
          val src = image.attr("src")
          val regex = "(^.+\\?)(width=)(\\d+)(.*)".r
          regex.findFirstMatchIn(src).foreach(matchPattern => {
            image.attr("width", matchPattern.group(3))
          })
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
        case Some("3asafeer") => {
          chapters.map(process3AsafeerImages)
        }
        case _ => chapters
      }
    }


    def getFromS3(uuid: String): Try[S3Object] = {
      Try(amazonClient.getObject(new GetObjectRequest(BookApiProperties.StorageName, s"$uuid.pdf"))) match {
        case Success(success) => Success(success)
        case Failure(_) => Failure(new NotFoundException(s"Pdf with name $uuid.pdf does not exist"))
      }
    }

    def uploadFromStream(stream: InputStream, uuid: String, contentType: String, size: Long): Try[String] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(size)

      Try(amazonClient.putObject(new PutObjectRequest(BookApiProperties.StorageName, s"$uuid.pdf", stream, metadata))).map(_ => uuid)
    }
  }
}


