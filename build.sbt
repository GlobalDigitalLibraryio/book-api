import java.util.Properties

val Scalaversion = "2.12.1"
val Scalatraversion = "2.5.1-NDLA-3"
val ScalaLoggingVersion = "3.7.2"
val Log4JVersion = "2.9.1"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.11.46"
val ScalaTestVersion = "3.0.1"
val MockitoVersion = "1.10.19"
val Elastic4sVersion = "6.1.1"
val ElasticsearchVersion = "6.0.0"
val JacksonVersion = "2.7.4"
val JsoupVersion =  "1.10.2"
val OpenHtmlPdfVersion = "0.0.1-RC11"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val commonSettings = Seq(
  organization := appProperties.value.getProperty("GDLOrganization"),
  version := appProperties.value.getProperty("GDLComponentVersion"),
  scalaVersion := Scalaversion
)

lazy val book_api = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "book-api",
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8"),
    libraryDependencies ++= Seq(
      "gdl" %% "network" % "0.8",
      "gdl" %% "language" % "0.2",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % JacksonVersion,
      "joda-time" % "joda-time" % "2.8.2",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "org.json4s"   %% "json4s-native" % "3.5.0",
      "org.scalikejdbc" %% "scalikejdbc" % "3.0.2",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "org.flywaydb" % "flyway-core" % "4.0",
      "com.netaporter" %% "scala-uri" % "0.4.16",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion % "test",
      "org.jsoup" % "jsoup" % JsoupVersion,
      "coza.opencollab" % "epub-creator" % "1.0.0",
      "com.osinka.i18n" %% "scala-i18n" % "1.0.2",
      "com.openhtmltopdf" % "openhtmltopdf-core" % OpenHtmlPdfVersion,
      "com.openhtmltopdf" % "openhtmltopdf-pdfbox" % OpenHtmlPdfVersion,
      "com.openhtmltopdf" % "openhtmltopdf-jsoup-dom-converter" % OpenHtmlPdfVersion,
      "com.github.blemale" %% "scaffeine" % "2.3.0",
      "commons-validator" % "commons-validator" % "1.6"
    )
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

unmanagedResourceDirectories in Compile <+= (baseDirectory) {_ / "src/main/webapp"}

assemblyJarName in assembly := "book-api.jar"
mainClass in assembly := Some("no.gdl.bookapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")  => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")  => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class")  => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run on Travis as there is no elasticsearch localhost:9200 there yet.
// NB this line will unfortunalty override runs on your local commandline so that
// sbt "test-only -- -n no.gdl.tag.IntegrationTest"
// will not run unless this line gets commented out or you remove the tag over the test class
// This should be solved better!
testOptions in Test += Tests.Argument("-l", "no.gdl.tag.IntegrationTest")

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("java")

    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
  }
}

val gitHeadCommitSha = settingKey[String]("current git commit SHA")
gitHeadCommitSha in ThisBuild := Process("git log --pretty=format:%h -n 1").lines.head

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)


resolvers ++= Seq(
  scala.util.Properties.envOrNone("GDL_RELEASES").map(repo => "GDL Release Sonatype Nexus Repository Manager" at repo),
  scala.util.Properties.envOrNone("NDLA_RELEASES").map(repo => "NDLA Release Sonatype Nexus Repository Manager" at repo)
).flatten

resolvers += "OpenCollab Nexus Release Repo" at "http://nexus.opencollab.co.za/nexus/content/repositories/releases"
