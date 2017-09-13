/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */


package no.gdl.bookapi.controller

import java.text.SimpleDateFormat
import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.gdl.bookapi.BookApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.gdl.bookapi.model.api.{AccessDeniedException, Error, NotFoundException, OptimisticLockException, ValidationError, ValidationException, ValidationMessage}
import io.digitallibrary.network.{ApplicationUrl, AuthUser, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.NativeJsonSupport

import scala.util.{Failure, Success, Try}

abstract class GdlController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  }


  before() {
    contentType = formats("json")
    CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
    ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
    ApplicationUrl.set(request)
    AuthUser.set(request)
    logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
  }

  after() {
    CorrelationID.clear()
    ThreadContext.remove(CorrelationIdKey)
    AuthUser.clear()
    ApplicationUrl.clear
  }

  error {
    case a: AccessDeniedException => Forbidden(body = Error(Error.ACCESS_DENIED, a.getMessage))
    case v: ValidationException => BadRequest(body=ValidationError(messages=v.errors))
    case n: NotFoundException => NotFound(body=Error(Error.NOT_FOUND, n.getMessage))
    case e: IndexNotFoundException => InternalServerError(body=Error.IndexMissingError)
    case o: OptimisticLockException => Conflict(body=Error(Error.RESOURCE_OUTDATED, o.getMessage))
    case t: Throwable => {
      logger.error(Error.GenericError.toString, t)
      InternalServerError(body=Error.GenericError)
    }
  }

  private val customRenderer: RenderPipeline = {
    case Failure(e) => errorHandler(e)
    case Success(s) => s
  }

  override def renderPipeline = customRenderer orElse super.renderPipeline

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String =
    params.get(paramName).map(_.trim).filterNot(_.isEmpty()).getOrElse(default)

  def longOrDefault(paramName: String, default: Long): Long =
    paramOrDefault(paramName, default.toString).toLong

  def intOrDefault(paramName: String, default: Int): Int = {
    val value = paramOrDefault(paramName, default.toString)
    if (value.forall(_.isDigit)) {
      value.toInt
    } else {
      throw new ValidationException(errors = Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }


  def paramAsListOfString(paramName: String)(implicit request: HttpServletRequest): List[String] = {
    params.get(paramName) match {
      case None => List.empty
      case Some(param) => param.split(",").toList.map(_.trim)
    }
  }

  def paramAsListOfLong(paramName: String)(implicit request: HttpServletRequest): List[Long] = {
    val strings = paramAsListOfString(paramName)
    strings.headOption match {
      case None => List.empty
      case Some(_) =>
        if (!strings.forall(entry => entry.forall(_.isDigit))) {
          throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")))
        }
        strings.map(_.toLong)
    }
  }

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    paramValue.forall(_.isDigit) match {
      case true => paramValue.toLong
      case false => throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }

  def extract[T](json: String)(implicit mf: scala.reflect.Manifest[T]): T = {
    Try(read[T](json)) match {
      case Failure(e) => {
        logger.error(e.getMessage, e)
        throw new ValidationException(errors=Seq(ValidationMessage("body", e.getMessage)))
      }
      case Success(data) => data
    }
  }

}

