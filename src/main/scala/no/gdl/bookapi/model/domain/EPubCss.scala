/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.model.domain


case class EPubCss(href: String = "epub.css", mimeType: String = "text/css") {
  def asBytes: Array[Byte] =
    """
      |body {
      |    margin: 0;
      |    text-align: center;
      |}
      |img {
      |    max-width: 100%;
      |    max-height: 50vh;
      |}
    """.stripMargin.getBytes
}