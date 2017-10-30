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
import no.gdl.bookapi.model.domain.FeedDefinition

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
  val DefaultReadingLevel = "1"

  val OpdsPath = "/book-api/opds"
  val OpdsLanguageParam = ":lang"
  val OpdsLevelParam = ":lev"

  val OpdsNavUrl = FeedDefinition(s"/$OpdsLanguageParam/nav.xml", "opds_nav_title", None)
  val OpdsRootUrl = FeedDefinition(s"/$OpdsLanguageParam/root.xml", "opds_root_title", None)
  val OpdsNewUrl = FeedDefinition(s"/$OpdsLanguageParam/new.xml", "new_entries_feed_title", Some("new_entries_feed_description"))
  val OpdsFeaturedUrl = FeedDefinition(s"/$OpdsLanguageParam/featured.xml", "featured_feed_title", Some("featured_feed_description"))
  val OpdsLevelUrl = FeedDefinition(s"/$OpdsLanguageParam/level$OpdsLevelParam.xml", "level_feed_title", Some("level_feed_description"))

  val OpdsFeeds = Seq(OpdsNavUrl, OpdsRootUrl, OpdsNewUrl, OpdsFeaturedUrl, OpdsLevelUrl)
  val OpdsJustArrivedLimit = 15

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
    env match {
      case "prod" => "TODO"
      case "staging" => "TODO"
      case "test" => "https://dzoxgfkzkvnkl.cloudfront.net"
      case "local" => Domain + DownloadPath
      case _ => throw new IllegalArgumentException(s"$env is not a valid env")
    }
  }
}
