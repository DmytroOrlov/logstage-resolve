import java.nio.file.Files

import Version._

val V = new {
  val distage = "0.9.17"
  val tapir = "0.12.7"
  val sttp = "2.0.0-RC4"
  val zio = "1.0.0-RC17"
  val zioCats = "2.0.0.0-RC10"
  val zioMacros = "0.6.0"

  val scalatest = "3.1.0"
  val scalacheck = "1.14.2"
  val http4s = "0.21.0-M6"
  val kindProjector = "0.11.0"
  val circeDerivation = "0.12.0-M7"
  val cats = "2.0.0"
  val circe2_12 = "0.11.2"
  val circe: Version = {
    case Some((2, 13)) => "0.12.3"
    case _ => circe2_12
  }
  val circeExtras: Version = {
    case Some((2, 13)) => "0.12.2"
    case _ => circe2_12
  }
  val refined = "0.9.10"
  val commonsText = "1.8"
  val chimney = "0.3.5"
  val scalaCsv = "1.3.6"
  val betterMonadicFor = "0.3.1"
  val prometheus = "0.8.0"
}

val Deps = new {
  val scalatest = "org.scalatest" %% "scalatest" % V.scalatest
  val scalacheck = "org.scalacheck" %% "scalacheck" % V.scalacheck

  val distageCore = "io.7mind.izumi" %% "distage-core" % V.distage
  val distageRoles = "io.7mind.izumi" %% "distage-roles" % V.distage
  val distageConfig = "io.7mind.izumi" %% "distage-config" % V.distage
  val distageTestkit = "io.7mind.izumi" %% "distage-testkit" % V.distage
  val logstageCore = "io.7mind.izumi" %% "logstage-core" % V.distage

  val http4sDsl = "org.http4s" %% "http4s-dsl" % V.http4s
  val http4sServer = "org.http4s" %% "http4s-blaze-server" % V.http4s
  val http4sClient = "org.http4s" %% "http4s-blaze-client" % V.http4s
  val http4sCirce = "org.http4s" %% "http4s-circe" % V.http4s
  val http4sPrometheus = "org.http4s" %% "http4s-prometheus-metrics" % V.http4s

  val prometheusCommon = "io.prometheus" % "simpleclient_common" % V.prometheus
  val prometheusHotspot = "io.prometheus" % "simpleclient_hotspot" % V.prometheus
  val prometheusSimpleclient = "io.prometheus" % "simpleclient" % V.prometheus

  val circeDerivation = "io.circe" %% "circe-derivation" % V.circeDerivation

  val kindProjector = "org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor

  val zio = "dev.zio" %% "zio" % V.zio
  val zioTestSbt = "dev.zio" %% "zio-test-sbt" % V.zio
  val zioMacros = "dev.zio" %% "zio-macros-core" % V.zioMacros
  val zioCats = "dev.zio" %% "zio-interop-cats" % V.zioCats

  val tapirJsonCirce = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % V.tapir
  val tapirSttpClient = "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % V.tapir

  val asyncHttpClientBackendZio = "com.softwaremill.sttp.client" %% "async-http-client-backend-zio" % V.sttp
}

val scala2_12 = "2.12.10"
val scala2_13 = "2.13.1"

lazy val is2_12 = settingKey[Boolean]("Is the scala version 2.12.")

def dependenciesFor(version: String)(deps: (Option[(Long, Long)] => ModuleID)*): Seq[ModuleID] =
  deps.map(_.apply(CrossVersion.partialVersion(version)))

lazy val nonTestScalacOptions = "-Xfatal-warnings"

lazy val `myapp` = (project in file("."))
  .enablePlugins(NoPublishPlugin, BuildInfoPlugin, DockerPlugin, JavaServerAppPackaging)
  .configs(FunTest)
  .settings(inConfig(FunTest)(Defaults.testSettings))
  .settings(
    inThisBuild(Seq(
      scalaVersion := scala2_13,
      addCompilerPlugin(Deps.kindProjector),
      addCompilerPlugin(Deps.betterMonadicFor),
    )),
    scalacOptions ++= nonTestScalacOptions :: List(
      "-language:higherKinds",
      "-feature",
      "-deprecation",
      "-unchecked",
    ) ++ (if (!is2_12.value) Some("-Ymacro-annotations") else None),
    scalacOptions in Test -= nonTestScalacOptions,
    libraryDependencies ++= dependenciesFor(scalaVersion.value)(
      "io.circe" %% "circe-core" % V.circe(_),
      "io.circe" %% "circe-generic" % V.circe(_),
      "io.circe" %% "circe-parser" % V.circe(_),
      "io.circe" %% "circe-refined" % V.circe(_),
      "io.circe" %% "circe-generic-extras" % V.circeExtras(_),
    ) ++ Seq(
      Deps.distageCore,
      Deps.distageRoles,
      Deps.distageConfig,
      Deps.distageTestkit % Test,
      Deps.scalatest % Test,
      Deps.scalacheck % Test,
      Deps.http4sDsl,
      Deps.http4sServer,
      Deps.http4sPrometheus,
      Deps.prometheusCommon,
      Deps.prometheusHotspot,
      Deps.prometheusSimpleclient,
      Deps.http4sClient % Test,
      Deps.circeDerivation,
      Deps.zio,
      Deps.zioCats,
      Deps.zioTestSbt % Test,
      Deps.zioMacros,

      "org.typelevel" %% "cats-core" % V.cats,
      "org.typelevel" %% "cats-effect" % V.cats,

      Deps.asyncHttpClientBackendZio,
      Deps.tapirSttpClient % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % V.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % V.tapir,

      Deps.logstageCore,
      "io.7mind.izumi" %% "logstage-rendering-circe" % V.distage,
      // Router from Slf4j to LogStage
      "io.7mind.izumi" %% "logstage-adapter-slf4j" % V.distage,
      // Configure LogStage with Typesafe Config
      "io.7mind.izumi" %% "logstage-config" % V.distage,
      // LogStage integration with DIStage
      "io.7mind.izumi" %% "logstage-di" % V.distage,
      // Router from LogStage to Slf4J
      "io.7mind.izumi" %% "logstage-sink-slf4j" % V.distage,

      "org.apache.commons" % "commons-text" % V.commonsText,
      "eu.timepit" %% "refined" % V.refined,
      "io.scalaland" %% "chimney" % V.chimney,

      "com.github.tototoshi" %% "scala-csv" % V.scalaCsv % Test,
    ),
    dockerExposedPorts += 8080,
    dockerAliases += dockerAlias.value.withTag(Some("SNAPSHOT")),
    dynverSeparator in ThisBuild := "-",
    dockerEntrypoint := Seq("sh", "-c", s"""${dockerEntrypoint.value.mkString(" ")} -u env:$${APP_ENV:-prod}"""),
    dockerBaseImage := "openjdk:11",
    is2_12 := scalaVersion.value.startsWith("2.12."),
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))

lazy val FunTest = config("fun") extend Test
