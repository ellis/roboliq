package roboliq.plan

import spray.json.JsValue
import roboliq.core._
import aiplan.strips2.Strips
import spray.json.JsNull
import aiplan.strips2.Unique
import aiplan.strips2.Strips.Literal


sealed trait Command

case class Call(
	name: String,
	args: List[(Option[String], JsValue)]
) extends Command

case class Task(
	call: Call,
	list: List[Call]
) extends Command

trait Procedure extends Command

trait Method extends Command

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

trait ActionHandler {
	def getDomainOperator(id: List[Int]): RqResult[aiplan.strips2.Strips.Operator]
	def getProblemParamMap(id: List[Int], jsval_l: List[JsValue]): RqResult[Map[String, String]]
}

class ActionHandlerAction(
	handler: ActionHandler
) extends Action {
	def getDomainOperator(id: List[Int]): RqResult[aiplan.strips2.Strips.Operator] =
		handler.getDomainOperator(id)
		
	def getProblemParamMap(id: List[Int], jsval_l: List[JsValue]): RqResult[Map[String, String]] =
		handler.getProblemParamMap(id, jsval_l)
}

class ActionHandler_ShakePlate extends ActionHandler {
	def getDomainOperator(id: List[Int]): RqResult[aiplan.strips2.Strips.Operator] = {
		RqSuccess(aiplan.strips2.Strips.Operator(
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
		))
	}	

	//def getProblemParamMap(id: List[Int], jsval_l: List[JsValue]): RqResult[Map[String, String]] =
		//handler.getProblemParamMap(id, jsval_l)
}

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

class Signature(val name: String, val paramName_l: List[String], val paramTyp_l: List[String]) {
	assert(paramTyp_l.length == paramName_l.length)
	
	def getSignatureString = name + (paramName_l zip paramTyp_l).map(pair => s"${pair._1}:${pair._2}").mkString("(", " ", ")")
	override def toString = getSignatureString
}

case class TaskSpec(
	signature: Signature
)

case class ProcedureSpec(
	signature: Signature,
	cmd_l: List[Call]
)

class CommandSet(
	val nameToHandler_m: Map[String, CommandHandler],
	val nameToArgs_m: Map[String, List[String]],
	val nameToTaskSpec_l: Map[String, TaskSpec],
	val nameToProcedureSpec_l: Map[String, ProcedureSpec]
)

case class CommandTree(
	val top_l: List[Call],
	val frontier_l: Set[Command],
	val parent_m: Map[Command, Command],
	val children_m: Map[Command, List[Command]],
	val idToCommand_m: Map[List[Int], Command],
	val commandToId_m: Map[Command, List[Int]],
	val taskToCall_m: Map[Task, Call]
) {
	def getId(cmd: Command): RqResult[List[Int]] = {
		commandToId_m.get(cmd).asRs(s"getId: no ID registered for command: $cmd")
	}
	
	def expand(cmd: Command, child_l: List[Command]): RqResult[CommandTree] = {
		for {
			_ <- RqResult.assert(frontier_l.contains(cmd), s"Tried to expand command that's not in frontier: $cmd")
			id <- getId(cmd)
		} yield {
			val id_l = child_l.zipWithIndex.map(pair => id ++ List(pair._2 + 1))
			copy(
				frontier_l = frontier_l - cmd ++ child_l,
				parent_m = parent_m ++ child_l.map(_ -> cmd),
				children_m = children_m + (cmd -> child_l),
				idToCommand_m = idToCommand_m ++ (id_l zip child_l),
				commandToId_m = commandToId_m ++ (child_l zip id_l)
			)
		}
	}
}

trait CommandHandler {
	def getArgNames: List[String]
	def expandCall(jsval_l: List[JsValue]): RqResult[Command] = {
		
	}
}

trait ProcedureHandler extends CommandHandler {
	def decompose(call: Call): RqResult[List[Command]]
}

object X {
	def expandTree(cs: CommandSet, tree: CommandTree) {
		tree.frontier_l.toList.foldLeft(RqSuccess(tree) : RqResult[CommandTree]) { (tree_?, cmd) =>
			tree_?.flatMap(tree => {
				cmd match {
					case x: Call =>
						for {
							child <- expandCall(cs, tree, x)
							tree1 <- tree.expand(x, List(child))
						} yield tree1
					case x: Task =>
						tree.taskToCall_m.get(x) match {
							case Some(call) => tree.expand(x, List(call))
							case None => RqSuccess(tree)
						}
					//case x: Procedure =>
					case x: Action => RqSuccess(tree.copy(frontier_l = tree.frontier_l - x))
					case x: Operator => RqSuccess(tree.copy(frontier_l = tree.frontier_l - x))
				}
			})
		}
		//tree.frontier_l.toList.map(cmd => cmd -> expandCommand(cmd))
	}

	
	def expandCall(cs: CommandSet, tree: CommandTree, call: Call): RqResult[Command] = {
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
				} yield {
					()
				}
		}
	}
	
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
	
	def expandTask(tree: CommandTree, task: Task): RqResult[Unit] = {
		
	}
	
	def decomposeTask(task: String, taskToCommand_m: Map[String, List[Command]]): List[Command] = {
		taskToCommand_m.get(task) match {
			
		}
	}
	
	def registerHandler(handler: CommandHandler) {
		
	}
}
