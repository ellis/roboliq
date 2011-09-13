package roboliq.labs.bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import evoware._


object Main extends App {
	val protocol = new examples.Example02
	val toolchain = new BsseToolchain(protocol.lab.sites)
	toolchain.compile(protocol, true) match {
		case Left(err) => err.print()
		case Right(succ) => succ.print()
	}
}
