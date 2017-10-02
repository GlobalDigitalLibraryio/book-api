/*
 * Part of GDL book_api.
 * Copyright (C) 2017 Global Digital Library
 *
 * See LICENSE
 */

package no.gdl.bookapi.repository

import no.gdl.bookapi.model.domain.License
import no.gdl.bookapi.{IntegrationSuite, TestEnvironment}


class LicenseRepositoryTest extends IntegrationSuite with TestEnvironment {

  override val licenseRepository = new LicenseRepository

  test("that License is added") {
    withRollback { implicit session =>
      val testName = "some-name"
      val testDesc = Some("some-description")
      val testUrl = Some("some-url")

      val licenseDef = License(None, None, testName, testDesc, testUrl)

      licenseRepository.add(licenseDef)

      val withName = licenseRepository.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.revision.isDefined should be(true)
      withName.head.name should equal(testName)
      withName.head.description should equal(testDesc)
      withName.head.url should equal(testUrl)
    }
  }

  test("that withName returns None when no License with given name") {
    val withName = licenseRepository.withName(s"some-name${System.currentTimeMillis()}")
    withName should be (None)
  }

  test("That withName returns the first occurence of License with that name") {
    withRollback { implicit session =>
      val testName = s"Some-license-${System.currentTimeMillis()}"
      val licenseDef = License(None, None, testName, None, None)

      val license1 = licenseRepository.add(licenseDef)
      val license2 = licenseRepository.add(licenseDef)
      val license3 = licenseRepository.add(licenseDef)

      val withName = licenseRepository.withName(testName)
      withName.isDefined should be(true)
      withName.head.id.isDefined should be(true)
      withName.head.id.get should equal(license1.id.get)
    }
  }
}
