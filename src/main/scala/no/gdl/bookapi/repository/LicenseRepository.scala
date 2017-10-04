/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.License
import scalikejdbc._


trait LicenseRepository {
  val licenseRepository: LicenseRepository

  class LicenseRepository {
    private val lic = License.syntax

    def add(license: License)(implicit session: DBSession = AutoSession): License = {
      val l = License.column
      val startRevision = 1

      val id = insert.into(License).namedValues(
        l.revision -> startRevision,
        l.name -> license.name,
        l.description -> license.description,
        l.url -> license.url
      ).toSQL.updateAndReturnGeneratedKey().apply()

      license.copy(id = Some(id), revision = Some(startRevision))
    }

    def withName(license: String)(implicit session: DBSession = ReadOnlyAutoSession): Option[License] = {
      sql"select ${lic.result.*} from ${License.as(lic)} where LOWER(${lic.name}) = LOWER($license) order by ${lic.id}".map(License(lic)).list.apply.headOption
    }
  }
}
