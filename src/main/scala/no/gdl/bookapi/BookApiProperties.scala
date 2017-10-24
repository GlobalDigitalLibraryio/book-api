/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi

import com.typesafe.scalalogging.LazyLogging
import io.digitallibrary.network.Domains
import io.digitallibrary.network.secrets.PropertyKeys
import io.digitallibrary.network.secrets.Secrets.readSecrets

import scala.util.Properties._
import scala.util.{Failure, Success}

object BookApiProperties extends LazyLogging {
  val RoleWithWriteAccess = "books:write"
  val SecretsFile = "book-api.secrets"

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "christergundersen@ndla.no"
  val Environment = propOrElse("GDL_ENVIRONMENT", "local")
  lazy val Domain: String = Domains.get(Environment)

  val SearchServer = propOrElse("SEARCH_SERVER", "http://search-book-api.gdl-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")
  val RunWithSignedSearchRequests = propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "books")
  val SearchDocument = "book"
  val DefaultPageSize = 10
  val MaxPageSize = 100
  val IndexBulkSize = 200

  val DefaultLanguage = "eng"

  val OpdsPath = "/book-api/opds"
  val DownloadPath = "/book-api/download"
  val CloudFrontUrl = getCloudFrontUrl(Environment)
  val LanguagesPath = "/book-api/v1/languages"
  val LevelsPath = "/book-api/v1/levels"
  val EditorPicksPath = "/book-api/v1/editorpicks"
  val ApiPath = "/book-api/v1/books"
  val ApiDocPath = "/book-api/api-docs"
  val ImagePath = "/image-api/v1/raw"

  val ImageApiHost = "image-api.gdl-local"
  val InternalImageApiUrl = s"$ImageApiHost/image-api/v1/images"

  val CoverPhotoTumbSize = 200

  lazy val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = prop(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = prop(PropertyKeys.MetaServerKey)
  lazy val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  lazy val MetaSchema = prop(PropertyKeys.MetaSchemaKey)
  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  lazy val secrets = readSecrets(SecretsFile) match {
     case Success(values) => values
     case Failure(exception) => throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
   }

  def booleanProp(key: String) = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    secrets.get(key).flatten match {
      case Some(secret) => secret
      case None =>
        envOrNone(key) match {
          case Some(env) => env
          case None => default
        }
    }
  }

  def getCloudFrontUrl(env: String): String = {
    // TODO Replace explicit CloudFront URLs with sub-domains created in Route 53:
    // Prod:        https://books.api.digitallibrary.io
    // Other envs:  https://books.<env>.api.digitallibrary.io
    if (env.equals("prod")) {
      "TODO"
    } else if (env.equals("staging")) {
      "TODO"
    } else if (env.equals("test")) {
      "https://dzoxgfkzkvnkl.cloudfront.net"
    } else if (env.equals("local")) {
      Domain + DownloadPath
    } else {
      throw new IllegalArgumentException(s"$env is not a valid env")
    }
  }
}
