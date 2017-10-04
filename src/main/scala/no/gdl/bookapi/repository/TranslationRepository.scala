/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.sql.PreparedStatement

import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain._
import scalikejdbc._

trait TranslationRepository {
  this: ContributorRepository =>
  val translationRepository: TranslationRepository

  class TranslationRepository {
    private val (t, ch, ea, ctb, p, cat) = (
      Translation.syntax,
      Chapter.syntax,
      EducationalAlignment.syntax,
      Contributor.syntax,
      Person.syntax,
      Category.syntax)

    def languagesFor(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[String] = {
      select(t.result.language)
        .from(Translation as t)
        .where.eq(t.bookId, id).toSQL
        .map(_.string(1)).list().apply()
    }

    def bookIdsWithLanguage(language: String, pageSize: Int, page: Int)(implicit session: DBSession = ReadOnlyAutoSession): SearchResult[Long] = {
      val limit = pageSize.max(1)
      val offset = (page.max(1) - 1) * pageSize

      val results = select(t.result.bookId)
        .from(Translation as t)
        .where.eq(t.language, language).limit(limit).offset(offset)
        .toSQL
        .map(_.long(1)).list().apply()

      SearchResult[Long](results.length, page, pageSize, language, results)
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

      val count =
        sql"""
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

}