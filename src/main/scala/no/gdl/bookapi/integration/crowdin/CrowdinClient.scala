/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration.crowdin

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.GdlClient
import no.gdl.bookapi.model.api.{Book, Chapter, CrowdinException}
import no.gdl.bookapi.model.crowdin._
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

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
  case class BookMetaData(title: String, description: String)

  def getProjectIdentifier: String = projectIdentifier

  def addBookMetadata(book: Book): Try[CrowdinFile] = {
    implicit val formats: DefaultFormats = DefaultFormats

    val metadata = BookMetaData(book.title, book.description)
    val filename = CrowdinUtils.metadataFilenameFor(book)

    val response = gdlClient
      .fetch[AddFilesResponse](Http(AddFileUrl)
      .postMulti(MultiPart(s"files[$filename]", filename, "application/json", write(metadata).getBytes)))

    response.map(res => CrowdinFile(None, "metadata", res.stats.get.files.head)) match {
      case Success(x) => Success(x)
      case Failure(ex) => Failure(new CrowdinException(ex))
    }
  }

  def addChaptersFor(book: Book, chapters: Seq[Chapter]): Try[Seq[CrowdinFile]] = {
    val uploadTries: Seq[Try[AddFilesResponse]] = chapters.sliding(20,20).toList.map(window => {
      val multiParts = window.map(chapter => {
        val filename = CrowdinUtils.filenameFor(book, chapter)
        MultiPart(s"files[$filename]", filename, "application/xhtml+xml", chapter.content.getBytes)
      })

      gdlClient.fetch[AddFilesResponse](Http(AddFileUrl).postMulti(multiParts:_*))
    })

    val httpExceptions = uploadTries.filter(_.isFailure).map(_.failed.get)
    val crowdinExceptions = uploadTries.filter(req => req.isSuccess && !req.get.success).map(_.get.error.get)

    if(httpExceptions.nonEmpty) {
      Failure(new CrowdinException(httpExceptions.head))
    } else if (crowdinExceptions.nonEmpty) {
      Failure(new CrowdinException(crowdinExceptions.head.code, crowdinExceptions.head.message))
    } else {
      val result = uploadTries.flatMap(_.get.stats.get.files).map(fil => {
        val chapterForFile = chapters
          .find(chapter => CrowdinUtils.filenameFor(book, chapter) == fil.name)
          .getOrElse(throw new RuntimeException("Inconsistent file-result"))

        CrowdinFile(Some(chapterForFile.id), "content", fil)
      })
      Success(result)
    }
  }

  def addDirectoryFor(book: Book): Try[String] = {
    val directoryName = CrowdinUtils.directoryNameFor(book)

    gdlClient.fetch[AddDirectoryResponse](Http(AddDirectoryUrl).postForm(Seq("name" -> directoryName)))
      .map(_ => directoryName) match {
        case Success(x) => Success(x)
        case Failure(ex) => Failure(new CrowdinException(ex))
    }
  }

  def deleteDirectoryFor(book: Book): Try[Unit] = {
    val result = gdlClient.fetch[DeleteDirectoryResponse](Http(DeleteDirectoryUrl).postForm(Seq("name" -> CrowdinUtils.directoryNameFor(book))))
    result match {
      case Success(x) if x.success => Success()
      case Success(y) if y.dirNotFoundError => Success()
      case Success(z) if !z.success => Failure(new CrowdinException(z.error.get.code, z.error.get.message))
      case Failure(err) => Failure(new CrowdinException(err))
    }
  }

  private val ProjectDetailsUrl = s"$CrowdinBaseUrl/project/$projectIdentifier/info?key=$projectKey&json"
  private val EditProjectUrl = s"$CrowdinBaseUrl/project/$projectIdentifier/edit-project?key=$projectKey&json"
  private val AddDirectoryUrl = s"$CrowdinBaseUrl/project/$projectIdentifier/add-directory?key=$projectKey&json"
  private val DeleteDirectoryUrl = s"$CrowdinBaseUrl/project/$projectIdentifier/delete-directory?key=$projectKey&json"
  private val AddFileUrl = s"$CrowdinBaseUrl/project/$projectIdentifier/add-file?key=$projectKey&json"


  def addTargetLanguage(toLanguage: String): Try[Unit] = {
    getTargetLanguages.flatMap(languages => {
      if (!languages.exists(_.code == toLanguage)) {
        val newCodeList = languages.map(_.code) :+ toLanguage
        val postParams = newCodeList.zipWithIndex.map(x => (s"languages[${x._2}]", x._1.toString))

        gdlClient.fetch[EditProjectResponse](Http(EditProjectUrl).postForm(postParams)) match {
          case Success(x) if x.project.isDefined && x.project.get.success => Success()
          case Success(y) if y.error.isDefined => Failure(new CrowdinException(y.error.get.code, y.error.get.message))
          case Failure(ex) => Failure(new CrowdinException(ex))
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
      case Failure(ex) => Failure(new CrowdinException(ex))
    }
  }

}

object CrowdinUtils {
  private val CrowdinDirectoryUrl = "https://crowdin.com/project/{PROJECT_IDENTIFIER}/{LANGUAGE_CODE}#/{DIRECTORY_NAME}"

  def crowdinUrlToBook(book: Book, crowdinProjectId: String, toLanguage: String): String =
    CrowdinDirectoryUrl
      .replace("{PROJECT_IDENTIFIER}", crowdinProjectId)
      .replace("{LANGUAGE_CODE}", toLanguage)
      .replace("{DIRECTORY_NAME}", directoryNameFor(book))

  def directoryNameFor(book: Book) = s"${book.id}-${book.title.replace(" ", "-")}"
  def metadataFilenameFor(book: Book) = s"${directoryNameFor(book)}/metadata.json"
  def filenameFor(book: Book, chapter: Chapter) = s"${directoryNameFor(book)}/${chapter.id}.xhtml"
}

