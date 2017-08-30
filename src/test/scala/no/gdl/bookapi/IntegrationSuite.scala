/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi

import javax.sql.DataSource

import io.digitallibrary.network.secrets.PropertyKeys
import org.postgresql.ds.PGPoolingDataSource

abstract class IntegrationSuite extends UnitSuite {

  setEnv(PropertyKeys.MetaUserNameKey, "postgres")
  setEnvIfAbsent(PropertyKeys.MetaPasswordKey, "hemmelig")
  setEnv(PropertyKeys.MetaResourceKey, "postgres")
  setEnv(PropertyKeys.MetaServerKey, "127.0.0.1")
  setEnv(PropertyKeys.MetaPortKey, "5432")
  setEnv(PropertyKeys.MetaSchemaKey, "listingapitest")


  def getDataSource: DataSource = {
    val datasource = new PGPoolingDataSource()
    datasource.setUser(BookApiProperties.MetaUserName)
    datasource.setPassword(BookApiProperties.MetaPassword)
    datasource.setDatabaseName(BookApiProperties.MetaResource)
    datasource.setServerName(BookApiProperties.MetaServer)
    datasource.setPortNumber(BookApiProperties.MetaPort)
    datasource.setInitialConnections(BookApiProperties.MetaInitialConnections)
    datasource.setMaxConnections(BookApiProperties.MetaMaxConnections)
    datasource.setCurrentSchema(BookApiProperties.MetaSchema)
    datasource
  }
}
