/*
package roboliq.plan

import roboliq.ai.strips
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.core.RsResult
import roboliq.core.RsSuccess
import spray.json.JsNull
import spray.json.JsValue


class Call(
	val name: String,
	val args: List[(Option[String], JsValue)]
) {
	def copy(
		name: String = name,
		args: List[(Option[String], JsValue)] = args
	): Call = {
		new Call(name, args)
	}
}

/*
sealed trait Command

case class Task(
	call: Call,
	list: List[Call]
) extends Command

//trait Procedure extends Command

//trait Method extends Command

trait Action extends Command {
	def getDomainOperator(id: List[Int]): RqResult[roboliq.ai.strips.Operator]
	def getProblemParamMap(id: List[Int], jsval_l: List[JsValue]): RqResult[Map[String, String]]
	//def getOperators(id: List[Int], jsval_l: List[JsValue], )
}

case class UnknownAction(
	call: Call
) extends Action {
	def getDomainOperator(id: List[Int]): RqResult[roboliq.ai.strips.Operator] = {
		RqSuccess(roboliq.ai.strips.Operator(
			name = "action_"+id.mkString("_"),
			paramName_l = Nil,
			paramTyp_l = Nil,
			preconds = roboliq.ai.strips.Literals.empty,
			effects = roboliq.ai.strips.Literals.empty
		))
	}

	def getProblemParamMap(id: List[Int], jsval_l: List[JsValue]): RqResult[Map[String, String]] = {
		RqSuccess(Map())
	}
}
*/

/**
 * @param id A command id for the operator
 * @param problemObjectToTyp_l A list of objects and their types for any new objects that this action needs to insert into the problem
 * @param problemState_l Any state atoms to insert into the problem
 * @param operatorName Name of the operator to lookup
 * @param operatorBinding_m Bindings for this operator (i.e. any settings for the domain operators parameters)
 * @param instructionParam_m Any parameters settings required to later generate the instruction from the planned operator
 */
case class OperatorInfo(
	id: List[Int],
	problemObjectToTyp_l: List[(String, String)],
	problemState_l: List[strips.Atom],
	operatorName: String,
	operatorBinding_m: Map[String, String],
	instructionParam_m: Map[String, JsValue]
)

trait ActionHandler {
	def getActionName: String
	
	def getActionParamNames: List[String]
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]]
}

trait OperatorHandler {
	def getDomainOperator: strips.Operator
	def getInstruction(
		operator: strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit]
}

/*
trait Operator extends Command

sealed trait CommandNode {
	val child_l: List[CommandNode]
}
case class CommandNode_Root(child_l: List[CommandNode]) extends CommandNode
case class CommandNode_Call(call: Call, child_l: List[CommandNode]) extends CommandNode
case class CommandNode_Task(call: Call, child_l: List[CommandNode]) extends CommandNode
case class CommandNode_Procedure(call: Call, child_l: List[CommandNode]) extends CommandNode
case class CommandNode_Method(call: Call, child_l: List[CommandNode]) extends CommandNode
case class CommandNode_Action(call: Call, child_l: List[CommandNode]) extends CommandNode
case class CommandNode_Operator(call: Call, child_l: List[CommandNode]) extends CommandNode
*/

/*
class Signature(val name: String, val paramName_l: List[String], val paramTyp_l: List[String]) {
	assert(paramTyp_l.length == paramName_l.length)
	
	def getSignatureString = name + (paramName_l zip paramTyp_l).map(pair => s"${pair._1}:${pair._2}").mkString("(", " ", ")")
	override def toString = getSignatureString
}

case class ProcedureSpec(
	signature: Signature,
	cmd_l: List[Call]
)*/

/**
 * @param nameToAutoOperator_l List of operator names which should be automatically added to the domain
 */
class CommandSet(
	val nameToActionHandler_m: Map[String, ActionHandler],
	val nameToOperatorHandler_m: Map[String, OperatorHandler],
	val nameToAutoOperator_l: List[String],
	val nameToMethods_m: Map[String, List[Call => RqResult[Call]]]
) {
	def getOperatorHandler(name: String): RsResult[OperatorHandler] =
		RsResult.from(nameToOperatorHandler_m.get(name), s"unknown operator `${name}`")
}

sealed trait CallExpandResult

case class CallExpandResult_Children(
	child_l: List[Call]
) extends CallExpandResult

case class CallExpandResult_Inputs(
	input_l: List[(String, List[String])]
) extends CallExpandResult

