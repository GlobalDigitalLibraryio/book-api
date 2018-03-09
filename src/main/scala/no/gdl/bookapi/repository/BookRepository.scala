/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.api.OptimisticLockException
import no.gdl.bookapi.model.domain.{Book, License, Publisher}
import scalikejdbc.{AutoSession, DBSession, ReadOnlyAutoSession, insert, select, update}

import scala.util.Try


trait BookRepository {
  val bookRepository: BookRepository

  class BookRepository {
    private val (b, lic, pub) = (Book.syntax, License.syntax, Publisher.syntax)

    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Book] = {
      select
        .from(Book as b)
        .innerJoin(License as lic).on(b.licenseId, lic.id)
        .innerJoin(Publisher as pub).on(b.publisherId, pub.id)
        .where.eq(b.id, id).toSQL
        .map(Book.apply(b, pub, lic)).single().apply()
    }

    def add(newBook: Book)(implicit session: DBSession = AutoSession): Book = {
      val b = Book.column
      val startRevision = 1


        val id = insert.into(Book).namedValues(
          b.revision -> startRevision,
          b.publisherId -> newBook.publisherId,
          b.licenseId -> newBook.licenseId,
          b.source -> newBook.source
        ).toSQL
          .updateAndReturnGeneratedKey()
          .apply()

        newBook.copy(id = Some(id), revision = Some(startRevision))
    }

    def updateBook(book: Book)(implicit session: DBSession = AutoSession): Try[Book] = {
      val b = Book.column
      val newRevision = book.revision.getOrElse(0) + 1

      Try {
        val count = update(Book).set(
          b.revision -> newRevision,
          b.publisherId -> book.publisherId,
          b.licenseId -> book.licenseId,
          b.source -> book.source
        ).where
          .eq(b.id, book.id).and
          .eq(b.revision, book.revision).toSQL.update().apply()

        if (count != 1) {
          throw new OptimisticLockException()
        } else {
          book.copy(revision = Some(newRevision))
        }
      }
    }
  }
}
