import java.util.Properties

val Scalaversion = "2.12.2"
val Scalatraversion = "2.6.2"
val ScalaLoggingVersion = "3.9.0"
val Log4JVersion = "2.11.0"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.11.231"
val ScalaTestVersion = "3.0.3"
val MockitoVersion = "2.7.22"
val Elastic4sVersion = "6.1.4"
val ElasticsearchVersion = "6.0.0"
val JacksonVersion = "2.7.4"
val JsoupVersion =  "1.11.2"
val OpenHtmlPdfVersion = "0.0.1-RC12"

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
      "gdl" %% "network" % "0.12",
      "gdl" %% "language" % "0.10",
      "gdl" %% "license" % "0.1",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "com.typesafe" % "config" % "1.3.1",
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % JacksonVersion,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % JacksonVersion,
      "joda-time" % "joda-time" % "2.8.2",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "org.json4s"   %% "json4s-native" % "3.5.2",
      "org.scalikejdbc" %% "scalikejdbc" % "3.2.1",
      "org.postgresql" % "postgresql" % "42.2.1",
      "com.zaxxer" % "HikariCP" % "2.7.8",
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "org.flywaydb" % "flyway-core" % "4.0",
      "com.netaporter" %% "scala-uri" % "0.4.16",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.mockito" % "mockito-core" % MockitoVersion % "test",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion % "test",
      "org.jsoup" % "jsoup" % JsoupVersion,
      "coza.opencollab.epub" % "epub-creator" % "1.0.3-GDL",
      "com.openhtmltopdf" % "openhtmltopdf-core" % OpenHtmlPdfVersion,
      "com.openhtmltopdf" % "openhtmltopdf-pdfbox" % OpenHtmlPdfVersion,
      "com.openhtmltopdf" % "openhtmltopdf-jsoup-dom-converter" % OpenHtmlPdfVersion,
      "com.openhtmltopdf" % "openhtmltopdf-rtl-support" % OpenHtmlPdfVersion,
      "com.github.blemale" %% "scaffeine" % "2.3.0",
      "commons-validator" % "commons-validator" % "1.6",
      "io.sentry" % "sentry-log4j2" % "1.7.3"
    )
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

unmanagedResourceDirectories in Compile <+= (baseDirectory) {_ / "src/main/webapp"}

assemblyJarName in assembly := "book-api.jar"
mainClass in assembly := Some("io.digitallibrary.bookapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")  => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")  => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class")  => MergeStrategy.first
  case PathList("META-INF", "org", "apache", "logging", "log4j", "core", "config", "plugins", "Log4j2Plugins.dat")  => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run on Travis as there is no elasticsearch localhost:9200 there yet.
// NB this line will unfortunalty override runs on your local commandline so that
// sbt "test-only -- -n io.digitallibrary.tag.IntegrationTest"
// will not run unless this line gets commented out or you remove the tag over the test class
// This should be solved better!
testOptions in Test += Tests.Argument("-l", "io.digitallibrary.tag.IntegrationTest")
parallelExecution in Test := false

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("openjdk:8-jre-alpine")

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

resolvers += Resolver.mavenLocal
