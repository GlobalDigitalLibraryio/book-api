/*
 * Part of GDL book_api.
 * Copyright (C) 2018 Global Digital Library
 *
 * See LICENSE
 */

package io.digitallibrary.bookapi.model.crowdin

import io.digitallibrary.bookapi.model.domain.FileType

case class SupportedLanguage (name: String, crowdinCode: String, editorCode: String, iso6391: String, iso6393: String, locale: String, androidCode: String, osxCode: String, osxLocale: String)
case class CrowdinProject (sourceLanguage: String, projectIdentifier: String, projectKey: String)

case class ProjectDetails(languages: Seq[TargetLanguage], files: Seq[File], details: Details)
case class Details (name: String)
case class File (id: String)
case class TargetLanguage(name: String, code: String, canTranslate: Long, canApprove: Long) {
  def isSupported: Boolean = canTranslate != 0 && canApprove != 0
}
case class EditProjectResponse(project: Option[Project], error: Option[Error])
case class Project(success: Boolean)
case class AddDirectoryResponse(success: Boolean)
case class AddFilesResponse(success: Boolean, stats: Option[Stats], error: Option[Error])
case class Stats(files: Seq[AddedFile])
case class AddedFile(fileId: Long, name: String, strings: Long, words: Long)
case class Error(code: Long, message: String)

case class DeleteDirectoryResponse(success: Boolean, error: Option[Error]) {
  def dirNotFoundError: Boolean = {
    error match {
      case Some(Error(17, _)) => true
      case _ => false
    }
  }
}

case class CrowdinFile (sourceId: Option[Long], seqNo: Int, fileType: FileType.Value, addedFile: AddedFile)
