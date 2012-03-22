/*package roboliq.test.bsse

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.matchers.MustMatchers

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.protocol.CommonProtocol
import roboliq.devices.pipette._
import roboliq.robots.evoware._

import roboliq.labs.bsse._
import roboliq.labs.bsse.station1._


class BsseRobotSpec extends FeatureSpec with GivenWhenThen with ShouldMatchers with MustMatchers {
	val station = new StationConfig
	
	feature("cleans tips at earliest possible occasion") {
		scenario("P1:A1+12 to P2:A1+4,A2+2,A1+4,A2+2") {
			run(station, new Protocol01(station))
		}
	}

	def run(station: StationConfig, protocol: ProtocolTest) {
		val toolchain = new BsseToolchain(station)
		val res1 = toolchain.compileProtocol(protocol, true)
		res1 match {
			case Left(err) =>
				println(err.toString)
			case Right(succT: TranslatorStageSuccess) =>
				val script = succT.internal.asInstanceOf[EvowareScriptBuilder]
				val translator = new EvowareTranslator(toolchain.evowareConfig)
				val s = script.cmds.mkString("\n")
				s must be === protocol.sExpect
			case Right(succ) =>
				println(succ.toString)
		}
		res1.isRight must be === true
	}
}
*/