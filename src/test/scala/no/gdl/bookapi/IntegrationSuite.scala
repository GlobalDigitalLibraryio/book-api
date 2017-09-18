/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import io.digitallibrary.network.secrets.PropertyKeys
import org.postgresql.ds.PGPoolingDataSource
import scalikejdbc._

abstract class IntegrationSuite extends UnitSuite {

  setEnv(PropertyKeys.MetaUserNameKey, "postgres")
  setEnvIfAbsent(PropertyKeys.MetaPasswordKey, "hemmelig")
  setEnv(PropertyKeys.MetaResourceKey, "postgres")
  setEnv(PropertyKeys.MetaServerKey, "127.0.0.1")
  setEnv(PropertyKeys.MetaPortKey, "5432")
  setEnv(PropertyKeys.MetaSchemaKey, "bookapitest")

  override def beforeAll(): Unit = {
    TestDBMigrator.migrate()
  }

  def withRollback[A](work: DBSession => A): A = {
    using(DB(conn = ConnectionPool.borrow())) { db =>
      try {
        db.begin()
        db.withinTx { implicit session =>
          work(session)
        }
      } finally {
        db.rollbackIfActive()
      }
    }
  }
}

object TestDBMigrator {
  def migrate(): Unit = this.synchronized {
    val datasource = new PGPoolingDataSource()
    datasource.setUser(BookApiProperties.MetaUserName)
    datasource.setPassword(BookApiProperties.MetaPassword)
    datasource.setDatabaseName(BookApiProperties.MetaResource)
    datasource.setServerName(BookApiProperties.MetaServer)
    datasource.setPortNumber(BookApiProperties.MetaPort)
    datasource.setInitialConnections(BookApiProperties.MetaInitialConnections)
    datasource.setMaxConnections(BookApiProperties.MetaMaxConnections)
    datasource.setCurrentSchema(BookApiProperties.MetaSchema)

    DBMigrator.migrate(datasource)
    ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
  }
}