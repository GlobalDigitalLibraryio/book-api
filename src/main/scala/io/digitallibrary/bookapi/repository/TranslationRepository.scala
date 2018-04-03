/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import java.sql.PreparedStatement

import io.digitallibrary.language.model.LanguageTag
import io.digitallibrary.bookapi.model.api.OptimisticLockException
import io.digitallibrary.bookapi.model.domain.{Sort, _}
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

    def allAvailableLevelsWithStatus(publishingStatus: PublishingStatus.Value, language: Option[LanguageTag])(implicit session: DBSession = ReadOnlyAutoSession): Seq[String] = {
      select(sqls.distinct(t.result.readingLevel))
        .from(Translation as t)
          .where.eq(t.publishingStatus, publishingStatus.toString)
          .and(sqls.toAndConditionOpt(
          language.map(lang => sqls.eq(t.language, lang.toString))
        )).orderBy(t.readingLevel).toSQL.map(_.string(1)).list().apply()
    }

    def allAvailableCategoriesAndReadingLevelsWithStatus(publishingStatus: PublishingStatus.Value, language: LanguageTag)(implicit session: DBSession = ReadOnlyAutoSession): Map[Category, Set[String]] = {
      select(cat.id, cat.revision, cat.name, t.readingLevel)
        .from(Category as cat)
        .innerJoin(Translation as t)
        .on(sqls"${cat.id} = any(${t.categoryIds})")
        .where.eq(t.publishingStatus, publishingStatus.toString)
        .and.eq(t.language, language.toString)
        .toSQL
        .map(rs => (Category(id = rs.longOpt(1), revision = rs.intOpt(2), name = rs.string(3)), rs.string(4)))
        .list()
        .apply()
        .groupBy(_._1).mapValues(_.map(_._2).toSet)
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

      val id = insert.into(Translation)
        .namedValues(
          t.revision -> startRevision,
          t.bookId -> translation.bookId,
          t.externalId -> translation.externalId,
          t.uuid -> translation.uuid,
          t.title -> translation.title,
          t.about -> translation.about,
          t.numPages -> translation.numPages,
          t.language -> translation.language.toString,
          t.translatedFrom -> translation.translatedFrom.map(_.toString),
          t.datePublished -> translation.datePublished,
          t.dateCreated -> translation.dateCreated,
          t.dateArrived -> translation.dateArrived,
          t.publishingStatus -> translation.publishingStatus.toString,
          t.coverphoto -> translation.coverphoto,
          t.isBasedOnUrl -> translation.isBasedOnUrl,
          t.educationalUse -> translation.educationalUse,
          t.educationalRole -> translation.educationalRole,
          t.eaId -> translation.eaId,
          t.timeRequired -> translation.timeRequired,
          t.typicalAgeRange -> translation.typicalAgeRange,
          t.readingLevel -> translation.readingLevel,
          t.interactivityType -> translation.interactivityType,
          t.learningResourceType -> translation.learningResourceType,
          t.accessibilityApi -> translation.accessibilityApi,
          t.accessibilityControl -> translation.accessibilityControl,
          t.accessibilityFeature -> translation.accessibilityFeature,
          t.accessibilityHazard -> translation.accessibilityHazard,
          t.bookFormat -> translation.bookFormat.toString,
          t.tags -> tagBinder,
          t.categoryIds -> categoryBinder
        ).toSQL.updateAndReturnGeneratedKey().apply()

      translation.copy(id = Some(id), revision = Some(startRevision))
    }

    def updateTranslation(replacement: Translation)(implicit session: DBSession = AutoSession): Translation = {
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

      val count = update(Translation)
        .set(
          t.revision -> nextRevision,
          t.title -> replacement.title,
          t.about -> replacement.about,
          t.numPages -> replacement.numPages,
          t.language -> replacement.language.toString,
          t.translatedFrom -> replacement.translatedFrom.map(_.toString),
          t.datePublished -> replacement.datePublished,
          t.dateCreated -> replacement.dateCreated,
          t.dateArrived -> replacement.dateArrived,
          t.publishingStatus -> replacement.publishingStatus.toString,
          t.coverphoto -> replacement.coverphoto,
          t.isBasedOnUrl -> replacement.isBasedOnUrl,
          t.educationalUse -> replacement.educationalUse,
          t.educationalRole -> replacement.educationalRole,
          t.eaId -> replacement.eaId,
          t.timeRequired -> replacement.timeRequired,
          t.typicalAgeRange -> replacement.typicalAgeRange,
          t.readingLevel -> replacement.readingLevel,
          t.interactivityType -> replacement.interactivityType,
          t.learningResourceType -> replacement.learningResourceType,
          t.accessibilityApi -> replacement.accessibilityApi,
          t.accessibilityControl -> replacement.accessibilityControl,
          t.accessibilityFeature -> replacement.accessibilityFeature,
          t.accessibilityHazard -> replacement.accessibilityHazard,
          t.bookFormat -> replacement.bookFormat.toString,
          t.tags -> tagBinder,
          t.categoryIds -> categoryBinder
        )
        .where
        .eq(t.id, replacement.id)
        .and
        .eq(t.revision, replacement.revision)
        .toSQL
        .update().apply()

      if (count != 1) {
        throw new OptimisticLockException()
      } else {
        replacement.copy(revision = Some(nextRevision))
      }
    }

    def deleteTranslation(translation: Translation)(implicit session: DBSession = AutoSession): Unit = {
      val t = Translation.column
      deleteFrom(Translation).where.eq(t.id, translation.id).toSQL.update().apply()
    }

    private def getSorting(sortDef: Sort.Value) = sortDef match {
      case (Sort.ByIdAsc) => sqls.orderBy(t.id).asc
      case (Sort.ByIdDesc) => sqls.orderBy(t.id).desc
      case (Sort.ByTitleAsc) => sqls.orderBy(t.title).asc
      case (Sort.ByTitleDesc) => sqls.orderBy(t.title).desc
      case (Sort.ByArrivalDateAsc) => sqls.orderBy(t.dateArrived.asc, t.bookId.asc)
      case (Sort.ByArrivalDateDesc) => sqls.orderBy(t.dateArrived.desc, t.bookId.desc)
    }

    def translationsWithLanguageAndStatus(languageTag: LanguageTag, status: PublishingStatus.Value, limit: Int, offset: Int)(implicit session: DBSession = ReadOnlyAutoSession): List[Translation] = {
      select(t.result.id)
        .from(Translation as t)
        .where
        .eq(t.language, languageTag.toString())
        .and
        .eq(t.publishingStatus, status.toString)
        .orderBy(t.id)
        .limit(limit)
        .offset(offset)
        .toSQL.map(rs => withId(rs.long(1)).get).list().apply()
    }

    def numberOfTranslationsWithStatus(languageTag: LanguageTag, status: PublishingStatus.Value)(implicit session: DBSession = ReadOnlyAutoSession): Int = {
      select(sqls"count(*)")
        .from(Translation as t)
        .where
        .eq(t.language, languageTag.toString())
        .and
        .eq(t.publishingStatus, status.toString)
        .toSQL.map(rs => rs.int(1)).single().apply().getOrElse(0)
    }
  }

}
