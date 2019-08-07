package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.BookApiProperties
import io.digitallibrary.bookapi.BookApiProperties.RoleWithAdminReadAccess
import io.digitallibrary.bookapi.service.ReadServiceV2
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait SourceControllerV2 {
  this: ReadServiceV2 =>
  val sourceControllerV2: SourceControllerV2

  class SourceControllerV2(implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for getting sources from GDL."

    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getSourcesForLanguage = (apiOperation[Seq[String]]("getSourcesForLanguage")
      summary s"Returns all the sources for the specified language"
      description s"Returns a list of sources in the specified language"
      tags "Misc v2"
      parameter pathParam[String]("lang").description("The language to receive sources for in ISO 639-2 format")
      authorizations "oauth2"
      responseMessages(response400, response403, response404, response500))

    get("/:lang", operation(getSourcesForLanguage)) {
      assertHasRole(RoleWithAdminReadAccess)

      val language = LanguageTag(params("lang"))
      readServiceV2.listSourcesForLanguage(language)
    }

    get("/") {
      redirect(s"${BookApiProperties.SourcePath}/${BookApiProperties.DefaultLanguage}")
    }
  }
}