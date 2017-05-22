/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.gdl.readingmaterialsapi.integration.DataSource
import no.gdl.readingmaterialsapi.model.domain._
import org.postgresql.util.PGobject
import scalikejdbc._
import org.json4s.native.Serialization.write

trait ReadingMaterialsRepository {
  this: DataSource =>
  val readingMaterialsRepository: ReadingMaterialsRepository

  def inTransaction[A](work: DBSession => A)(implicit session: DBSession = null):A = {
    Option(session) match {
      case Some(x) => work(x)
      case None => {
        DB localTx { implicit newSession =>
          work(newSession)
        }
      }
    }
  }

  class ReadingMaterialsRepository extends LazyLogging {


    implicit val formats = org.json4s.DefaultFormats

    def withTitle(title: String): Option[ReadingMaterial] = {
      readingMaterialWhere(sqls"rm.document->>'title' = $title")
    }

    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[ReadingMaterial] = {
      readingMaterialWhere(sqls"rm.id = $id")
    }

    def readingMaterialInLanguageFor(id: Long, language: String): Option[ReadingMaterialInLanguage] = {
      withId(id).flatMap(_.readingMaterialInLanguage.find(_.language == language))
    }

    def all()(implicit session: DBSession = ReadOnlyAutoSession): Seq[ReadingMaterial] = {
      val (rm, rmIL) = (ReadingMaterial.syntax("rm"), ReadingMaterialInLanguage.syntax("rmIL"))
      sql"select ${rm.result.*}, ${rmIL.result.*} from ${ReadingMaterial.as(rm)} left join ${ReadingMaterialInLanguage.as(rmIL)} on ${rm.id} = ${rmIL.readingMaterialId}"
        .one(ReadingMaterial(rm.resultName))
        .toMany(ReadingMaterialInLanguage.opt(rmIL.resultName))
        .map { (readingMaterial, readingMaterialInLanguage) => readingMaterial.copy(readingMaterialInLanguage = readingMaterialInLanguage) }
        .list.apply()
    }

    def updateReadingMaterial(toUpdate: ReadingMaterial)(implicit session: DBSession = AutoSession): ReadingMaterial = {
      if(toUpdate.id.isEmpty) {
        throw new RuntimeException("A non-persisted reading-material cannot be updated without being saved first")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(toUpdate))

      val newRevision = toUpdate.revision.getOrElse(0) + 1
      val count = sql"update ${ReadingMaterial.table} set document=${dataObject}, revision=$newRevision where id=${toUpdate.id} and revision=${toUpdate.revision}".update.apply

      if(count != 1) {
        val message = s"Found revision mismatch when attempting to update reading-material ${toUpdate.revision}"
        logger.info(message)
        throw new RuntimeException(message) //TODO: Replace with Failure
      }

      val updatedRmInLanguage = toUpdate.readingMaterialInLanguage.map(updateReadingMaterialInLanguage)
      toUpdate.copy(revision = Some(newRevision), readingMaterialInLanguage = updatedRmInLanguage)

    }

    def updateReadingMaterialInLanguage(toUpdate: ReadingMaterialInLanguage)(implicit session: DBSession = AutoSession): ReadingMaterialInLanguage = {
      if(toUpdate.id.isEmpty) {
        throw new RuntimeException("A non-persisted reading-material-in-language cannot be updated without being saved first")
      }

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(toUpdate))

      val newRevision = toUpdate.revision.getOrElse(0) + 1
      val count = sql"update ${ReadingMaterialInLanguage.table} set document=${dataObject}, revision=$newRevision where id=${toUpdate.id} and revision=${toUpdate.revision}".update.apply

      if(count != 1) {
        val message = s"Found revision mismatch when attempting to update reading-material ${toUpdate.revision}"
        logger.info(message)
        throw new RuntimeException(message) //TODO: Replace with Failure
      }

      toUpdate.copy(revision = Some(newRevision))

    }

    def insertReadingMaterial(readingMaterial: ReadingMaterial) (implicit session: DBSession = AutoSession): ReadingMaterial = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(readingMaterial))

      val startRevision = 1

      val readingMaterialId = sql"insert into ${ReadingMaterial.table} (document, revision) values (${dataObject}, $startRevision)".updateAndReturnGeneratedKey.apply
      val inLanguages = readingMaterial.readingMaterialInLanguage.map(rm => {
        insertReadingMaterialInLanguage(rm.copy(readingMaterialId = Some(readingMaterialId)))
      })
      readingMaterial.copy(id = Some(readingMaterialId), revision = Some(startRevision), readingMaterialInLanguage = inLanguages)
    }

    def insertReadingMaterialInLanguage(readingMaterialInLanguage: ReadingMaterialInLanguage)(implicit session: DBSession = AutoSession): ReadingMaterialInLanguage = {
      val startRevision = 1
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(readingMaterialInLanguage))

      val id = sql"insert into ${ReadingMaterialInLanguage.table} (reading_material_id, document, revision) values (${readingMaterialInLanguage.readingMaterialId}, ${dataObject}, $startRevision)".updateAndReturnGeneratedKey.apply
      readingMaterialInLanguage.copy(id = Some(id), revision = Some(startRevision))
    }

    private def readingMaterialWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[ReadingMaterial] = {
      val (rm, rmIL) = (ReadingMaterial.syntax("rm"), ReadingMaterialInLanguage.syntax("rmIL"))
      sql"select ${rm.result.*}, ${rmIL.result.*} from ${ReadingMaterial.as(rm)} left join ${ReadingMaterialInLanguage.as(rmIL)} on ${rm.id} = ${rmIL.readingMaterialId} where $whereClause"
        .one(ReadingMaterial(rm.resultName))
        .toMany(ReadingMaterialInLanguage.opt(rmIL.resultName))
        .map { (readingMaterial, readingMaterialInLanguage) => readingMaterial.copy(readingMaterialInLanguage = readingMaterialInLanguage) }
        .single.apply()

    }
  }


}
