package roboliq.labs.bsse

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._

import station1._


object Main extends App {
	val station = new StationConfig
	val protocol = new examples.Example02(station)
	val toolchain = new BsseToolchain(station)
	toolchain.compileProtocol(protocol, true) match {
		case Left(err) => err.print()
		case Right(succ) => succ.print()
	}
}
