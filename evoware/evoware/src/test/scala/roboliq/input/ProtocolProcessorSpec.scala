/*package roboliq.input

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

class ProtocolProcessorSpec extends FunSpec {
	import ContextValueWrapper._
	import ResultCWrapper._

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
	val details1 = ProtocolProcessor.extractProtocolData(protocol1).run().value
	
	describe("ProtocolProcessorSpec") {
		it("test protocol 1 without lab info") {
			assert(details1.planningDomainObjects == Map(
				"plate1" -> "plate"
			))
			assert(details1.planningInitialState == strips.Literals(Unique[strips.Literal](
				strips.Literal.parse("labware plate1"),
				strips.Literal.parse("model plate1 plateModel_384_square"),
				strips.Literal.parse("location plate1 P3")
			)))
			
			// strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3")
			val ctxval1 = for {
				action1Basic <- RjsValue.fromValueToBasicValue(action1)
			} yield Map("1" -> action1Basic)
			assert(details1.commands == ctxval1.run().value)
			
			//Map("1" -> RjsBasicMap("INPUT" -> RjsBasicMap("agent" -> RjsString("mario"), "device" -> RjsString("mario__Shaker"), "labware" -> RjsString("plate1"), "program" -> RjsBasicMap("duration" -> RjsNumber(10,None), "rpm" -> RjsNumber(200,None)), "site" -> RjsString("P3")), "NAME" -> RjsString("shakePlate"), "TYPE" -> RjsString("action")))
			//did not equal
			//Map("1" -> RjsAction(shakePlate,RjsMap("agent" -> RjsString("mario"), "device" -> RjsString("mario__Shaker"), "labware" -> RjsString("plate1"), "program" -> RjsBasicMap("duration" -> RjsNumber(10,None), "rpm" -> RjsNumber(200,None)), "site" -> RjsString("P3")))) (ProtocolProcessorSpec.scala:63)
		}
		/*
		it("test protocol 1 without lab info") {
			assert(details1.planningDomainObjects == Map(
				"plate1" -> "plate"
			))
			assert(details1.planningInitialState == strips.Literals(Unique[strips.Literal](
				strips.Literal.parse("labware plate1"),
				strips.Literal.parse("model plate1 plateModel_384_square"),
				strips.Literal.parse("location plate1 P3")
			)))
			
			val ctxval1 = for {
				_ <- ResultE.evaluate(RjsImport("shakePlate", "1.0"))
				details2 <- protocolEvaluator.expandCommands(details1)
			} yield details2
			
			// strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3")
			val expected1 = Map(
				"1" -> CommandInfo(
					action1,
					effects = strips.Literals.empty,
					validations = List(
						CommandValidation("agent-has-device mario mario__Shaker", precond_? = Some(1)),
						CommandValidation("device-can-site mario__Shaker P3", precond_? = Some(2))
					)
				)
			)
			assert(ctxval1.run(data0).value.commands == expected1)
		}
		*/
		/*
		it("test protocol 1 with lab info") {
			val dataALab = new ProtocolData(
				planningDomainObjects = Map("mario" -> "agent", "mario__Shaker" -> "shaker"),
				planningInitialState = strips.Literals(Unique(
					strips.Literal.parse("agent-has-device mario mario__Shaker"),
					strips.Literal.parse("device-can-site mario__Shaker P3")
				))
			)
			val ctxval2 = for {
				details2 <- ResultE.from(dataALab merge details1)
				_ <- ResultE.evaluate(RjsImport("shakePlate", "1.0"))
				details3 <- protocolEvaluator.expandCommands(details2)
			} yield details3
			
			// strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3")
			val expected2 = Map(
				"1" -> CommandInfo(
					action1,
					effects = strips.Literals.empty
				),
				"1.1" -> CommandInfo(
					instruction1,
					effects = strips.Literals.empty
				)
			)
			assert(ctxval2.run(data0).value.commands("1") == expected2("1"))
			assert(ctxval2.run(data0).value.commands("1.1") == expected2("1.1"))
		}
		*/
	}
}*/