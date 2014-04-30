package roboliq.commands

import aiplan.strips2.PartialPlan
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.core.pimpedEither1
import roboliq.plan.Call
import roboliq.plan.CallTree
import roboliq.plan.CommandSet
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsString
import roboliq.entities.EntityBase
import roboliq.plan.ActionHandler
import roboliq.plan.ActionPlanInfo
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import scalax.collection.Graph
import scalax.collection.edge.LHyperEdge
import scalax.collection.edge.LkUnDiEdge
/*
object Test {
	def main(args: Array[String]) {
		val eb = {
			import roboliq.entities._
			val r1 = Agent("r1", Some("r1"))
			val sm001 = SiteModel("sm001", Some("sm001"), None)
			val siteA = Site("siteA", Some("siteA"))
			val siteB = Site("siteB", Some("siteB"))
			val shaker = Shaker("r1_shaker", Some("r1_shaker"))
			val m001 = PlateModel("m001", Some("m001"), None, 8, 12, LiquidVolume.ul(300))
			val plateA = Plate("plateA", Some("plateA"))
			val eb = new EntityBase
			eb.addAgent(r1, r1.key)
			eb.addModel(sm001, sm001.key)
			eb.addSite(siteA, siteA.key)
			eb.addSite(siteB, siteB.key)
			eb.addDevice(r1, shaker, shaker.key)
			eb.addDeviceSite(shaker, siteB)
			eb.addModel(m001, m001.key)
			eb.addLabware(plateA, plateA.key)
			eb.transportGraph = Graph[Site, LkUnDiEdge](LkUnDiEdge(siteA, siteB)(("user", "", "")))
			eb
		}
		
		val autoHandler_l = List[AutoActionHandler_TransportLabware](
			new AutoActionHandler_TransportLabware
		)
		val handler_l = List[ActionHandler](
			new ActionHandler_ShakePlate
		)
		val cs = new CommandSet(
			nameToAutoActionHandler_m = autoHandler_l.map(h => h.getName -> h).toMap,
			nameToActionHandler_m = handler_l.map(h => h.getName -> h).toMap,
			nameToMethods_m = Map(
				/*"shakePlate" -> List(
					shakePlate_to_tecan_shakePlate,
					(call: Call) => RqSuccess(call.copy(name = "tecan_shakePlate")
				)*/
			)
		)
		val top_l = List(
			new Call("shakePlate", List(
				Some("object") -> JsString("plateA"),
				Some("program") -> JsObject(Map("rpm" -> JsNumber(200)))
			))
		)
		val tree0 = CallTree(top_l)
		
		val x = for {
			tree1 <- CallTree.expandTree(cs, tree0)
			tree2 <- CallTree.expandTree(cs, tree1)
			planInfo_l <- CallTree.getActionPlanInfo(cs, tree2)
			_ = println("planInfo_l:")
			_ = println(planInfo_l)
			_ = println("domain:")
			domain <- createDomain(cs, planInfo_l)
			_ = println(domain.toStripsText)
			problem <- createProblem(planInfo_l, domain)
			_ = println(problem.toStripsText)
			plan0 = PartialPlan.fromProblem(problem)
			plan1 <- plan0.addActionSequence(planInfo_l.map(_.planAction)).asRs
			step0 = aiplan.strips2.PopState_SelectGoal(plan1, 0)
			plan2 <- aiplan.strips2.Pop.stepToEnd(step0).asRs
			_ = println("plan2:")
			_ = println(plan2.toDot())
			plan3 <- aiplan.strips2.Pop.groundPlan(plan2).asRs
			_ = println("plan3:")
			_ = println(plan3.toDot())
		} yield {
			roboliq.utils.FileUtils.writeToFile("test.dot", plan3.toDot())
			val actionOrig_l = planInfo_l zip plan3.action_l.drop(2)
			actionOrig_l.map(pair => {
				val (planInfo, action) = pair
				val planned = plan3.bindings.bind(action)
				val handler = cs.nameToActionHandler_m(planInfo.planAction.name)
				val op = handler.getInstruction(planInfo, planned, eb)
				println("op:")
				println(op)
				op
			})
			// Additional actions added by the planner
			val actionAuto_l = plan3.action_l.toList.drop(2 + planInfo_l.size)
			actionAuto_l.map(action => {
				val planned = plan3.bindings.bind(action)
				val handler = cs.nameToAutoActionHandler_m(action.name)
				val op = handler.getInstruction(planned, eb)
				println("op:")
				println(op)
				op
			})
		}
		x match {
			case RqError(e, w) =>
				println("ERRORS: "+e)
				println("WARNINGS: "+w)
			case _ =>
		}
	}
	
	def createDomain(cs: CommandSet, planInfo_l: List[ActionPlanInfo]): RqResult[Strips.Domain] = {
		val operator_l = cs.nameToAutoActionHandler_m.values.map(_.getDomainOperator).toList ++ planInfo_l.map(_.domainOperator)

		RqSuccess(Strips.Domain(
			type_l = List(
				"labware",
				"model",
				"site",
				"siteModel",
				
				"agent",
				
				"pipetter",
				"pipetterProgram",
				
				"shaker",
				"shakerProgram"
			),
			constantToType_l = Nil,
			predicate_l = List[Strips.Signature](
				Strips.Signature("agent-has-device", "?agent" -> "agent", "?device" -> "device"),
				Strips.Signature("device-can-site", "?device" -> "device", "?site" -> "site"),
				Strips.Signature("location", "?labware" -> "labware", "?site" -> "site"),
				Strips.Signature("model", "?labware" -> "labware", "?model" -> "model"),
				Strips.Signature("stackable", "?sm" -> "siteModel", "?m" -> "model")
			),
			operator_l = operator_l
		))
	}


	def createProblem(planInfo_l: List[ActionPlanInfo], domain: Strips.Domain): RqResult[Strips.Problem] = {
		val typToObject_l: List[(String, String)] = List(
			"agent" -> "r1",
			"pipetter" -> "r1_pipetter",
			"shaker" -> "r1_shaker",
			"model" -> "m001",
			"siteModel" -> "sm001",
			"site" -> "siteA",
			"site" -> "siteB",
			"labware" -> "plateA",
			"labware" -> "plateB"
		) ++ planInfo_l.flatMap(_.problemObjectToTyp_l).map(_.swap)
		val state0 = Strips.State(Set[Strips.Atom](
			Strips.Atom("location", "plateA", "siteA"),
			Strips.Atom("site-blocked", "siteA"),
			Strips.Atom("agent-has-device", "r1", "r1_pipetter"),
			Strips.Atom("agent-has-device", "r1", "r1_shaker"),
			Strips.Atom("model", "plateA", "m001"),
			Strips.Atom("device-can-site", "r1_pipetter", "siteB"),
			Strips.Atom("device-can-site", "r1_shaker", "siteB"),
			Strips.Atom("model", "siteA", "sm001"),
			Strips.Atom("model", "siteB", "sm001"),
			Strips.Atom("stackable", "sm001", "m001")
		) ++ planInfo_l.flatMap(_.problemState_l))
		
		RqSuccess(Strips.Problem(
			domain = domain,
			typToObject_l = typToObject_l,
			state0 = state0,
			goals = Strips.Literals.empty
		))
	}
}
*/