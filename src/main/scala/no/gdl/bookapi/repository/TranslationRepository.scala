/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.sql.PreparedStatement
import java.time.LocalDate

import io.digitallibrary.language.model.LanguageTag
import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.{Sort, _}
import scalikejdbc._

trait TranslationRepository {
  this: ContributorRepository =>
  val translationRepository: TranslationRepository

  val countAllBeforeLimiting = sqls"count(*) over ()"

  class TranslationRepository {
    private val (t, ch, ea, ctb, p, cat) = (
      Translation.syntax,
      Chapter.syntax,
      EducationalAlignment.syntax,
      Contributor.syntax,
      Person.syntax,
      Category.syntax)

    def latestArrivalDateFor(language: LanguageTag, readingLevel: String = null)(implicit session: DBSession = ReadOnlyAutoSession): LocalDate = {
      val levelOpt = Option(readingLevel)
      select(sqls.max(t.dateArrived))
        .from(Translation as t)
        .where.eq(t.language, language.toString)
        .and(
          sqls.toAndConditionOpt(levelOpt.map(l => sqls.eq(t.readingLevel, l))))
        .toSQL
        .map(_.localDate(1))
        .single().apply().get
    }

    def languagesFor(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[LanguageTag] = {
      select(t.result.language)
        .from(Translation as t)
        .where.eq(t.bookId, id)
        .and.eq(t.publishingStatus, PublishingStatus.PUBLISHED.toString)
        .toSQL
        .map(rs => LanguageTag(rs.string(1))).list().apply()
    }

    def bookIdsWithLanguageAndStatus(language: LanguageTag, publishingStatus: PublishingStatus.Value, pageSize: Int, page: Int, sortDef: Sort.Value)(implicit session: DBSession = ReadOnlyAutoSession): SearchResult[Long] = {
      val limit = pageSize.max(1)
      val offset = (page.max(1) - 1) * pageSize

      val result =
        select(countAllBeforeLimiting, t.result.bookId)
        .from(Translation as t)
        .where.eq(t.language, language.toString)
        .and.eq(t.publishingStatus, publishingStatus.toString)
        .append(getSorting(sortDef))
        .limit(limit).offset(offset)
        .toSQL
        .map(row => (row.long(1), row.long(2))).list().apply()

      SearchResult[Long](result.map(_._1).headOption.getOrElse(0), page, pageSize, language, result.map(_._2))
    }

    def bookIdsWithLanguageAndLevelAndStatus(language: LanguageTag, readingLevel: Option[String], publishingStatus: PublishingStatus.Value, pageSize: Int, page: Int, sortDef: Sort.Value)(implicit session: DBSession = ReadOnlyAutoSession): SearchResult[Long] = {
      val limit = pageSize.max(1)
      val offset = (page.max(1) - 1) * pageSize

      val result = select(countAllBeforeLimiting, t.result.bookId)
        .from(Translation as t)
        .where
        .eq(t.language, language.toString)
        .and.eq(t.publishingStatus, publishingStatus.toString)
        .and(
          sqls.toAndConditionOpt(readingLevel.map(l => sqls.eq(t.readingLevel, l))))
        .append(getSorting(sortDef))
        .limit(limit).offset(offset).toSQL
        .map(row => (row.long(1), row.long(2))).list().apply()

      SearchResult[Long](result.map(_._1).headOption.getOrElse(0), page, pageSize, language, result.map(_._2))
    }

    def forBookIdAndLanguage(bookId: Long, language: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Option[Translation] = {
      val translationWithChapters = select
        .from(Translation as t)
        .leftJoin(EducationalAlignment as ea).on(t.eaId, t.id)
        .leftJoin(Chapter as ch).on(t.id, ch.translationId)
        .where.eq(t.bookId, bookId)
        .and.eq(t.language, language.toString).toSQL
        .one(Translation(t))
        .toManies(
          rs => EducationalAlignment.opt(ea)(rs),
          rs => Chapter.opt(ch)(rs)
        )
        .map { (tra, eaT, ch) => tra.copy(educationalAlignment = eaT.headOption, chapters = ch) }.single().apply()

      val translationWithContributors = translationWithChapters.map(t => {
        t.copy(contributors = t.id match {
          case None => Seq()
          case Some(translationId) => contributorRepository.forTranslationId(translationId)
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
          case Some(id) => contributorRepository.forTranslationId(id)
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

    def withUuId(uuid: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Translation] = {
      val translationWithChapters = select
        .from(Translation as t)
        .leftJoin(EducationalAlignment as ea).on(t.eaId, t.id)
        .leftJoin(Chapter as ch).on(t.id, ch.translationId)
        .where.eq(t.uuid, uuid).toSQL
        .one(Translation(t))
        .toManies(
          rs => EducationalAlignment.opt(ea)(rs),
          rs => Chapter.opt(ch)(rs)
        )
        .map { (tra, eaT, ch) => tra.copy(educationalAlignment = eaT.headOption, chapters = ch) }.single().apply()

      val translationWithContributors = translationWithChapters.map(t => {
        t.copy(contributors = t.id match {
          case None => Seq()
          case Some(id) => contributorRepository.forTranslationId(id)
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
          case Some(translationId) => contributorRepository.forTranslationId(translationId)
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

    def allAvailableLanguagesWithStatus(publishingStatus: PublishingStatus.Value)(implicit session: DBSession = ReadOnlyAutoSession): Seq[LanguageTag] = {
      select(sqls.distinct(t.result.language))
        .from(Translation as t)
          .where.eq(t.publishingStatus, publishingStatus.toString)
        .toSQL
        .map(rs => LanguageTag(rs.string(1))).list().apply()
    }

    def allAvailableLevels(language: Option[LanguageTag])(implicit session: DBSession = ReadOnlyAutoSession): Seq[String] = {
      select(sqls.distinct(t.result.readingLevel))
        .from(Translation as t)
        .where(sqls.toAndConditionOpt(
          language.map(lang => sqls.eq(t.language, lang.toString))
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

      val id =
        sql"""
              insert into ${Translation.table} (
                ${t.revision},
                ${t.bookId},
                ${t.externalId},
                ${t.uuid},
                ${t.title},
                ${t.about},
                ${t.numPages},
                ${t.language},
                ${t.translatedFrom},
                ${t.datePublished},
                ${t.dateCreated},
                ${t.dateArrived},
                ${t.publishingStatus},
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
               ${translation.language.toString},
               ${translation.translatedFrom.map(_.toString)},
               ${translation.datePublished},
               ${translation.dateCreated},
               ${translation.dateArrived},
               ${translation.publishingStatus.toString},
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

      val count =
        sql"""
      update ${Translation.table} set
        ${t.revision} = ${nextRevision},
        ${t.title} = ${replacement.title},
        ${t.about} = ${replacement.about},
        ${t.numPages} = ${replacement.numPages},
        ${t.language} = ${replacement.language.toString},
        ${t.translatedFrom} = ${replacement.translatedFrom.map(_.toString)},
        ${t.datePublished} = ${replacement.datePublished},
        ${t.dateCreated} = ${replacement.dateCreated},
        ${t.dateArrived} = ${replacement.dateArrived},
        ${t.publishingStatus} = ${replacement.publishingStatus.toString},
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

    private def getSorting(sortDef: Sort.Value) = sortDef match {
      case (Sort.ByIdAsc) => sqls.orderBy(t.id).asc
      case (Sort.ByIdDesc) => sqls.orderBy(t.id).desc
      case (Sort.ByTitleAsc) => sqls.orderBy(t.title).asc
      case (Sort.ByTitleDesc) => sqls.orderBy(t.title).desc
      case (Sort.ByArrivalDateAsc) => sqls.orderBy(t.dateArrived.asc, t.bookId.asc)
      case (Sort.ByArrivalDateDesc) => sqls.orderBy(t.dateArrived.desc, t.bookId.desc)
    }
  }

}
