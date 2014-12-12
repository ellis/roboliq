package roboliq.evoware.commands

import roboliq.ai.strips
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.WorldState
import roboliq.evoware.translator.L0C_StartTimer
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsNumber


case class EvowareTimerSleepActionParams(
	agent_? : Option[String],
	id: Int,
	duration: Int // duration in seconds
)

class EvowareTimerSleepActionHandler extends ActionHandler {
	
	def getActionName = "evoware.timer.sleep"

	def getActionParamNames = List("agent", "id", "duration")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[EvowareTimerSleepActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "evoware.timer.start", binding, paramToJsval_l.toMap) ::
			OperatorInfo(id, Nil, Nil, "evoware.timer.wait", binding, (("till" -> JsNumber(params.duration)) :: paramToJsval_l).toMap) :: Nil
		}
	}
}
