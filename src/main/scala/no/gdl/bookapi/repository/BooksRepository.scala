/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.integration.DataSource
import no.gdl.bookapi.model.domain._
import org.postgresql.util.PGobject
import scalikejdbc._
import org.json4s.native.Serialization.write

trait BooksRepository {
  this: DataSource =>
  val booksRepository: BooksRepository

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null):A = {
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


    implicit val formats = org.json4s.DefaultFormats

    def withTitle(title: String): Option[Book] = {
      bookWhere(sqls"b.document->>'title' = $title")
    }

    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Book] = {
      bookWhere(sqls"b.id = $id")
    }

    def withExternalId(externalId: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[Book] = {
      bookWhere(sqls"b.external_id = $externalId")
    }

    def bookInLanguageWithExternalId(externalId: String): Option[BookInLanguage] = {
      bookInLanguageWhere(sqls"bIL.external_id = $externalId")
    }

    def all()(implicit session: DBSession = ReadOnlyAutoSession): Seq[Book] = {
      val (b, bIL) = (Book.syntax("b"), BookInLanguage.syntax("bIL"))
      sql"select ${b.result.*}, ${bIL.result.*} from ${Book.as(b)} left join ${BookInLanguage.as(bIL)} on ${b.id} = ${bIL.bookId}"
        .one(Book(b.resultName))
        .toMany(BookInLanguage.opt(bIL.resultName))
        .map { (book, bookInLanguage) => book.copy(bookInLanguage = bookInLanguage) }
        .list.apply()
    }

    def updateBook(toUpdate: Book)(implicit session: DBSession = AutoSession): Book = {
      if(toUpdate.id.isEmpty) {
        throw new RuntimeException("A non-persisted book cannot be updated without being saved first")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(toUpdate))

      val newRevision = toUpdate.revision.getOrElse(0) + 1
      val count = sql"update ${Book.table} set document=${dataObject}, revision=$newRevision where id=${toUpdate.id} and revision=${toUpdate.revision}".update.apply

      if(count != 1) {
        val message = s"Found revision mismatch when attempting to update book ${toUpdate.revision}"
        logger.info(message)
        throw new RuntimeException(message) //TODO: Replace with Failure
      }

      val updatedRmInLanguage = toUpdate.bookInLanguage.map(updateBookInLanguage)
      toUpdate.copy(revision = Some(newRevision), bookInLanguage = updatedRmInLanguage)

    }

    def updateBookInLanguage(toUpdate: BookInLanguage)(implicit session: DBSession = AutoSession): BookInLanguage = {
      if(toUpdate.id.isEmpty) {
        throw new RuntimeException("A non-persisted book-in-language cannot be updated without being saved first")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(toUpdate))

      val newRevision = toUpdate.revision.getOrElse(0) + 1
      val count = sql"update ${BookInLanguage.table} set document=${dataObject}, revision=$newRevision where id=${toUpdate.id} and revision=${toUpdate.revision}".update.apply

      if(count != 1) {
        val message = s"Found revision mismatch when attempting to update book ${toUpdate.revision}"
        logger.info(message)
        throw new RuntimeException(message) //TODO: Replace with Failure
      }

      toUpdate.copy(revision = Some(newRevision))

    }

    def insertBook(book: Book)(implicit session: DBSession = AutoSession): Book = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(book))

      val startRevision = 1

      val bookId = sql"insert into ${Book.table} (document, revision, external_id) values (${dataObject}, $startRevision, ${book.externalId})".updateAndReturnGeneratedKey.apply
      val inLanguages = book.bookInLanguage.map(b => {
        insertBookInLanguage(b.copy(bookId = Some(bookId)))
      })
      book.copy(id = Some(bookId), revision = Some(startRevision), bookInLanguage = inLanguages)
    }

    def insertBookInLanguage(bookInLanguage: BookInLanguage)(implicit session: DBSession = AutoSession): BookInLanguage = {
      val startRevision = 1
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(bookInLanguage))

      val id = sql"insert into ${BookInLanguage.table} (book_id, document, revision, external_id) values (${bookInLanguage.bookId}, ${dataObject}, $startRevision, ${bookInLanguage.externalId})".updateAndReturnGeneratedKey.apply
      bookInLanguage.copy(id = Some(id), revision = Some(startRevision))
    }

    private def bookWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Book] = {
      val (b, bIL) = (Book.syntax("b"), BookInLanguage.syntax("bIL"))
      val resultat = sql"select ${b.result.*}, ${bIL.result.*} from ${Book.as(b)} left join ${BookInLanguage.as(bIL)} on ${b.id} = ${bIL.bookId} where $whereClause"
        .one(Book(b.resultName))
        .toMany(BookInLanguage.opt(bIL.resultName))
        .map { (book, bookInLanguage) => book.copy(bookInLanguage = bookInLanguage) }
        .single.apply()

      resultat

    }

    private def bookInLanguageWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[BookInLanguage] = {
      val bIL = BookInLanguage.syntax("bIL")
      sql"select ${bIL.result.*} from ${BookInLanguage.as(bIL)} where $whereClause".map(BookInLanguage(bIL)).single.apply
    }
  }


}
