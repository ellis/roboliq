import AssemblyKeys._
import RoboliqSettings._

lazy val root =
	project.in(file("."))
		.aggregate(reactivesim, base, utils0)
		.settings(roboliqSettings: _*)
		.settings(assemblySettings: _*)

lazy val reactivesim = project.in(file("extern/reactive-sim/core"))

lazy val base = project
	.settings(roboliqSettings: _*)
	.settings(assemblySettings: _*)
	.settings(libraryDependencies ++= Seq(RoboliqDeps.scalatest, RoboliqDeps.scopt, RoboliqDeps.json_gson))
	.settings(initialCommands in console := """import scalaz._, Scalaz._, roboliq.core._""")
	.settings(mainClass in assembly := Some("roboliq.main.Main"))
	.dependsOn(reactivesim)

lazy val utils0 = project
	.settings(roboliqSettings: _*)
	.settings(libraryDependencies ++= Seq(RoboliqDeps.compiler, RoboliqDeps.scalatest, RoboliqDeps.scopt, RoboliqDeps.yaml, RoboliqDeps.ejml, RoboliqDeps.json_gson))
	.settings(initialCommands in console := "import scalaz._, Scalaz._")
