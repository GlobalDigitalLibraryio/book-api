/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import java.sql.PreparedStatement
import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.integration.DataSource
import no.gdl.bookapi.model.domain._
import scalikejdbc._

import scala.util.Try

trait BooksRepository {
  this: DataSource =>
  val booksRepository: BooksRepository

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null): A = {
    Option(session) match {
      case Some(x) => work(x)
      case None => {
        DB localTx { implicit newSession =>
          work(newSession)
        }
      }
    }
  }

  class BooksRepository extends LazyLogging {
    def newChapter(newChapter: Chapter)(implicit session: DBSession = AutoSession): Chapter = {
      val ch = Chapter.column
      val startRevision = 1

      val id = insert.into(Chapter).namedValues(
        ch.translationId -> newChapter.translationId,
        ch.seqNo -> newChapter.seqNo,
        ch.title -> newChapter.title,
        ch.content -> newChapter.content
      ).toSQL.updateAndReturnGeneratedKey().apply()

      newChapter.copy(id = Some(id), revision = Some(startRevision))
    }


    def newContributor(contributor: Contributor)(implicit session: DBSession = AutoSession): Contributor = {
      val ctb = Contributor.column
      val startRevision = 1

      val id = insert.into(Contributor).namedValues(
        ctb.revision -> startRevision,
        ctb.personId -> contributor.person.id.get,
        ctb.translationId -> contributor.translationId,
        ctb.`type` -> contributor.`type`
      ).toSQL.updateAndReturnGeneratedKey().apply()

      contributor.copy(id = Some(id), revision = Some(startRevision))
    }

    def newTranslation(translation: Translation)(implicit session: DBSession = AutoSession): Translation = {
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

    def newPerson(person: Person)(implicit session: DBSession = AutoSession): Person = {
      val p = Person.column
      val startRevision = 1

      val id = insert.into(Person).namedValues(
        p.revision -> startRevision,
        p.name -> person.name
      ).toSQL.updateAndReturnGeneratedKey().apply()

      person.copy(id = Some(id), revision = Some(startRevision))
    }

    def newEducationalAlignment(educationalAlignment: EducationalAlignment)(implicit session: DBSession = AutoSession): EducationalAlignment = {
      val ea = EducationalAlignment.column
      val startRevision = 1

      val id = insert.into(EducationalAlignment).namedValues(
        ea.revision -> startRevision,
        ea.alignmentType -> educationalAlignment.alignmentType,
        ea.educationalFramework -> educationalAlignment.educationalFramework,
        ea.targetDescription -> educationalAlignment.targetDescription,
        ea.targetName -> educationalAlignment.targetName,
        ea.targetUrl -> educationalAlignment.targetUrl
      ).toSQL.updateAndReturnGeneratedKey().apply()

      educationalAlignment.copy(id = Some(id), revision = Some(startRevision))
    }

    def newCategory(newCategory: Category)(implicit session: DBSession = AutoSession): Category = {
      val c = Category.column
      val startRevision = 1

      val id = insert.into(Category).namedValues(
        c.revision -> startRevision,
        c.name -> newCategory.name
      ).toSQL.updateAndReturnGeneratedKey().apply()

      newCategory.copy(id = Some(id), revision = Some(startRevision))
    }


    def newBook(newBook: Book)(implicit session: DBSession = AutoSession): Try[Book] = {
      val b = Book.column
      val startRevision = 1

      Try{
        val id = insert.into(Book).namedValues(
          b.revision -> startRevision,
          b.publisherId -> newBook.publisherId,
          b.licenseId -> newBook.licenseId
        ).toSQL
        .updateAndReturnGeneratedKey()
        .apply()

        newBook.copy(id = Some(id), revision = Some(startRevision))
      }
    }

    def newPublisher(publisher: Publisher)(implicit session: DBSession = AutoSession): Try[Publisher] = {
      val p = Publisher.column
      val startRevision = 1

      Try {
        val id = insert.into(Publisher).namedValues(
          p.revision -> startRevision,
          p.name -> publisher.name
        ).toSQL
        .updateAndReturnGeneratedKey()
        .apply()

        publisher.copy(id = Some(id), revision = Some(startRevision))
      }
    }


    implicit val formats = org.json4s.DefaultFormats
    val (b, lic, pub) = (Book.syntax, License.syntax, Publisher.syntax)

    val (t, ea, ch, ctb, p, cat) = (
      Translation.syntax,
      EducationalAlignment.syntax,
      Chapter.syntax,
      Contributor.syntax,
      Person.syntax,
      Category.syntax)

    def personWithName(name: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Person] = {
      sql"select ${p.result.*} from ${Person.as(p)} where LOWER(${p.name}) = LOWER($name)".map(Person(p)).single.apply
    }

    def categoryWithName(category: String)(implicit session: DBSession = AutoSession): Option[Category] = {
      sql"select ${cat.result.*} from ${Category.as(cat)} where LOWER(${cat.name}) = LOWER($category)".map(Category(cat)).single.apply
    }

    def publisherWithName(publisher: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Publisher] = {
      sql"select ${pub.result.*} from ${Publisher.as(pub)} where LOWER(${pub.name}) = LOWER($publisher)".map(Publisher(pub)).single.apply
    }

    def licenseWithName(license: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[License] = {
      sql"select ${lic.result.*} from ${License.as(lic)} where LOWER(${lic.name}) = LOWER($license)".map(License(lic)).single.apply
    }

    def languagesFor(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Seq[String] = {
      select(t.result.language)
        .from(Translation as t)
        .where.eq(t.bookId, id).toSQL
        .map(_.string(1)).list().apply()
    }

    def translationWithExternalId(externalId: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Translation] = {
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
        val contributors = select
          .from(Contributor as ctb)
          .innerJoin(Person as p).on(p.id, ctb.personId)
          .where.eq(ctb.translationId, t.id).toSQL
          .map(Contributor(ctb, p)).list().apply()

        t.copy(contributors = contributors)
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

    def translationForBookIdAndLanguage(bookId: Long, language: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Translation] = {
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
        val contributors = select
          .from(Contributor as ctb)
          .innerJoin(Person as p).on(p.id, ctb.personId)
          .where.eq(ctb.translationId, t.id).toSQL
          .map(Contributor(ctb, p)).list().apply()

        t.copy(contributors = contributors)
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

    def chaptersForBookIdAndLanguage(bookId: Long, language: String)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Chapter] = {
      select
        .from(Chapter as ch)
        .rightJoin(Translation as t).on(ch.translationId, t.id)
        .where.eq(t.bookId, bookId).and.eq(t.language, language).toSQL
        .map(Chapter(ch)).list().apply()

    }

    def chapterForBookWithLanguageAndId(bookId: Long, language: String, chapterId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Chapter] = {
      select
        .from(Chapter as ch)
        .rightJoin(Translation as t).on(ch.translationId, t.id)
        .where
        .eq(t.bookId, bookId).and
        .eq(t.language, language).and
        .eq(ch.id, chapterId).toSQL
        .map(Chapter(ch)).single().apply()
    }

    def bookIdsWithLanguage(language: String, pageSize: Int, page: Int)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Long] = {
      val limit = pageSize.max(1)
      val offset = (page.max(1) - 1) * pageSize

      select(t.result.bookId)
        .from(Translation as t)
        .where.eq(t.language, language).limit(limit).offset(offset)
        .toSQL
        .map(_.long(1)).list().apply()
    }

    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Book] = {
      select
        .from(Book as b)
        .innerJoin(License as lic).on(b.licenseId, lic.id)
        .innerJoin(Publisher as pub).on(b.publisherId, pub.id)
        .where.eq(b.id, id).toSQL
        .map(Book.apply(b, pub, lic)).single().apply()
    }
  }


}
