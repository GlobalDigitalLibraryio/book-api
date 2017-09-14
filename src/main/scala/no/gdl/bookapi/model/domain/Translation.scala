/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import java.time.LocalDate

import no.gdl.bookapi.BookApiProperties
import scalikejdbc._

case class Translation(id: Option[Long],
                       revision: Option[Int],
                       bookId: Long,
                       externalId: Option[String],
                       uuid: String,
                       title: String,
                       about: String,
                       numPages: Option[Int],
                       language: String,
                       datePublished: Option[LocalDate],
                       dateCreated: Option[LocalDate],
                       categoryIds: Seq[Long],
                       coverphoto: Option[Long],
                       tags: Seq[String],
                       isBasedOnUrl: Option[String],
                       educationalUse: Option[String],
                       educationalRole: Option[String],
                       eaId: Option[Long],
                       timeRequired: Option[String],
                       typicalAgeRange: Option[String],
                       readingLevel: Option[String],
                       interactivityType: Option[String],
                       learningResourceType: Option[String],
                       accessibilityApi: Option[String],
                       accessibilityControl: Option[String],
                       accessibilityFeature: Option[String],
                       accessibilityHazard: Option[String],
                       educationalAlignment: Option[EducationalAlignment] = None,
                       chapters: Seq[Chapter],
                       contributors: Seq[Contributor],
                       categories: Seq[Category])

object Translation extends SQLSyntaxSupport[Translation] {
  import scala.collection.JavaConverters._

  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "translation"
  override val schemaName = Some(BookApiProperties.MetaSchema)

  def apply(t: SyntaxProvider[Translation])(rs: WrappedResultSet): Translation = apply(t.resultName)(rs)
  def apply(t: ResultName[Translation])(rs: WrappedResultSet): Translation = Translation(
    id = rs.longOpt(t.id),
    revision = rs.intOpt(t.revision),
    bookId = rs.long(t.bookId),
    externalId = rs.stringOpt(t.externalId),
    uuid = rs.string(t.uuid),
    title = rs.string(t.title),
    about = rs.string(t.about),
    numPages = rs.intOpt(t.numPages),
    language = rs.string(t.language),
    datePublished = rs.localDateOpt(t.datePublished),
    dateCreated = rs.localDateOpt(t.dateCreated),
    categoryIds = rs.array(t.categoryIds).getArray().asInstanceOf[Array[java.lang.Long]].toSeq.map(_.toLong),
    coverphoto = rs.longOpt(t.coverphoto),
    tags = rs.arrayOpt(t.tags).map(_.getArray.asInstanceOf[Array[String]].toList).getOrElse(Seq()),
    isBasedOnUrl = rs.stringOpt(t.isBasedOnUrl),
    educationalUse = rs.stringOpt(t.educationalUse),
    educationalRole = rs.stringOpt(t.educationalRole),
    eaId = rs.longOpt(t.eaId),
    timeRequired = rs.stringOpt(t.timeRequired),
    typicalAgeRange = rs.stringOpt(t.typicalAgeRange),
    readingLevel = rs.stringOpt(t.readingLevel),
    interactivityType = rs.stringOpt(t.interactivityType),
    learningResourceType = rs.stringOpt(t.learningResourceType),
    accessibilityApi = rs.stringOpt(t.accessibilityApi),
    accessibilityControl = rs.stringOpt(t.accessibilityControl),
    accessibilityFeature = rs.stringOpt(t.accessibilityFeature),
    accessibilityHazard = rs.stringOpt(t.accessibilityHazard),
    chapters = Seq(),
    contributors = Seq(),
    categories = Seq()
  )

  def apply(t: ResultName[Translation], ea: ResultName[EducationalAlignment])(rs: WrappedResultSet): Translation = {
    val translation = apply(t)(rs)
    translation.copy(educationalAlignment = translation.eaId.map(_ => EducationalAlignment.apply(ea)(rs)))
  }
}