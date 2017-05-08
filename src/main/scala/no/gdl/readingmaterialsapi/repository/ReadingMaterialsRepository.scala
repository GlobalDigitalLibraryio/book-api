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

  class ReadingMaterialsRepository extends LazyLogging {


    implicit val formats = org.json4s.DefaultFormats


    def withId(id: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[ReadingMaterial] = {
      readingMaterialWhere(sqls"rm.id = $id")
    }

    def all()(implicit session: DBSession = ReadOnlyAutoSession): Seq[ReadingMaterial] = {
      val (rm, rmIL) = (ReadingMaterial.syntax("rm"), ReadingMaterialInLanguage.syntax("rmIL"))
      sql"select ${rm.result.*}, ${rmIL.result.*} from ${ReadingMaterial.as(rm)} left join ${ReadingMaterialInLanguage.as(rmIL)} on ${rm.id} = ${rmIL.readingMaterialId}"
        .one(ReadingMaterial(rm.resultName))
        .toMany(ReadingMaterialInLanguage.opt(rmIL.resultName))
        .map { (readingMaterial, readingMaterialInLanguage) => readingMaterial.copy(readingMaterialInLanguage = readingMaterialInLanguage) }
        .list.apply()
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
