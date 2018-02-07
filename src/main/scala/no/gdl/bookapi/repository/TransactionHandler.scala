/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.integration.DataSource
import scalikejdbc._

trait TransactionHandler {
  this: DataSource =>

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null): A
}

trait LiveTransactionHandler extends TransactionHandler {
  this: DataSource =>

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null): A = {
    Option(session) match {
      case Some(x) => work(x)
      case None => DB localTx { implicit newSession =>
        work(newSession)
      }
    }
  }
}