/*
trait CallHandler {
	def expand(
		cs: CommandSet,
		id: List[Int],
		call: Call,
		variable_m: Map[String, String]
	): RqResult[CallExpandResult]
}

class CallHandler_Action {
	def expand(
		cs: CommandSet,
		id: List[Int],
		call: Call,
		variable_m: Map[String, String]
	): RqResult[CallExpandResult] = {
		RqSuccess(CallExpandResult_Children(Nil))
	}
}*/

case class CallTree(
	val top_l: List[Call],
	val frontier_l: Set[Call],
	val parent_m: Map[Call, Call],
	val children_m: Map[Call, List[Call]],
	val idToCall_m: Map[List[Int], Call],
	val callToId_m: Map[Call, List[Int]],
	val callToInputs_m: Map[Call, List[(String, List[String])]],
	val callToVariables_m: Map[Call, Map[String, String]]
	//val taskToCall_m: Map[Task, Call]
) {
	def getId(cmd: Call): RqResult[List[Int]] = {
		RsResult.from(callToId_m.get(cmd), s"getId: no ID registered for command: $cmd")
	}
	
	def expand(call: Call, child_l: List[Call]): RqResult[CallTree] = {
		for {
			_ <- RqResult.assert(frontier_l.contains(call), s"Tried to expand command that's not in frontier: call")
			id <- getId(call)
		} yield {
			val id_l = child_l.zipWithIndex.map(pair => id ++ List(pair._2 + 1))
			copy(
				frontier_l = frontier_l - call ++ child_l,
				parent_m = parent_m ++ child_l.map(_ -> call),
				children_m = children_m + (call -> child_l),
				idToCall_m = idToCall_m ++ (id_l zip child_l),
				callToId_m = callToId_m ++ (child_l zip id_l)
			)
		}
	}
	
	def setCallExpandResult(call: Call, result: CallExpandResult): RqResult[CallTree] = {
		result match {
			case CallExpandResult_Children(child_l) => expand(call, child_l)
			case CallExpandResult_Inputs(input_l) =>
				RqSuccess(copy(
					callToInputs_m = callToInputs_m + (call -> input_l)
				))
		}
	}
	
	def getLeafs: List[Call] = {
		// TODO: annotate as recursive
		def step(l: List[Call]): List[Call] = {
			l.flatMap { call =>
				children_m.get(call) match {
					case None => List(call)
					case Some(Nil) => List(call)
					case Some(child_l) => step(child_l)
				}
			}
		}
		step(top_l)
	}
}

object CallTree {
	def apply(top_l: List[Call]): CallTree = {
		val callToId_l = top_l.zipWithIndex.map(pair => (pair._1, List(pair._2 + 1)))
		CallTree(
			top_l = top_l,
			frontier_l = top_l.toSet,
			parent_m = Map(),
			children_m = Map(),
			idToCall_m = callToId_l.map(_.swap).toMap,
			callToId_m = callToId_l.toMap,
			callToInputs_m = Map(),
			callToVariables_m = Map()
		)
	}
	
	def expandTree(cs: CommandSet, tree: CallTree): RqResult[CallTree] = {
		tree.frontier_l.toList.foldLeft(RqSuccess(tree) : RqResult[CallTree]) { (tree_?, call) =>
			tree_?.flatMap(tree => {
				if (cs.nameToActionHandler_m.contains(call.name)) {
					RqSuccess(tree.copy(frontier_l = tree.frontier_l - call))
				}
				else if (cs.nameToMethods_m.contains(call.name)) {
					val method_l = cs.nameToMethods_m(call.name)
					for {
						result <- expandTask(call, method_l, tree.callToVariables_m.getOrElse(call, Map()))
						tree1 <- tree.setCallExpandResult(call, result)
					} yield tree1
				}
				else {
					RqError(s"Unknown command `${call.name}`")
				}
			})
		}
		//tree.frontier_l.toList.map(cmd => cmd -> expandCommand(cmd))
	}

	def getOperatorInfo(
		cs: CommandSet,
		tree: CallTree,
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		val call_l = tree.getLeafs
		RqResult.mapAll(call_l)(call => {
			for {
				id <- tree.getId(call)
				handler <- RsResult.from(cs.nameToActionHandler_m.get(call.name), s"Command `${call.name}` is not an action")
				paramName_l = handler.getActionParamNames
				jsval_l <- getParams(paramName_l, call.args)
				paramToJsval_l = paramName_l zip jsval_l
				planInfo <- handler.getOperatorInfo(id, paramToJsval_l, eb, state0)
			} yield planInfo
		}).map(_.flatten)
	}
	
