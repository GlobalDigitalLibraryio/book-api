/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.controller

import java.text.SimpleDateFormat
import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

import no.gdl.bookapi.model.api.LocalDateSerializer
import no.gdl.bookapi.{TestEnvironment, UnitSuite}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.test.scalatest.ScalatraFunSuite

class OPDSControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats: Formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
  } + LocalDateSerializer


  lazy val controller = new DownloadController
  addServlet(controller, "/*")

  test("something") {

    val dtf: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    val dato = LocalDate.now().atStartOfDay(ZoneId.systemDefault())

    val datoSomString = dtf.format(dato)
    print(s"DATO = $datoSomString")
  }

}
