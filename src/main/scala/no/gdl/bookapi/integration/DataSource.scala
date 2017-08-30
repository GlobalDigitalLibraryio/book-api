/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.integration

trait DataSource {
  val dataSource: javax.sql.DataSource
}
