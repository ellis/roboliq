package roboliq.processor2

package roboliq.processor2

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.TypeTag
import grizzled.slf4j.Logger
import org.scalatest.FunSpec
import spray.json._
import roboliq.core._
import ConversionsDirect._
import roboliq.commands2.arm.MovePlateHandler


class ProcessorSpec extends FunSpec {
	private val logger = Logger[this.type]

	describe("MovePlateHandler") {
		val p = new ProcessorData(List(
			new MovePlateHandler
		))
	
		p.loadJsonData(Config.config01)

		p.setCommands(List(aspirate))
	
	//println(p.db.get(TKP("plate", "P1", Nil)))
	p.run(17)
		
	}
	
}