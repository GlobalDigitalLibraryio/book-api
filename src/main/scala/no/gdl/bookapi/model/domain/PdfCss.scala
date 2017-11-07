/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain

case class PdfCss (fontNames: Seq[String]) {
  def asString: String = {
    """
      |div.page {
      | page-break-after: always;
      |}
      |
      |body {
      |    margin: 0;
      |    text-align: center;
      |    font-family: {FONT-FAMILY};
      |}
    """.stripMargin.replace("{FONT-FAMILY}", s"'${fontNames.mkString("','")}', sans-serif")
  }
}


