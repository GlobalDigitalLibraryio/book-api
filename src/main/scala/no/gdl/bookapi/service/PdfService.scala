/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.service

import java.io.{ByteArrayOutputStream, InputStream}

import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest, S3Object}
import com.openhtmltopdf.extend.FSSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.integration.AmazonClient
import no.gdl.bookapi.model.api.{NotFoundException, PdfStream}
import no.gdl.bookapi.model.domain.{BookFormat, PdfCss}
import no.gdl.bookapi.repository.{BookRepository, TranslationRepository}
import org.apache.commons.io.IOUtils

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

    case class Pdf(pdfRendererBuilder: PdfRendererBuilder, fileName: String) extends PdfStream {
      override def stream: InputStream = {
        val baos = new ByteArrayOutputStream()
        pdfRendererBuilder.toStream(baos).run()
        IOUtils.toInputStream(baos.toString)
      }
    }

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

    def getPdf(language: LanguageTag, uuid: String): Option[PdfStream] = {
      translationRepository.withUuId(uuid) match {
        case Some(translation) => translation.bookFormat match {
          case BookFormat.HTML =>
            Some(Pdf(createPdf(language, uuid).get, s"$uuid.pdf"))
          case BookFormat.PDF => getFromS3(uuid) match {
            case Success(s3Object) => Some(S3Pdf(s3Object, s"$uuid.pdf"))
            case Failure(failure) => None
          }
        }
        case _ => None
      }
    }

    def createPdf(language: LanguageTag, uuid: String): Option[PdfRendererBuilder] = {
      translationRepository.withUuId(uuid).map(translation => {
        val publisher = bookRepository.withId(translation.bookId).map(_.publisher.name)

        val chapters = readService.chaptersForIdAndLanguage(translation.bookId, language).flatMap(ch => readService.chapterForBookWithLanguageAndId(translation.bookId, language, ch.id))
        val fonts = fontDefinitions.get(language.toString).toSeq :+ DefaultFont

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


