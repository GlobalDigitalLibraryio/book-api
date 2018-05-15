/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain

import java.time.LocalDate

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.BookApiProperties
import org.json4s.FieldSerializer
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class Translation(id: Option[Long],
                       revision: Option[Int],
                       bookId: Long,
                       externalId: Option[String],
                       uuid: String,
                       title: String,
                       about: String,
                       numPages: Option[Int],
                       language: LanguageTag,
                       translatedFrom: Option[LanguageTag],
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
                       dateArrived: LocalDate,
                       publishingStatus: PublishingStatus.Value,
                       educationalAlignment: Option[EducationalAlignment] = None,
                       chapters: Seq[Chapter],
                       contributors: Seq[Contributor],
                       categories: Seq[Category],
                       bookFormat: BookFormat.Value,
                       pageOrientation: PageOrientation.Value)

object PageOrientation extends Enumeration {
  val PORTRAIT, LANDSCAPE = Value

  def valueOf(s: String): Try[PageOrientation.Value] = {
    PageOrientation.values.find(_.toString.equalsIgnoreCase(s)) match {
      case Some(x) => Success(x)
      case None => Failure(new RuntimeException(s"Unknown PageOrientation $s."))
    }
  }

  def valueOfOrDefault(s: String): PageOrientation.Value = {
    PageOrientation.values.find(_.toString.equalsIgnoreCase(s)).getOrElse(PORTRAIT)
  }

  def valueOfOrDefault(s: Option[String]): PageOrientation.Value = {
    s.map(valueOfOrDefault).getOrElse(PORTRAIT)
  }
}

object PublishingStatus extends Enumeration {
  val PUBLISHED, UNLISTED, FLAGGED = Value

  def valueOf(s: String): Try[PublishingStatus.Value] = {
    PublishingStatus.values.find(_.toString.equalsIgnoreCase(s)) match {
      case Some(x) => Success(x)
      case None => Failure(new RuntimeException(s"Unknown PublishingStatus $s."))
    }
  }

  def valueOfOrDefault(s: String): PublishingStatus.Value = {
    PublishingStatus.values.find(_.toString == s.toUpperCase).getOrElse(PUBLISHED)
  }
}

object BookFormat extends Enumeration {
  val HTML, PDF = Value

  def valueOfOrDefault(s: String): BookFormat.Value = {
    BookFormat.values.find(_.toString == s.toUpperCase).getOrElse(HTML)
  }
}

sealed trait TranslationView extends SQLSyntaxSupport[Translation] {
  implicit val formats = org.json4s.DefaultFormats
  val JSonSerializer = FieldSerializer[Translation]()

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
    language = LanguageTag(rs.string(t.language)),
    translatedFrom = rs.stringOpt(t.translatedFrom).map(LanguageTag(_)),
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
    dateArrived = rs.localDate(t.dateArrived),
    publishingStatus = PublishingStatus.valueOfOrDefault(rs.string(t.publishingStatus)),
    bookFormat = BookFormat.valueOfOrDefault(rs.string(t.bookFormat)),
    pageOrientation = PageOrientation.valueOfOrDefault(rs.string(t.pageOrientation)),
    chapters = Seq(),
    contributors = Seq(),
    categories = Seq()
  )

  def apply(t: ResultName[Translation], ea: ResultName[EducationalAlignment])(rs: WrappedResultSet): Translation = {
    val translation = apply(t)(rs)
    translation.copy(educationalAlignment = translation.eaId.map(_ => EducationalAlignment.apply(ea)(rs)))
  }
}


object UnflaggedTranslations extends TranslationView {
  override val schemaName = Some(BookApiProperties.MetaSchema)
  override val tableName = "translation_not_flagged"
}

object AllTranslations extends TranslationView {
  override val schemaName = Some(BookApiProperties.MetaSchema)
  override val tableName = "translation"
}