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
			eb
		}
		
		val tecan_shakePlate_handler = new ActionHandler_ShakePlate
		def shakePlate_to_tecan_shakePlate(call: Call): RqResult[Call] = {
			RqSuccess(call.copy(name = "tecan_shakePlate"))
		}
		val cs = new CommandSet(
			nameToActionHandler_m = Map("shakePlate" -> tecan_shakePlate_handler),
			nameToMethods_m = Map("shakePlate" -> List(shakePlate_to_tecan_shakePlate))
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
			domain <- CallTree.createDomain(planInfo_l)
			_ = println(domain.toStripsText)
			problem <- CallTree.createProblem(planInfo_l, domain)
			_ = println(problem.toStripsText)
			plan0 = PartialPlan.fromProblem(problem)
			// FIXME: Add all actions, not just the first
			plan1 <- plan0.addAction(planInfo_l.head.planAction).asRs
			step0 = aiplan.strips2.PopState_SelectGoal(plan1, 0)
			plan2 <- aiplan.strips2.Pop.stepToEnd(step0).asRs
			_ = println(plan2.toDot)
		} yield {
			val planInfo = planInfo_l.head
			val planned = plan2.bindings.bind(plan2.action_l(2))
			val handler = cs.nameToActionHandler_m(planInfo.planAction.name)
			val op = handler.getOperator(planInfo, planned, eb)
			println("op:")
			println(op)
		}
		x match {
			case RqError(e, w) =>
				println("ERRORS: "+e)
				println("WARNINGS: "+w)
			case _ =>
		}
	}

}