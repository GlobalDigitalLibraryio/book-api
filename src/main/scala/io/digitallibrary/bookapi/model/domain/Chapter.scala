/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

import io.digitallibrary.bookapi.BookApiProperties
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class Chapter(id: Option[Long],
                   revision: Option[Int],
                   translationId: Long,
                   seqNo: Int,
                   title: Option[String],
                   content: String,
                   chapterType: ChapterType.Value) {

  def imagesInChapter(): Seq[Long] = {
    val document = Jsoup.parseBodyFragment(content)
    val images: Elements = document.select("embed[data-resource='image']")

    for {
      i <- 0 until images.size()
      image = images.get(i)
      nodeId = image.attr("data-resource_id")
    } yield nodeId.toLong
  }
}

object ChapterType extends Enumeration {
  val Content, License, Cover, BackCover = Value

  def valueOf(s: String): Try[ChapterType.Value] = {
    ChapterType.values.find(_.toString.equalsIgnoreCase(s)) match {
      case Some(x) => Success(x)
      case None => Failure(new RuntimeException(s"Unknown ChapterType $s."))
    }
  }

  def valueOfOrDefault(s: String): ChapterType.Value = {
    valueOf(s).getOrElse(ChapterType.Content)
  }

  def valueOfOrDefault(s: Option[String]): ChapterType.Value = {
    s.map(valueOfOrDefault).getOrElse(Content)
  }
}

object Chapter extends SQLSyntaxSupport[Chapter] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "chapter"
  override val schemaName = Some(BookApiProperties.MetaSchema)


  def apply(ch: SyntaxProvider[Chapter])(rs: WrappedResultSet): Chapter = apply(ch.resultName)(rs)

  def apply(ch: ResultName[Chapter])(rs: WrappedResultSet): Chapter = Chapter(
    rs.longOpt(ch.id),
    rs.intOpt(ch.revision),
    rs.long(ch.translationId),
    rs.int(ch.seqNo),
    rs.stringOpt(ch.title),
    rs.string(ch.content),
    ChapterType.valueOf(rs.string(ch.chapterType)).get
  )

  def opt(ch: SyntaxProvider[Chapter])(rs: WrappedResultSet): Option[Chapter] =
    rs.longOpt(ch.resultName.id).map(_ => Chapter(ch)(rs))

}