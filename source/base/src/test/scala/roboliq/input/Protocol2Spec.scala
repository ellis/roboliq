package roboliq.input

import org.scalatest.FunSpec
import roboliq.entities.EntityBase
import roboliq.core.RsSuccess
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsString
import spray.json.JsValue
import java.io.File
import roboliq.utils.JsonUtils
import roboliq.ai.strips
import roboliq.ai.plan.Unique

class Protocol2Spec extends FunSpec {
	import ContextValueWrapper._

	val protocolEvaluator = new Protocol2
	val eb = new EntityBase
	val data0 = ContextEData(EvaluatorState(eb, searchPath_l = List(new File("testfiles"))))
	val evaluator = new Evaluator()

	val protocol1 = RjsProtocol(
		labwares = Map("plate1" -> RjsProtocolLabware(model_? = Some("plateModel_384_square"), location_? = Some("P3"))),
		substances = Map(),
		sources = Map(),
		commands = List(
			RjsAction("shakePlate", RjsMap(
				"agent" -> RjsString("mario"),
				"device" -> RjsString("mario__Shaker"),
				"labware" -> RjsString("plate1"),
				"site" -> RjsString("P3"),
				"program" -> RjsMap(
					"rpm" -> RjsNumber(200),
					"duration" -> RjsNumber(10)
				)
			))
		)
	)
	val dataA1 = protocolEvaluator.extractDataA(protocol1)
	
	describe("Protocol2Spec") {
		/*it("processData") {
/*			val yaml = """
object:
  plate1:
    type: plate
    model: plateModel_384_square
    location: P3
  "plate1(A01)":
    type: well
    content: water
  water:
    type: substance
  dye:
    type: substance
  dyeLight:
    type: source
    source: plate1(A01)
    substance:
      dye:
        amount: 1/10
      water:
        amount: 9/10
cmd:
- index: [1]
  command: distribute
  input:
    source: plate1(A01)
    destination: plate1(B01)
    amount: 20ul
"""*/
			val protocol1 = RjsProtocol(
				labwares = Map("plate1" -> RjsProtocolLabware(model_? = Some("plateModel_384_square"), location_? = Some("P3"))),
				substances = Map("water" -> RjsProtocolSubstance(), "dye" -> RjsProtocolSubstance()),
				sources = Map("dyeLight" -> RjsProtocolSource(well = "plate1(A01)", substances = List(
					RjsProtocolSourceSubstance(name = "dye", amount_? = Some("1/10")),
					RjsProtocolSourceSubstance(name = "water")
				))),
				commands = List(
					RjsAction("distribute", RjsMap(
						"source" -> RjsString("dyeLight"),
						"destination" -> RjsString("plate1(B01)"),
						"amount" -> RjsString("20ul")
					))
				)
			)
		
			val dataA = protocol.extractDataA(protocol1)
			assert(dataA.planningDomainObjects == Map(
				"plate1" -> "plate"
			))
			assert(dataA.planningProblemState == strips.State(Set[strips.Atom](
				strips.Atom.parse("labware plate1"),
				strips.Atom.parse("model plate1 plateModel_384_square"),
				strips.Atom.parse("location plate1 P3")
			)))
		}*/
		
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
				_ <- ContextE.evaluate(RjsImport("shakePlate", "1.0"))
				dataB <- protocolEvaluator.stepB(dataA1)
			} yield dataB.commandExpansions).run(data0)
			
			// strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3")
			val expected1 = Map(
				"1" -> ProtocolCommandResult(
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
			val dataALab = new Protocol2DataA(
				objects = RjsMap(),
				commands = RjsMap(),
				commandOrderingConstraints = Nil,
				commandOrder = Nil,
				planningDomainObjects = Map("mario" -> "agent", "mario__Shaker" -> "shaker"),
				planningInitialState = strips.Literals(Unique(
					strips.Literal.parse("agent-has-device mario mario__Shaker"),
					strips.Literal.parse("device-can-site mario__Shaker P3")
				))
			)
			val dataA2 = dataALab ++ dataA1
			val ctxval2 = (for {
				_ <- ContextE.evaluate(RjsImport("shakePlate", "1.0"))
				dataB <- protocolEvaluator.stepB(dataA2)
			} yield dataB.commandExpansions).run(data0)
			
			// strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3")
			val expected2 = Map(
				"1" -> ProtocolCommandResult(
					effects = strips.Literals.empty,
					validation_l = Nil
				)
			)
			assert(ctxval2.value == expected2)
			
			/*// Completely specify parameters
			val yaml1 = """
object:
  plate1:
    type: plate
    model: plateModel_384_square
    location: P3
command:
  "1":
    command: shakePlate
    input:
      agent: mario
      device: mario__Shaker
      labware: plate1
      site: P3
      program:
        rpm: 200
        duration: 10
"""
			// Omit 'device' parameter
			val yaml2 = """
object:
  plate1:
    type: plate
    model: plateModel_384_square
    location: P3
command:
  "1":
    command: shakePlate
    input:
      agent: mario
      labware: plate1
      site: P3
      program:
        rpm: 200
        duration: 10
"""
			val jsobj1 = JsonUtils.yamlToJson(yaml1).asInstanceOf[JsObject]
			val jsobj2 = JsonUtils.yamlToJson(yaml2).asInstanceOf[JsObject]
			val ctx = for {
				rjsval1 <- RjsValue.fromJson(jsobj1)
				rjsval2 <- RjsValue.fromJson(jsobj2)
				m1 = rjsval1.asInstanceOf[RjsMap]
				m2 = rjsval2.asInstanceOf[RjsMap]
				_ <- ContextE.evaluate(RjsImport("shakePlate", "1.0"))
				_ = println("A")
				// Get state from protocol data
				temp0 <- protocol.processData(m1)
				_ = println("B")
				state0 = temp0._2
				// Add a couple atoms to the state regarding the robot's setup
				state1 = strips.State(state0.atoms + strips.Atom.parse("agent-has-device mario mario__Shaker") + strips.Atom.parse("device-can-site mario__Shaker P3"))
				// Leave out a necessary state value, in order to get a precondition error
				state2 = strips.State(state0.atoms + strips.Atom.parse("agent-has-device mario mario__Shaker"))
				validation1_l <- protocol.validateDataCommand(state1, m1, "1")
				_ = println("C")
				validation2_l <- protocol.validateDataCommand(state1, m2, "1")
				validation3_l <- protocol.validateDataCommand(state2, m1, "1")
				validation4_l <- protocol.validateDataCommand(state2, m2, "1")
			} yield {
				assert(validation1_l == Nil)
				assert(validation2_l == List(CommandValidation_Param("device")))
				assert(validation3_l == List(CommandValidation_Precond("device-can-site mario__Shaker P3")))
				assert(validation4_l == List(CommandValidation_Param("device")))
			}
			val (data1, _) = ctx.run(data0)
			if (!data1.error_r.isEmpty) {
				println("ERRORS:")
				data1.error_r.foreach(println)
			}
			assert(data1.error_r == Nil)
			*/
		}
	}
}