/*package roboliq.input

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsSuccess
import roboliq.entities.Aliquot
import roboliq.entities.Entity
import roboliq.entities.EntityBase
import roboliq.entities.Labware
import roboliq.entities.LabwareModel
import roboliq.entities.Well
import roboliq.entities.WorldState
import roboliq.entities.WorldStateBuilder
import spray.json.JsValue
import scala.reflect.runtime.universe.TypeTag
import roboliq.entities.WellInfo
import roboliq.entities.Agent
import roboliq.entities.WorldStateEvent
import roboliq.entities.Amount
import java.io.File
import spray.json.JsObject

case class ContextDataEvaluation(
	context_r: List[String] = Nil,
	warning_r: List[String] = Nil,
	error_r: List[String] = Nil,
	scope_r: List[Map[String, JsObject]] = Nil
) extends ContextData {
	def logWarning(s: String): ContextData = {
		copy(warning_r = prefixMessage(s) :: warning_r)
	}
	
	def logError(s: String): ContextData = {
		copy(error_r = prefixMessage(s) :: error_r)
	}
	
	def log[A](res: RsResult[A]): ContextData = {
		res match {
			case RsSuccess(_, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r)
			case RsError(error_l, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r, error_r = error_l.reverse.map(prefixMessage) ++ this.error_r)
		}
	}
	
	def pushLabel(label: String): ContextData = {
		copy(context_r = label :: context_r)
	}
	
	def popLabel(): ContextData = {
		copy(context_r = context_r.tail)
	}

	def prefixMessage(s: String): String = {
		(s :: context_r).reverse.mkString(": ")
	}
	
	def pushScope(scope: Map[String, JsObject]): ContextData = {
		copy(scope_r = scope :: scope_r)
	}
	
	def popScope(scope: Map[String, JsObject]): ContextData = {
		scope_r match {
			case Nil =>
				logError("tried to call popScope() on empty scope stack")
			case _ :: rest =>
				copy(scope_r = rest)
		}
	}
	
	def addScopeVariable(name: String, value: JsObject): ContextData = {
		val scope_r_~ = (scope_r.head + (name -> value)) :: scope_r.tail
		copy(scope_r = scope_r_~)
	}
}

trait ContextE[+A] extends ContextT[A] {
}

object ContextE {
	
}
*/