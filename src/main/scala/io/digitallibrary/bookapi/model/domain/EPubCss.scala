/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.domain


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
      |    display: block;
      |    margin-left: auto;
      |    margin-right: auto;
      |    margin-bottom: 10px;
      |}
    """.stripMargin.getBytes
}