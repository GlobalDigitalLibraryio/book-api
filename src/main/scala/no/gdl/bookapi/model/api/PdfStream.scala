package no.gdl.bookapi.model.api

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
