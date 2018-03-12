/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package io.digitallibrary.bookapi

import javax.sql.DataSource

import org.flywaydb.core.Flyway

object DBMigrator {
  def migrate(datasource: DataSource) = {
    val flyway = new Flyway()
    flyway.setDataSource(datasource)
    flyway.migrate()
  }
}
