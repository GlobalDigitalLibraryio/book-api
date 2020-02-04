package io.digitallibrary.bookapi.service

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest, S3Object}
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.integration.AmazonClient
import io.digitallibrary.bookapi.model.api.{NotFoundException, PdfStream}
import io.digitallibrary.bookapi.model.domain.BookFormat
import io.digitallibrary.bookapi.repository.{BookRepository, TranslationRepository}
import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.service.pdf.{Pdf, PdfV2}

import scala.util.{Failure, Success, Try}

trait PdfServiceV2 {
  this: TranslationRepository with BookRepository with TranslationRepository with ReadServiceV2 with AmazonClient =>
  val pdfServiceV2: PdfServiceV2

  class PdfServiceV2 extends LazyLogging {

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
        PdfV2(language=language,
          chapters=readServiceV2.chaptersForIdAndLanguage(translation.bookId, language).flatMap(ch => readServiceV2.chapterForBookWithLanguageAndId(translation.bookId, language, ch.id, true)),
          translation=translation,
          source=bookRepository.withId(translation.bookId).map(_.source))
          .create()
      })
    }

    def getFromS3(uuid: String): Try[S3Object] = {
      Try(amazonS3Client.getObject(new GetObjectRequest(BookApiProperties.StorageName, s"$uuid.pdf"))) match {
        case Success(success) => Success(success)
        case Failure(_) => Failure(new NotFoundException(s"Pdf with name $uuid.pdf does not exist"))
      }
    }

    def uploadFromStream(stream: InputStream, uuid: String, contentType: String, size: Long): Try[String] = {
      val metadata = new ObjectMetadata()
      metadata.setContentType(contentType)
      metadata.setContentLength(size)

      Try(amazonS3Client.putObject(new PutObjectRequest(BookApiProperties.StorageName, s"$uuid.pdf", stream, metadata))).map(_ => uuid)
    }
  }

}
