/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

import no.gdl.bookapi.BookApiProperties
import no.gdl.bookapi.model.api.OptimisticLockException
import scalikejdbc._

import scala.util.Try


case class Book(id: Option[Long],
                revision: Option[Int],
                publisherId: Long,
                licenseId: Long,
                publisher: Publisher,
                license: License)

object Book extends SQLSyntaxSupport[Book] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "book"
  override val schemaName = Some(BookApiProperties.MetaSchema)
  private val (b, lic, pub) = (Book.syntax, License.syntax, Publisher.syntax)

  def apply(b: SyntaxProvider[Book], pub: SyntaxProvider[Publisher], lic: SyntaxProvider[License])(rs: WrappedResultSet): Book =
    apply(b.resultName, pub.resultName, lic.resultName)(rs)


  def apply(b: ResultName[Book], pub: ResultName[Publisher], lic: ResultName[License])(rs: WrappedResultSet): Book = Book(
      rs.longOpt(b.id),
      rs.intOpt(b.revision),
      rs.long(b.publisherId),
      rs.long(b.licenseId),
      Publisher.apply(pub)(rs),
      License.apply(lic)(rs))

  def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Book] = {
    select
      .from(Book as b)
      .innerJoin(License as lic).on(b.licenseId, lic.id)
      .innerJoin(Publisher as pub).on(b.publisherId, pub.id)
      .where.eq(b.id, id).toSQL
      .map(Book.apply(b, pub, lic)).single().apply()
  }

  def add(newBook: Book)(implicit session: DBSession = AutoSession): Try[Book] = {
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

  def updateBook(book: Book)(implicit session: DBSession = AutoSession): Try[Book] = {
    val b = Book.column
    val newRevision = book.revision.getOrElse(0) + 1

    Try{
      val count = update(Book).set(
        b.revision -> newRevision,
        b.publisherId -> book.publisherId,
        b.licenseId -> book.licenseId
      ).where
        .eq(b.id, book.id).and
        .eq(b.revision, book.revision).toSQL.update().apply()

      if(count != 1) {
        throw new OptimisticLockException()
      } else {
        book.copy(revision = Some(newRevision))
      }
    }
  }

}
