package roboliq.input

import org.scalatest.FunSpec
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsValue
import java.io.File
import roboliq.utils.JsonUtils
import roboliq.ai.strips
import roboliq.ai.plan.Unique

class ProtocolHandlerSpec extends FunSpec {
	import ContextValueWrapper._
	import ResultCWrapper._

	val protocolEvaluator = new ProtocolHandler
	val data0 = ResultEData(EvaluatorState(searchPath_l = List(new File("testfiles"), new File("base/testfiles"))))
	val evaluator = new Evaluator()

	val action1 = RjsAction("shakePlate", RjsMap(
		"agent" -> RjsString("mario"),
		"device" -> RjsString("mario__Shaker"),
		"labware" -> RjsString("plate1"),
		"site" -> RjsString("P3"),
		"program" -> RjsBasicMap(
			"rpm" -> RjsNumber(200),
			"duration" -> RjsNumber(10)
		)
	))
	val instruction1 = RjsInstruction("ShakerRun", RjsMap(
		"agent" -> RjsString("mario"),
		"device" -> RjsString("mario__Shaker"),
		"program" -> RjsMap(
			"rpm" -> RjsNumber(200),
			"duration" -> RjsNumber(10)
		),
		"labware" -> RjsString("plate1"),
		"site" -> RjsString("P3")
	))

	val protocol1 = RjsProtocol(
		labwares = Map("plate1" -> RjsProtocolLabware(model_? = Some("plateModel_384_square"), location_? = Some("P3"))),
		substances = Map(),
		sources = Map(),
		commands = List(action1)
	)
	val dataA1 = protocolEvaluator.extractDataA(protocol1).run().value
	
	describe("ProtocolHandlerSpec") {
		it("test protocol 1 without lab info") {
			assert(dataA1.planningDomainObjects == Map(
				"plate1" -> "plate"
			))
			assert(dataA1.planningInitialState == strips.Literals(Unique[strips.Literal](
				strips.Literal.parse("labware plate1"),
				strips.Literal.parse("model plate1 plateModel_384_square"),
				strips.Literal.parse("location plate1 P3")
			)))
			
			val ctxval1 = (for {
				_ <- ResultE.evaluate(RjsImport("shakePlate", "1.0"))
				dataB <- protocolEvaluator.stepB(dataA1)
			} yield dataB.commandExpansions).run(data0)
			
			// strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3")
			val expected1 = Map(
				"1" -> ProtocolCommandResult(
					action1,
					effects = strips.Literals.empty,
					validation_l = List(
						CommandValidation_Precond("agent-has-device mario mario__Shaker"),
						CommandValidation_Precond("device-can-site mario__Shaker P3")
					)
				)
			)
			assert(ctxval1.value == expected1)
		}
		
		it("test protocol 1 with lab info") {
			val dataALab = new ProtocolDataA(
				planningDomainObjects = Map("mario" -> "agent", "mario__Shaker" -> "shaker"),
				planningInitialState = strips.Literals(Unique(
					strips.Literal.parse("agent-has-device mario mario__Shaker"),
					strips.Literal.parse("device-can-site mario__Shaker P3")
				))
			)
			val ctxval2 = (for {
				dataA2 <- ResultE.from(dataALab merge dataA1)
				_ <- ResultE.evaluate(RjsImport("shakePlate", "1.0"))
				dataB <- protocolEvaluator.stepB(dataA2)
			} yield dataB.commandExpansions).run(data0)
			
			// strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3")
			val expected2 = Map(
				"1" -> ProtocolCommandResult(
					action1,
					effects = strips.Literals.empty,
					validation_l = Nil
				),
				"1.1" -> ProtocolCommandResult(
					instruction1,
					effects = strips.Literals.empty,
					validation_l = Nil
				)
			)
			assert(ctxval2.value("1") == expected2("1"))
			assert(ctxval2.value("1.1") == expected2("1.1"))
		}
	}
}