	/*
	def expandCall(cs: CommandSet, tree: CallTree, call: Call): RqResult[Call] = {
		cs.nameToHandler_m.get(call.name) match {
			// Unknown calls become unknown actions for a human operator to interpret
			case None =>
				val cmd = UnknownAction(call)
				RqSuccess(cmd, List(s"unrecognized command `${call.name}`"))
			
			// Call has the name of a known command
			case Some(handler) =>
				val argName_l = handler.getArgNames
				for {
					jsval_l <- getParams(argName_l, call.args)
					cmd <- handler.expandCall(jsval_l)
				} yield cmd
		}
	}
	*/
	
	private def getParams(
		argName_l: List[String],
		nameToVal_l: List[(Option[String], JsValue)]
	): RqResult[List[JsValue]] = {
		val jsval_l = nameToVal_l.collect({case (None, jsval) => jsval})
		val nameToVal2_l: List[(String, JsValue)] = nameToVal_l.collect({case (Some(name), jsval) => (name, jsval)})
		val nameToVals_m: Map[String, List[JsValue]] = nameToVal2_l.groupBy(_._1).mapValues(_.map(_._2))
		// Make sure no parameter is assigned to more than one time
		val multiple_l = nameToVals_m.toList.filter(pair => pair._2.size > 1)
		if (!multiple_l.isEmpty)
			return RqError(multiple_l.map(pair => s"Call supplied multiple values for parameter `${pair._1}`: ${pair._2}"))
			
		def doit(
			argName_l: List[String],
			jsval_l: List[JsValue],
			nameToVal_m: Map[String, JsValue],
			acc_r: List[JsValue]
		): RqResult[List[JsValue]] = {
			argName_l match {
				case Nil =>
					val warning0_l = jsval_l.map(v => s"Extra argument: $v") ++ nameToVal_m.toList.map(pair => s"Extra argument: ${pair._1} = ${pair._2}")
					val warning_l = warning0_l match {
						case Nil => Nil
						case l => s"Extra arguments in ${nameToVal_l}" :: warning0_l
					}
					RsSuccess(acc_r.reverse, warning_l)
				case name :: argName_l_~ =>
					// Check whether named parameter is provided
					nameToVal_m.get(name) match {
						case Some(jsval) =>
							val nameToVal_m_~ = nameToVal_m - name
							doit(argName_l_~, jsval_l, nameToVal_m_~, jsval :: acc_r)
						case None =>
							jsval_l match {
								// Use unnamed parameter
								case jsval :: jsval_l_~ =>
									doit(argName_l_~, jsval_l_~, nameToVal_m, jsval :: acc_r)
								// Else parameter value is blank
								case Nil =>
									doit(argName_l_~, jsval_l, nameToVal_m, JsNull :: acc_r)
							}
					}
			}
		}

		val nameToVal_m: Map[String, JsValue] = nameToVals_m.mapValues(_.head)
		doit(argName_l, jsval_l, nameToVal_m, Nil)
	}
	
	def expandTask(
		call: Call,
		method_l: List[Call => RqResult[Call]],
		variable_m: Map[String, String]
	): RqResult[CallExpandResult] = {
		method_l match {
			case Nil =>
				// TODO: Instead of returning an error, create an `unknown` call, sort of like this idea:
				//val cmd = UnknownAction(call)
				//RqSuccess(cmd, List(s"unrecognized command `${call.name}`"))
				RqError(s"No methods for task `${call.name}`")
			// If only one method is available, go ahead and use it
			case method :: Nil =>
				for {
					call2 <- method(call)
				} yield CallExpandResult_Children(List(call2))
			// If multiple methods are available:
			case _ =>
				val call_l_? = RsResult.toResultOfList(method_l.map(method => method(call)))
				variable_m.get("method") match {
					// If no method has been selected yet
					case None =>
						// Return a list of possible method names
						for {
							call_l <- call_l_?
						} yield CallExpandResult_Inputs(List("method" -> call_l.map(_.name)))
					case Some(name) =>
						// The selected method will be the child call
						for {
							call_l <- call_l_?
							call2 <- RsResult.from(call_l.find(_.name == name), s"For task `${call.name}`, the selected method `${name}` is not one of the available methods")
						} yield CallExpandResult_Children(List(call2))
				}
		}
	}

	def expandAction(
		cs: CommandSet,
		id: List[Int],
		call: Call,
		variable_m: Map[String, String]
	): RqResult[CallExpandResult] = {
		RqSuccess(CallExpandResult_Children(Nil))
	}
	
	//def createPartialPlan(planInfo_l: List[ActionPlanInfo], problem: strips.Problem): RqResult[PartialPlan] = {
		
	//}
}
*/