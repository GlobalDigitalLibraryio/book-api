/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import java.sql.PreparedStatement
import java.time.LocalDate

import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.Translation.{ch, t}
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
  private val (t, ch, ea, ctb, p, cat) = (
    Translation.syntax,
    Chapter.syntax,
    EducationalAlignment.syntax,
    Contributor.syntax,
    Person.syntax,
    Category.syntax)

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

  def languagesFor(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[String] = {
    select(t.result.language)
      .from(Translation as t)
      .where.eq(t.bookId, id).toSQL
      .map(_.string(1)).list().apply()
  }

  def bookIdsWithLanguage(language: String, pageSize: Int, page: Int)(implicit session: DBSession = ReadOnlyAutoSession): SearchResult[Long] = {
    val limit = pageSize.max(1)
    val offset = (page.max(1) - 1) * pageSize

    val num_matching = select(sqls.count)
      .from(Translation as t)
      .where.eq(t.language, language).toSQL
      .map(_.long(1)).single().apply()

    val results = select(t.result.bookId)
      .from(Translation as t)
      .where.eq(t.language, language).limit(limit).offset(offset)
      .toSQL
      .map(_.long(1)).list().apply()

    SearchResult[Long](num_matching.getOrElse(0), page, pageSize, language, results)
  }

  def bookIdsWithLanguageAndLevel(language: String, readingLevel: Option[String], pageSize: Int, page: Int)(implicit session: DBSession = ReadOnlyAutoSession): SearchResult[Long] = {
    val limit = pageSize.max(1)
    val offset = (page.max(1) - 1) * pageSize

    val num_matching = select(sqls.count)
      .from(Translation as t)
      .where
      .eq(t.language, language)
      .and(
        sqls.toAndConditionOpt(readingLevel.map(l => sqls.eq(t.readingLevel, l))))
      .toSQL.map(_.long(1)).single().apply()

    val results = select(t.result.bookId)
      .from(Translation as t)
      .where
      .eq(t.language, language)
      .and(
        sqls.toAndConditionOpt(readingLevel.map(l => sqls.eq(t.readingLevel, l))))
      .limit(limit).offset(offset).toSQL
      .map(_.long(1)).list().apply()

    SearchResult[Long](num_matching.getOrElse(0), page, pageSize, language, results)

  }

  def forBookIdAndLanguage(bookId: Long, language: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Translation] = {
    val translationWithChapters = select
      .from(Translation as t)
      .leftJoin(EducationalAlignment as ea).on(t.eaId, t.id)
      .leftJoin(Chapter as ch).on(t.id, ch.translationId)
      .where.eq(t.bookId, bookId)
      .and.eq(t.language, language).toSQL
      .one(Translation(t))
      .toManies(
        rs => EducationalAlignment.opt(ea)(rs),
        rs => Chapter.opt(ch)(rs)
      )
      .map { (tra, eaT, ch) => tra.copy(educationalAlignment = eaT.headOption, chapters = ch) }.single().apply()

    val translationWithContributors = translationWithChapters.map(t => {
      t.copy(contributors = t.id match {
        case None => Seq()
        case Some(translationId) => Contributor.forTranslationId(translationId)
      })
    })

    val translationWithCategories = translationWithContributors.map(t => {
      val categories = select
        .from(Category as cat)
        .where.in(cat.id, t.categoryIds).toSQL
        .map(Category(cat)).list().apply()

      t.copy(categories = categories)
    })

    translationWithCategories
  }

  def withId(translationId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Translation] = {
    val translationWithChapters = select
      .from(Translation as t)
      .leftJoin(EducationalAlignment as ea).on(t.eaId, t.id)
      .leftJoin(Chapter as ch).on(t.id, ch.translationId)
      .where.eq(t.id, translationId).toSQL
      .one(Translation(t))
      .toManies(
        rs => EducationalAlignment.opt(ea)(rs),
        rs => Chapter.opt(ch)(rs)
      )
      .map { (tra, eaT, ch) => tra.copy(educationalAlignment = eaT.headOption, chapters = ch) }.single().apply()

    val translationWithContributors = translationWithChapters.map(t => {
      t.copy(contributors = t.id match {
        case None => Seq()
        case Some(id) => Contributor.forTranslationId(id)
      })
    })

    val translationWithCategories = translationWithContributors.map(t => {
      val categories = select
        .from(Category as cat)
        .where.in(cat.id, t.categoryIds).toSQL
        .map(Category(cat)).list().apply()

      t.copy(categories = categories)
    })

    translationWithCategories
  }

  def withExternalId(externalId: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Translation] = {
    val translationWithChapters = select
      .from(Translation as t)
      .leftJoin(EducationalAlignment as ea).on(t.eaId, t.id)
      .leftJoin(Chapter as ch).on(t.id, ch.translationId)
      .where.eq(t.externalId, externalId).toSQL
      .one(Translation(t))
      .toManies(
        rs => EducationalAlignment.opt(ea)(rs),
        rs => Chapter.opt(ch)(rs)
      )
      .map { (tra, eaT, ch) => tra.copy(educationalAlignment = eaT.headOption, chapters = ch) }.single().apply()

    val translationWithContributors = translationWithChapters.map(t => {
      t.copy(contributors = t.id match {
        case None => Seq()
        case Some(translationId) => Contributor.forTranslationId(translationId)
      })
    })

    val translationWithCategories = translationWithContributors.map(t => {
      val categories = select
        .from(Category as cat)
        .where.in(cat.id, t.categoryIds).toSQL
        .map(Category(cat)).list().apply()

      t.copy(categories = categories)
    })

    translationWithCategories
  }

  def allAvailableLanguages()(implicit session: DBSession = ReadOnlyAutoSession): Seq[String] = {
    select(sqls.distinct(t.result.language))
      .from(Translation as t).toSQL
      .map(_.string(1)).list().apply()
  }

  def allAvailableLevels(language: Option[String])(implicit session: DBSession = ReadOnlyAutoSession): Seq[String] = {
    select(sqls.distinct(t.result.readingLevel))
      .from(Translation as t)
      .where(sqls.toAndConditionOpt(
        language.map(lang => sqls.eq(t.language, lang))
      )).orderBy(t.readingLevel).toSQL.map(_.string(1)).list().apply()
  }

  def add(translation: Translation)(implicit session: DBSession = AutoSession): Translation = {
    import collection.JavaConverters._

    val t = Translation.column
    val startRevision = 1

    val categoryBinder = ParameterBinder(
      value = translation.categoryIds,
      binder = (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("bigint", translation.categoryIds.asJava.toArray))
    )

    val tagBinder = ParameterBinder(
      value = translation.tags,
      binder = (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("text", translation.tags.asJava.toArray))
    )

    val id = sql"""
              insert into ${Translation.table} (
                ${t.revision},
                ${t.bookId},
                ${t.externalId},
                ${t.uuid},
                ${t.title},
                ${t.about},
                ${t.numPages},
                ${t.language},
                ${t.datePublished},
                ${t.dateCreated},
                ${t.coverphoto},
                ${t.isBasedOnUrl},
                ${t.educationalUse},
                ${t.educationalRole},
                ${t.eaId},
                ${t.timeRequired},
                ${t.typicalAgeRange},
                ${t.readingLevel},
                ${t.interactivityType},
                ${t.learningResourceType},
                ${t.accessibilityApi},
                ${t.accessibilityControl},
                ${t.accessibilityFeature},
                ${t.accessibilityHazard},
                ${t.tags},
                ${t.categoryIds})
              values (
               ${startRevision},
               ${translation.bookId},
               ${translation.externalId},
               ${translation.uuid},
               ${translation.title},
               ${translation.about},
               ${translation.numPages},
               ${translation.language},
               ${translation.datePublished},
               ${translation.dateCreated},
               ${translation.coverphoto},
               ${translation.isBasedOnUrl},
               ${translation.educationalUse},
               ${translation.educationalRole},
               ${translation.eaId},
               ${translation.timeRequired},
               ${translation.typicalAgeRange},
               ${translation.readingLevel},
               ${translation.interactivityType},
               ${translation.learningResourceType},
               ${translation.accessibilityApi},
               ${translation.accessibilityControl},
               ${translation.accessibilityFeature},
               ${translation.accessibilityHazard},
               ${tagBinder},
               ${categoryBinder})""".updateAndReturnGeneratedKey().apply()

    translation.copy(id = Some(id), revision = Some(startRevision))
  }

  def update(replacement: Translation)(implicit session: DBSession = AutoSession): Translation = {
    import collection.JavaConverters._

    val t = Translation.column
    val nextRevision = replacement.revision.getOrElse(0) + 1

    val categoryBinder = ParameterBinder(
      value = replacement.categoryIds,
      binder = (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("bigint", replacement.categoryIds.asJava.toArray))
    )

    val tagBinder = ParameterBinder(
      value = replacement.tags,
      binder = (stmt: PreparedStatement, idx: Int) => stmt.setArray(idx, stmt.getConnection.createArrayOf("text", replacement.tags.asJava.toArray))
    )

    val count = sql"""
      update ${Translation.table} set
        ${t.revision} = ${nextRevision},
        ${t.title} = ${replacement.title},
        ${t.about} = ${replacement.about},
        ${t.numPages} = ${replacement.numPages},
        ${t.language} = ${replacement.language},
        ${t.datePublished} = ${replacement.datePublished},
        ${t.dateCreated} = ${replacement.dateCreated},
        ${t.coverphoto} = ${replacement.coverphoto},
        ${t.isBasedOnUrl} = ${replacement.isBasedOnUrl},
        ${t.educationalUse} = ${replacement.educationalUse},
        ${t.educationalRole} = ${replacement.educationalRole},
        ${t.eaId} = ${replacement.eaId},
        ${t.timeRequired} = ${replacement.timeRequired},
        ${t.typicalAgeRange} = ${replacement.typicalAgeRange},
        ${t.readingLevel} = ${replacement.readingLevel},
        ${t.interactivityType} = ${replacement.interactivityType},
        ${t.learningResourceType} = ${replacement.learningResourceType},
        ${t.accessibilityApi} = ${replacement.accessibilityApi},
        ${t.accessibilityControl} = ${replacement.accessibilityControl},
        ${t.accessibilityFeature} = ${replacement.accessibilityFeature},
        ${t.accessibilityHazard} = ${replacement.accessibilityHazard},
        ${t.tags} = ${tagBinder},
        ${t.categoryIds} = ${categoryBinder}
       where ${t.id} = ${replacement.id}
       and ${t.revision} = ${replacement.revision}
      """.update().apply()

    if (count != 1) {
      throw new OptimisticLockException()
    } else {
      replacement.copy(revision = Some(nextRevision))
    }
  }
}