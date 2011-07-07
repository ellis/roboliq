package roboliq.tokens

import scala.collection.mutable.HashMap

import roboliq.robot._
import roboliq.tokens._


class Translator() {
	var robot: Robot = _
	var state: RobotState = _
	val mapTokenHandlers = new HashMap[String, Seq[TokenHandler]]
	
	def translate2(state0: RobotState, tok: T2_Token): Seq[T1_TokenState] = {
		mapTokenHandlers.get(tok.name) match {
			case None => List(new T1_TokenState(new T1_TokenError("no handler for "+tok.name), RobotState.empty))
			case Some(handler :: rest) => handler.translate2(state0, tok)
		}
	}
}
