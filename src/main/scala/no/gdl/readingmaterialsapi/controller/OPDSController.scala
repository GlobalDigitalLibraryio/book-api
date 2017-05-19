/*
 * Part of GDL reading_materials_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.readingmaterialsapi.controller

import java.text.SimpleDateFormat
import java.util.Date

import no.gdl.readingmaterialsapi.ReadingMaterialsApiProperties.DefaultLanguage
import no.gdl.readingmaterialsapi.service.ReadService

trait OPDSController {
  this: ReadService =>
  val opdsController: OPDSController
  val sdf: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

  class OPDSController extends GdlController {

    before() {
      contentType = formats("xml")
    }

    get("/catalog.atom") {
        <feed xmlns="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/terms/" xmlns:opds="http://opds-spec.org/2010/catalog">
          <id>http://api.digitallibrary.io/reading-materials-api/opds/catalog.atom</id>
          <title>Global Digital Library - Open Access</title>
          <updated>2017-04-28T12:54:15Z</updated>
          <link href="http://api.digitallibrary.io/reading-materials-api/opds/catalog.atom" rel="self"/>
          {readService.all(paramOrDefault("language", DefaultLanguage)).map(x =>
            <entry>
              <id>urn:gdl:{x.id}</id>
              <title>{x.title}</title>
              {x.authors.map(auth => <author><name>{auth}</name></author>)}
              <updated>{sdf.format(new Date())}</updated>
              <summary>{x.description}</summary>
              <link href={x.coverPhoto.large} type="image/jpeg" rel="http://opds-spec.org/image"/>
              <link href={x.coverPhoto.small} type="image/png" rel="http://opds-spec.org/image/thumbnail"/>
              <link href={x.downloads.epub} type="application/epub+zip" rel="http://opds-spec.org/acquisition/open-access"/>
            </entry>
          )}
        </feed>
    }
  }
}
