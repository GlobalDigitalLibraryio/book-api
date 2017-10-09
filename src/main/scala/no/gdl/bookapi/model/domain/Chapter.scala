/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities.EscapeMode
import org.jsoup.select.Elements
import scalikejdbc._

case class Chapter(id: Option[Long],
                   revision: Option[Int],
                   translationId: Long,
                   seqNo: Int,
                   title: Option[String],
                   content: String) {

  def imagesInChapter(): Seq[Long] = {
    import com.netaporter.uri.dsl._

    val document = Jsoup.parseBodyFragment(content)
    val images: Elements = document.select("embed[data-resource='image']")
    var imageIds: Seq[Long] = Seq()

    for (i <- 0 until images.size()) {
      val image = images.get(i)
      val nodeId = image.attr("data-resource_id")
      imageIds = imageIds :+ nodeId.toLong
    }

    imageIds
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
    rs.string(ch.content)
  )

  def opt(ch: SyntaxProvider[Chapter])(rs: WrappedResultSet): Option[Chapter] =
    rs.longOpt(ch.resultName.id).map(_ => Chapter(ch)(rs))

}