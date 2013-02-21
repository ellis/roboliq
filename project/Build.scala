import sbt._
import sbt.Keys._

object BuildSettings {
  val buildOrganization = "bsse.ethz.ch"
  val buildVersion      = "0.1"
  val buildScalaVersion = "2.10.0"

  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "io.spray repo" at "http://repo.spray.io"
    ),
	libraryDependencies ++= Seq(reflect, commons_io, scalaz, grizzled, logback, json_spray)
  )
  
	// Dependencies
	val compiler = "org.scala-lang" % "scala-compiler" % "2.10.0"
	val reflect = "org.scala-lang" % "scala-reflect" % "2.10.0"
	val commons_io = "commons-io" % "commons-io" % "2.2"
	val scalaz = "org.scalaz" % "scalaz-core_2.10" % "7.0.0-M7"
	val grizzled = "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1"
	val logback = "ch.qos.logback" % "logback-classic" % "1.0.7"
	val scalatest = "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
	val akka = "com.typesafe.akka" %% "akka-actor" % "2.1.0"
	val scopt = "com.github.scopt" %% "scopt" % "2.1.0"
	val yaml = "org.yaml" % "snakeyaml" % "1.10"
	val ejml = "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.16"
	val json_gson = "com.google.code.gson" % "gson" % "2.2.1"
	val json_spray = "io.spray" % "spray-json_2.10" % "1.2.3"
}

object MyBuild extends Build {
	import BuildSettings._

	lazy val root = Project(
			id = "root",
			base = file("."),
			settings = buildSettings
		) aggregate(projBase, projUtils0)

	lazy val projBase = Project(
			id = "base", 
			base = file("base"),
			settings = buildSettings ++ Seq(
				name := "base",
				libraryDependencies ++= Seq(scalatest, akka, yaml, ejml),
				initialCommands in console := """import scalaz._, Scalaz._, roboliq.core._"""
			)
		)

	lazy val projEvoware = Project(
			id = "evoware",
			base = file("evoware"),
			settings = buildSettings ++ Seq(
				name := "evoware",
				libraryDependencies ++= Seq(scalatest, yaml, ejml)
			)
		) dependsOn(projBase)

	lazy val projBsse = Project(
			id = "bsse",
			base = file("bsse"),
			settings = buildSettings ++ Seq(
				name := "bsse",
				libraryDependencies ++= Seq(scalatest, yaml, ejml)
			)
		) dependsOn(projBase, projEvoware)
	
	lazy val projUtils0 = Project(
			id = "utils0",
			base = file("utils0"),
			settings = buildSettings ++ Seq(
				name := "utils0",
				libraryDependencies ++= Seq(compiler, scalatest, scopt, yaml, ejml, json_gson),
				initialCommands in console := "import scalaz._, Scalaz._"
			)
		)

	//addSbtPlugin("org.ensime" % "ensime-sbt-cmd" % "0.1.0")
}
