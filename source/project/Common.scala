import sbt._
import sbt.Keys._

object RoboliqSettings {
	val buildOrganization = "bsse.ethz.ch"
	val buildVersion      = "0.1"
	val buildScalaVersion = "2.10.3"

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
		val compiler = "org.scala-lang" % "scala-compiler" % "2.10.3"
		val reflect = "org.scala-lang" % "scala-reflect" % "2.10.3"
		val commons_io = "commons-io" % "commons-io" % "2.2"
		val scalaz = "org.scalaz" % "scalaz-core_2.10" % "7.0.0-M7"
		val grizzled = "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1"
		val logback = "ch.qos.logback" % "logback-classic" % "1.0.7"
		val scalatest = "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
		val akka = "com.typesafe.akka" %% "akka-actor" % "2.1.0"
		val scopt = "com.github.scopt" %% "scopt" % "3.2.0"
		val yaml = "org.yaml" % "snakeyaml" % "1.10"
		val ejml = "com.googlecode.efficient-java-matrix-library" % "ejml" % "0.16"
		val json_gson = "com.google.code.gson" % "gson" % "2.2.1"
		val json_spray = "io.spray" % "spray-json_2.10" % "1.2.3"
		val scala_graph_core = "com.assembla.scala-incubator" % "graph-core_2.10" % "1.8.0"
		val saddle = "org.scala-saddle" %% "saddle-core" % "1.3.+"
		val saddle_hdf5 = "org.scala-saddle" %% "saddle-hdf5" % "1.3.+"
	}
}
