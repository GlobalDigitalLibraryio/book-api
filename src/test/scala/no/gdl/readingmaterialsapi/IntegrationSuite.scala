/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi

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
    datasource.setUser(ReadingMaterialsApiProperties.MetaUserName)
    datasource.setPassword(ReadingMaterialsApiProperties.MetaPassword)
    datasource.setDatabaseName(ReadingMaterialsApiProperties.MetaResource)
    datasource.setServerName(ReadingMaterialsApiProperties.MetaServer)
    datasource.setPortNumber(ReadingMaterialsApiProperties.MetaPort)
    datasource.setInitialConnections(ReadingMaterialsApiProperties.MetaInitialConnections)
    datasource.setMaxConnections(ReadingMaterialsApiProperties.MetaMaxConnections)
    datasource.setCurrentSchema(ReadingMaterialsApiProperties.MetaSchema)
    datasource
  }
}
