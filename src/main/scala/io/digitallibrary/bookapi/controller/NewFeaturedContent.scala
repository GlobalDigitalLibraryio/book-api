package io.digitallibrary.bookapi.controller

case class NewFeaturedContent(language: String, title: String, description: String, link: String, imageUrl: String, category: Option[String])

