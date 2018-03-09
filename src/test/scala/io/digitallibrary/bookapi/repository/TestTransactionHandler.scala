/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.repository

import io.digitallibrary.bookapi.integration.DataSource
import org.scalatest.mockito.MockitoSugar
import scalikejdbc.DBSession

trait TestTransactionHandler extends TransactionHandler with MockitoSugar {
  this: DataSource =>

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null): A = {
    Option(session) match {
      case Some(x) => work(x)
      case None => work(mock[DBSession])
    }
  }
}
