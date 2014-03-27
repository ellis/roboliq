package roboliq.plan

import spray.json._
import roboliq.core._
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import aiplan.strips2.Strips.Literal
import aiplan.strips2.PartialPlan


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
	def getDomainOperator(id: List[Int]): RqResult[aiplan.strips2.Strips.Operator]
	def getProblemParamMap(id: List[Int], jsval_l: List[JsValue]): RqResult[Map[String, String]]
	//def getOperators(id: List[Int], jsval_l: List[JsValue], )
}

case class UnknownAction(
	call: Call
) extends Action {
	def getDomainOperator(id: List[Int]): RqResult[aiplan.strips2.Strips.Operator] = {
		RqSuccess(aiplan.strips2.Strips.Operator(
			name = "action_"+id.mkString("_"),
			paramName_l = Nil,
			paramTyp_l = Nil,
			preconds = aiplan.strips2.Strips.Literals.empty,
			effects = aiplan.strips2.Strips.Literals.empty
		))
	}

	def getProblemParamMap(id: List[Int], jsval_l: List[JsValue]): RqResult[Map[String, String]] = {
		RqSuccess(Map())
	}
}
*/

case class ActionPlanInfo(
	domainOperator: Strips.Operator,
	problemAction: Strips.Operator,
	problemObjects: List[(String, String)] = Nil,
	problemState: List[Strips.Atom] = Nil
)

trait ActionHandler {
	def getSignature: Strips.Signature
	
	def getActionPlanInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)]
	): RqResult[ActionPlanInfo]
}

class ActionHandler_ShakePlate extends ActionHandler {
	def getSignature: Strips.Signature = new Strips.Signature(
		name = "shakePlate",
		paramName_l = List("agent", "device", "program", "object", "site"),
		paramTyp_l = List("agent", "shaker", "shakerProgram", "labware", "site")
	)
	
	def getActionPlanInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)]
	): RqResult[ActionPlanInfo] = {
		val domainOperator = Strips.Operator(
			name = "action_"+id.mkString("_"),
			paramName_l = List("?agent", "?device", "?program", "?labware", "?model", "?site"),
			paramTyp_l = List("agent", "shaker", "shakerProgram", "labware", "model", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device"),
				Strips.Literal(Strips.Atom("device-can-site", List("?device", "?site")), true),
				Strips.Literal(Strips.Atom("model", List("?plate", "?model")), true),
				Strips.Literal(Strips.Atom("location", List("?plate", "?site")), true)
			)),
			effects = aiplan.strips2.Strips.Literals.empty
		)

		val m0 = paramToJsval_l.toMap
		val (programName, programObject_?) = m0.get("program") match {
			case None => ("?program", None)
			case Some(JsNull) => ("?program", None)
			case Some(JsString(s)) => (s, None)
			case Some(JsObject(obj)) =>
				val programName = id.mkString("_")+"_program"
				(programName, Some(programName -> "shakerProgram"))
			case x =>
				// TODO, should return an error here
				return RqError(s"Unexpected data for `program`: ${x}")
		}

		// Create planner objects if program was defined inside this command
		val problemObjects = List[Option[(String, String)]](
			programObject_?
		).flatten
		
		// TODO: require labware, otherwise the action doesn't make sense
		// TODO: possibly lookup labwareModel of labware
		val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
		val binding = Map(
			"?agent" -> m.getOrElse("agent", "?agent"),
			"?device" -> m.getOrElse("device", "?device"),
			"?program" -> programName,
			"?labware" -> m.getOrElse("object", "?labware"),
			//"?model" -> m.getOrElse("model", "?model"),
			"?site" -> m.getOrElse("site", "?site")
		)
		
		val programAction = domainOperator.bind(binding)
		
		println(s"getActionPlanInfo(${id}, ${paramToJsval_l}):")
		println(m0)
		println(programName, programObject_?)
		println(m)
		println(binding)
		
		RqSuccess(ActionPlanInfo(domainOperator, programAction, problemObjects))
	}
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

class CommandSet(
	val nameToActionHandler_m: Map[String, ActionHandler],
	val nameToMethods_m: Map[String, List[Call => RqResult[Call]]]
)

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
		callToId_m.get(cmd).asRs(s"getId: no ID registered for command: $cmd")
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

	def getActionPlanInfo(cs: CommandSet, tree: CallTree): RqResult[List[ActionPlanInfo]] = {
		val call_l = tree.getLeafs
		val x = call_l.map(call => {
			for {
				id <- tree.getId(call)
				handler <- cs.nameToActionHandler_m.get(call.name).asRs(s"Command `${call.name}` is not an action")
				argName_l = handler.getSignature.paramName_l
				jsval_l <- getParams(argName_l, call.args)
				paramToJsval_l = argName_l zip jsval_l
				planInfo <- handler.getActionPlanInfo(id, paramToJsval_l)
			} yield planInfo
			
		})
		for {
			planInfo_l <- RqResult.toResultOfList(x)
		} yield planInfo_l
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
					val warning_l = jsval_l.map(v => s"Extra argument: $v") ++ nameToVal_m.toList.map(pair => s"Extra argument: ${pair._1} = ${pair._2}")
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
							call2 <- call_l.find(_.name == name).asRs(s"For task `${call.name}`, the selected method `${name}` is not one of the available methods")
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
	
	def makeDomain(planInfo_l: List[ActionPlanInfo]): RqResult[Strips.Domain] = {
		val operator0_l = List[Strips.Operator](
			Strips.Operator(
				"moveLabware",
				List("?labware", "?site1", "?site2"),
				List("labware", "site", "site"),
				preconds = Strips.Literals(Unique(Strips.Literal(true, "location", "?labware", "?site1"))),
				effects = Strips.Literals(Unique(Strips.Literal(false, "location", "?labware", "?site1"), Strips.Literal(true, "location", "?labware", "?site2")))
			)
		)

		RqSuccess(Strips.Domain(
			type_l = List(
				"labware",
				"model",
				"site",
				"siteModel",
				
				"agent",
				"tecan",
				
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
			operator_l = operator0_l ++ planInfo_l.map(_.domainOperator)
		))
	}

	//def makeProblem(planInfo_l: List[ActionPlanInfo], domain: Strips.Domain): RqResult[Strips.Problem] = {
		
	//}
	
	//def makePartialPlan(planInfo_l: List[ActionPlanInfo], problem: Strips.Problem): RqResult[PartialPlan] = {
		
	//}
	
	def main(args: Array[String]) {
		val tecan_shakePlate_handler = new ActionHandler_ShakePlate
		def shakePlate_to_tecan_shakePlate(call: Call): RqResult[Call] = {
			RqSuccess(call.copy(name = "tecan_shakePlate"))
		}
		val cs = new CommandSet(
			nameToActionHandler_m = Map("tecan_shakePlate" -> tecan_shakePlate_handler),
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
			domain <- makeDomain(planInfo_l)
			_ = println(domain.toStripsText)
		} yield {
		}
		x match {
			case RqError(e, w) =>
				println("ERRORS: "+e)
				println("WARNINGS: "+w)
			case _ =>
		}
	}
}
