package io.digitallibrary.bookapi.controller

import io.digitallibrary.bookapi.model.api
import io.digitallibrary.bookapi.service.ReadServiceV2
import io.digitallibrary.language.model.LanguageTag
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait LevelControllerV2 {
  this: ReadServiceV2 =>
  val levelControllerV2: LevelControllerV2

  class LevelControllerV2 (implicit val swagger: Swagger) extends GdlController with SwaggerSupport {
    protected val applicationDescription = "API for retrieving all levels from GDL"

    registerModel[api.Error]
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val getAllLevels = (apiOperation[Seq[String]]("getAllLevels")
      summary s"Returns all levels with content in GDL"
      tags "Misc v2"
      notes s"Returns all levels with content in GDL"
      parameters headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted.")
      authorizations "oauth2"
      responseMessages response500)

    private val getAllLevelsForLanguage = (apiOperation[Seq[String]]("getAllLevelsForLanguage")
      summary s"Returns all levels for specified language with content in GDL"
      tags "Misc v2"
      notes s"Returns all levels for specified language with content in GDL"
      parameters(
      headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
      pathParam[String]("lang").description("Desired language for books specified in ISO 639-1/2 format."))
      authorizations "oauth2"
      responseMessages response500)

    get("/", operation(getAllLevels)) {
      readServiceV2.listAvailablePublishedLevelsForLanguage()
    }

    get("/:lang", operation(getAllLevelsForLanguage)) {
      readServiceV2.listAvailablePublishedLevelsForLanguage(Some(LanguageTag(params("lang"))))
    }
  }
}

