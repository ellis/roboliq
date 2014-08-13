import sbt._
import sbt.Keys._

object RoboliqSettings {
	val buildOrganization = "bsse.ethz.ch"
	val buildVersion      = "0.1"
	val buildScalaVersion = "2.11.2"

	import RoboliqDeps._

	val roboliqSettings = Defaults.defaultSettings ++ Seq (
		organization := buildOrganization,
		version      := buildVersion,
		scalaVersion := buildScalaVersion,
		scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
		resolvers ++= Seq(
			"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
			"io.spray repo" at "http://repo.spray.io",
			Resolver.sonatypeRepo("public")
		),
		libraryDependencies ++= Seq(reflect, commons_io, scalaz, grizzled, logback, json_spray, yaml),
		parallelExecution in Test := false
	)

	// Dependencies
	object RoboliqDeps {
		val compiler = "org.scala-lang" % "scala-compiler" % "2.11.2"
		val reflect = "org.scala-lang" % "scala-reflect" % "2.11.2"
		val commons_io = "commons-io" % "commons-io" % "2.2"
		val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.0"
		val grizzled = "org.clapper" %% "grizzled-slf4j" % "1.0.2"
		val logback = "ch.qos.logback" % "logback-classic" % "1.0.7"
		val scalatest = "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"
		//val akka = "com.typesafe.akka" %% "akka-actor" % "2.1.0"
		val scopt = "com.github.scopt" %% "scopt" % "3.2.0"
		val yaml = "org.yaml" % "snakeyaml" % "1.10"
		val ejml = "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.16"
		val json_gson = "com.google.code.gson" % "gson" % "2.2.1"
		val json_spray = "io.spray" %% "spray-json" % "1.2.6"
		val scala_graph_core = "com.assembla.scala-incubator" %% "graph-core" % "1.9.0"
		val saddle = "org.scala-saddle" %% "saddle-core" % "1.3.+"
		val saddle_hdf5 = "org.scala-saddle" %% "saddle-hdf5" % "1.3.+"
	}
}
