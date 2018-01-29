/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration.crowdin

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.GdlClient
import no.gdl.bookapi.model.api.internal.ChapterId
import no.gdl.bookapi.model.api.{Book, Chapter, CrowdinException}
import no.gdl.bookapi.model.crowdin._
import no.gdl.bookapi.model.domain.{FileType, InTranslation, InTranslationFile}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.{read, write}

import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, MultiPart}


class LimitedCrowdinClient extends GdlClient with LazyLogging {
  override val gdlClient = new GdlClient

  protected val CrowdinBaseUrl = "https://api.crowdin.com/api"
  protected val SupportedLanguagesUrl = s"$CrowdinBaseUrl/supported-languages?json"

  def getSupportedLanguages: Try[Seq[SupportedLanguage]] =
    gdlClient.fetch[Seq[SupportedLanguage]](Http(SupportedLanguagesUrl))
}



class CrowdinClient(fromLanguage: String, projectIdentifier: String, projectKey: String) extends LimitedCrowdinClient {


  private def urlFor(action: String) = s"$CrowdinBaseUrl/project/$projectIdentifier/$action?key=$projectKey&json"
  private val ProjectDetailsUrl = urlFor("info")
  private val EditProjectUrl = urlFor("edit-project")
  private val AddDirectoryUrl = urlFor("add-directory")
  private val DeleteDirectoryUrl = urlFor("delete-directory")
  private val AddFileUrl = urlFor("add-file")
  private val ExportFileUrl = urlFor("export-file")

  def getProjectIdentifier: String = projectIdentifier

  def addBookMetadata(book: Book): Try[CrowdinFile] = {
    implicit val formats: DefaultFormats = DefaultFormats

    val metadata = BookMetaData(book.title, book.description)
    val filename = CrowdinUtils.metadataFilenameFor(book)

    val response = gdlClient
      .fetch[AddFilesResponse](Http(AddFileUrl)
      .postMulti(MultiPart(s"files[$filename]", filename, "application/json", write(metadata).getBytes)))

    response.map(res => CrowdinFile(None, FileType.METADATA, res.stats.get.files.head)) match {
      case Success(x) => Success(x)
      case Failure(ex) => Failure(CrowdinException(ex))
    }
  }


  def fetchTranslatedMetaData(inTranslationFile: InTranslationFile, language: String): Try[BookMetaData] = {
    val url = s"$ExportFileUrl&file=${inTranslationFile.filename}&language=$language"

    gdlClient.doRequestAsString(Http(url).copy(compress = false)).flatMap(response => {
      gdlClient.parseResponse[BookMetaData](response).map(bookMetaData =>  {
        bookMetaData.copy(etag = response.header("ETag"))
      })
    })
  }


  def addChaptersFor(book: Book, chapters: Seq[Chapter]): Try[Seq[CrowdinFile]] = {
    val uploadTries: Seq[Try[AddFilesResponse]] = chapters.sliding(20, 20).toList.map(window => {
      val multiParts = window.map(chapter => {
        val filename = CrowdinUtils.filenameFor(book, chapter)
        MultiPart(s"files[$filename]", filename, "application/xhtml+xml", chapter.content.getBytes)
      })

      gdlClient.fetch[AddFilesResponse](Http(AddFileUrl).postMulti(multiParts:_*).timeout(1000, 10000))
    })

    val httpExceptions = uploadTries.filter(_.isFailure).map(_.failed.get)
    val crowdinExceptions = uploadTries.filter(req => req.isSuccess && !req.get.success).map(_.get.error.get)

    if(httpExceptions.nonEmpty || crowdinExceptions.nonEmpty) {
      Failure(CrowdinException(crowdinExceptions, httpExceptions))
    } else {
      val addedFiles = uploadTries.flatMap(_.get.stats.get.files)
      val result = addedFiles.flatMap(file => {
        chapters.find(chapter => CrowdinUtils.filenameFor(book, chapter) == file.name).map(chapterForFile => {
          CrowdinFile(Some(chapterForFile.id), FileType.CONTENT, file)
        })
      })

      if (result.length == addedFiles.length) {
        Success(result)
      } else {
        Failure(CrowdinException("Inconsistent file-result"))
      }
    }
  }

