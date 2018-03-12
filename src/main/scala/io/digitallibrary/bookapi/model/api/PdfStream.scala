/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.api

import java.io.{InputStream, OutputStream}

import org.apache.commons.io.IOUtils

trait PdfStream {
  def stream: InputStream
  def fileName: String
  def toOutputStream(outputStream: OutputStream): Unit = {
    IOUtils.copy(stream, outputStream)
    stream.close()
    outputStream.flush()
    outputStream.close()
  }
}