  def fetchTranslatedChapter(inTranslationFile: InTranslationFile, language: String): Try[TranslatedChapter] = {
    val url = s"$ExportFileUrl&file=${inTranslationFile.filename}&language=$language"

    gdlClient.doRequestAsString(Http(url).copy(compress = false)).map(response => {
      TranslatedChapter(inTranslationFile.originalChapterId.get, inTranslationFile.newChapterId, response.body, response.header("ETag"))
    })
  }

  def addDirectoryFor(book: Book): Try[String] = {
    val directoryName = CrowdinUtils.directoryNameFor(book)

    gdlClient.fetch[AddDirectoryResponse](Http(AddDirectoryUrl).postForm(Seq("name" -> directoryName)))
      .map(_ => directoryName) match {
      case Success(x) => Success(x)
      case Failure(ex) => Failure(CrowdinException(ex))
    }
  }

  def deleteDirectoryFor(book: Book): Try[Unit] = {
    val result = gdlClient.fetch[DeleteDirectoryResponse](Http(DeleteDirectoryUrl).postForm(Seq("name" -> CrowdinUtils.directoryNameFor(book))))
    result match {
      case Success(deleteDirectoryResponse) if deleteDirectoryResponse.success => Success()
      case Success(deleteDirectoryResponse) if deleteDirectoryResponse.dirNotFoundError => Success()
      case Success(deleteDirectoryResponse) if !deleteDirectoryResponse.success =>
        Failure(CrowdinException(deleteDirectoryResponse.error.get))

      case Failure(err) => Failure(CrowdinException(err))
    }
  }

  def addTargetLanguage(toLanguage: String): Try[Unit] = {
    getTargetLanguages.flatMap(languages => {
      if (!languages.exists(_.code == toLanguage)) {
        val newCodeList = languages.map(_.code) :+ toLanguage
        val postParams = newCodeList.zipWithIndex.map(x => (s"languages[${x._2}]", x._1.toString))

        gdlClient.fetch[EditProjectResponse](Http(EditProjectUrl).postForm(postParams)) match {
          case Success(EditProjectResponse(Some(Project(true)), _)) => Success()
          case Success(EditProjectResponse(_, Some(error))) => Failure(CrowdinException(error))
          case Success(_) => Failure(CrowdinException(s"Unknown error when adding $toLanguage to Crowdin"))
          case Failure(ex) => Failure(CrowdinException(ex))
        }
      } else {
        Success()
      }
    })
  }

  def getTargetLanguages: Try[Seq[TargetLanguage]] = {
    getProjectDetails.map(_.languages)
  }

  def getProjectDetails: Try[ProjectDetails] = {
    gdlClient.fetch[ProjectDetails](Http(ProjectDetailsUrl)) match {
      case Success(x) => Success(x)
      case Failure(ex) => Failure(CrowdinException(ex))
    }
  }

}

object CrowdinUtils {
  private val CrowdinDirectoryUrl = "https://crowdin.com/project/{PROJECT_IDENTIFIER}/{LANGUAGE_CODE}#/{DIRECTORY_NAME}"

  def crowdinUrlToBook(book: Book, crowdinProjectId: String, toLanguage: String): String =
    s"https://crowdin.com/project/$crowdinProjectId/$toLanguage#/${directoryNameFor(book)}"

  def directoryNameFor(book: Book) = s"${book.id}-${book.title.replace(" ", "-")}"

  def metadataFilenameFor(book: Book) = s"${directoryNameFor(book)}/metadata.json"

  def filenameFor(book: Book, chapter: Chapter) = s"${directoryNameFor(book)}/${chapter.id}.xhtml"
  def filenameFor(book: Book, chapterId: Long) = s"${directoryNameFor(book)}/$chapterId.xhtml"
}

case class BookMetaData(title: String, description: String, etag: Option[String] = None)
case class TranslatedChapter(originalChapterId: Long, newChapterId: Option[Long], content: String, etag: Option[String] = None)