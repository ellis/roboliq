package roboliq.processor2

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer
import scala.collection._
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.runtime.{universe => ru}
import scalaz._
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsNull
import spray.json.JsonParser
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.math.Ordering
import roboliq.core._
import RqPimper._
import spray.json.JsNumber


/**
 * A command is a JSON object that represents a command and its parameters.
 * A command handler produces a Computation which requires the command's 
 * json parameters and objects in the database or programmatically generated objects,
 * and it returns a list of computations, events, commands, and tokens.  
 */

case class KeyClass(key: TKP, clazz: ru.Type, time: List[Int] = Nil) {
	def changeKey(key: String): KeyClass =
		this.copy(key = this.key.copy(key = key))
	def isJsValue: Boolean =
		(clazz =:= ru.typeOf[JsValue])
	def changeClassToJsValue: KeyClass =
		this.copy(clazz = ru.typeOf[JsValue])
	def changeTime(time: List[Int]): KeyClass =
		this.copy(time = time)
	def id: String =
		s"${key.id}<${getSimplifiedClassName}>" + (if (time.isEmpty) "" else time.mkString("@", ".", ""))
	def getSimplifiedClassName: String = {
		val s0 = clazz.typeSymbol.name.toString
		val iPeriod = s0.lastIndexOf('.')
		if (iPeriod >= 0) s0.substring(iPeriod + 1)
		else s0
	}
	
	override def toString = id
}

case class KeyClassOpt(
	kc: KeyClass,
	opt: Boolean = false,
	conversion_? : Option[(List[Object] => ConversionResult, RqArgs)] = None
) {
	def changeKey(key: String): KeyClassOpt =
		this.copy(kc = kc.changeKey(key))
	def changeClassToJsValue: KeyClassOpt =
		this.copy(kc = kc.changeClassToJsValue)
	def changeTime(time: List[Int]): KeyClassOpt =
		this.copy(kc = kc.changeTime(time))
}
//case class ComputationItem_Entity(key: TKP, jsval: JsValue) extends ComputationItem
//case class ComputationItem_Object(kc: KeyClass, obj: Object) extends ComputationItem

/*case class ComputationSpec(
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationItem]]
)

case class Computation(
	id_r: List[Int],
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationItem]]
) {
	val id: List[Int] = id_r.reverse
}

case class ComputationNode(
	computation: Computation
)*/

class Event
trait Token

case class Token_Comment(s: String) extends Token

/*case class DataKey(time: List[Int], table: String, key: String, path: List[String]) {
	def idWithoutTime = (s"$table[$key]" :: path).mkString(".")
}*/

class ProcessorData(
	handler_l: List[CommandHandler]
) {
	private val handler_m: Map[String, CommandHandler] = handler_l.flatMap(handler => handler.cmd_l.map(_ -> handler)).toMap
	
	val db = new DataBase
	// Top levels command nodes
	var cmd1_l: List[Node] = Nil
	// Conversion nodes
	val kcNode_m = new HashMap[KeyClass, Node_Conversion]
	val state_m = new HashMap[Node, NodeState]
	//val status_m = new HashMap[Node, Status.Value]
	//val dep_m: MultiMap[KeyClass, Node] = new HashMap[KeyClass, mutable.Set[Node]] with MultiMap[KeyClass, Node]
	//val children_m = new HashMap[Node, List[Node]]
	//val result_m = new HashMap[Node, RqResult[_]]
	//val entity_m = new HashMap[KeyClass, RqFunctionArgs]
	//val fnCache_m = new HashMap[RqFunctionInputs, RqReturn]
	//val argsCache_m = new HashMap[RqArgs, RqInputs]
	val cache_m = new HashMap[KeyClass, Object]
	//val entityChanged_l = mutable.Set[KeyClass]()
	// List of nodes for which we want to force a check of whether inputs are ready.
	//val nodeCheck_l = mutable.Set[Node]()
	//val inputs_m = new HashMap[Node, RqResult[List[Object]]]
	//val entityStatus_m = new HashMap[KeyClass, Status.Value]
	//val entityMessages_m = new HashMap[KeyClass, RqResult[_]]
	val token_m = new HashMap[List[Int], Token]
	val lookupMessage_m = new HashMap[String, RqResult[Unit]]
	val internalMessage_l = new ArrayBuffer[RqResult[Unit]]
	//val computationMessage_m = new HashMap[List[Int], RqResult[Unit]]
	//val conversionMessage_m = new HashMap[KeyClass, RqResult[Unit]]
	
	val conversion_m = new HashMap[ru.Type, (JsValue) => ConversionResult]
	conversion_m(ru.typeOf[String]) = Conversions.asString
	conversion_m(ru.typeOf[Integer]) = Conversions.asInteger
	conversion_m(ru.typeOf[PlateModel]) = Conversions.asPlateModel
	conversion_m(ru.typeOf[PlateLocation]) = Conversions.asPlateLocation
	conversion_m(ru.typeOf[Plate]) = Conversions.asPlate
	conversion_m(ru.typeOf[PlateState]) = Conversions.asPlateState
	conversion_m(ru.typeOf[List[String]]) = Conversions.asStringList
	
	def setCommands(cmd_l: List[JsObject]) {
		cmd1_l = handleComputationItems(None, cmd_l.map(js => ComputationItem_Command(js)))
		registerNodes(cmd1_l)
	}

	private def setComputationResult(node: Node, result: scala.util.Try[ComputationResult]): Status.Value = {
		println("setComputationResult()")
		val state = state_m(node)
		val child_l = result match {
			case scala.util.Success(RqSuccess(l, _)) =>
				handleComputationItems(Some(node), l)
			case _ => Nil
		}
		registerNodes(child_l)
		state.setChildren(child_l)
		state.setFunctionResult(result)
		state.status
	}
	
	private def handleComputationItems(parent_? : Option[Node], l: List[ComputationItem]): List[Node] = {
		val idParent = parent_?.map(_.id).getOrElse(Nil)
		l.zipWithIndex.flatMap { pair => 
			val (r, index0) = pair
			val index = index0 + 1
			r match {
				case ComputationItem_Command(cmd) =>
					//val idCmd = node_?.map(_.id).getOrElse(Nil) ++ List(index)
					//val idCmd_s = getIdString(idCmd)
					//cmdToJs_m(idCmd) = cmd
					//setEntity(idEntity, cmd)

					//val result = ComputationItem_Computation(Nil, (_: List[Object]) => fn)
					val node = Node_Command(parent_?, index, cmd)
					val key = TKP("cmd", node.label, Nil)
					setEntity(key, Nil, cmd)
					Some(node)
				case result: ComputationItem_Computation =>
					val entity2_l = concretizeArgs(result.entity_l, parent_?, index)
					//val result2 = result.copy(entity_l = entity2_l)
					Some(Node_Computation(parent_?, index, entity2_l, result.fn))
				case ComputationItem_Token(token) =>
					val id = parent_?.map(_.id).getOrElse(Nil) ++ List(index)
					//println("token: "+Node_Token(node, index + 1, token))
					token_m(id) = token
					None
				/*case ComputationItem_Entity(id, jsval) =>
					setEntity(id, jsval)
					//new Node_Result(node, index, r)
					None*/
				case _ =>
					//new Node_Result(node, index, r)
					None
			}
		}
	}
	
	private def concretizeArgs(kco_l: List[KeyClassOpt], parent_? : Option[Node], index: Int): List[KeyClassOpt] = {
		val keyCmd = findCommandParent(parent_?).map(_.label).getOrElse("")
		val idPrefix = (parent_?.map(_.id).getOrElse(Nil) ++ List(index)).mkString("", ".", "#")
		val time = parent_?.map(_.time).getOrElse(Nil)
		kco_l.zipWithIndex.map(pair => {
			val (kco0, i) = pair
			// Set time if this is a state variables
			val kco = if (kco0.kc.key.table.endsWith("State")) kco0.changeTime(time) else kco0
			// Substitute in full path for command parameters starting with '$'
			if (kco.kc.key.key == "$") kco.changeKey(keyCmd)
			else if (kco.kc.key.key == "#") kco.changeKey(idPrefix+(i+1))
			else kco
		})
	}
	
	private def findCommandParent(node_? : Option[Node]): Option[Node] = {
		node_? match {
			case None => None
			case Some(node: Node_Command) => Some(node)
			case Some(node) => findCommandParent(node.parent_?)
		}
	}
	
	private def setConversionResult(node: Node_Conversion, result: scala.util.Try[ConversionResult]): Status.Value = {
		val state = state_m(node)
		val child_l = result match {
			case scala.util.Success(RqSuccess(l, _)) =>
				l.zipWithIndex.flatMap { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ConversionItem_Conversion(input_l, fn) =>
							val input2_l = concretizeArgs(input_l, Some(node), index)
							Some(Node_Conversion(Some(node), None, Some(index), node.time, node.kc, input2_l, fn))
						case ConversionItem_Object(obj) =>
							setEntityObj(node.kc, obj)
							None
					}
				}
			case _ => Nil
		}
		registerNodes(child_l)
		state.setChildren(child_l)
		state.setFunctionResult(result)
		state.status
	}
	
	private def registerNodes(node_l: List[Node]) {
		node_l.foreach(registerNode)
	}
	
	private def registerNode(node: Node) {
		val state = new NodeState(node)
		state_m(node) = state
		makeConversionNodesForInputs(node)
	}
	
	private def makeConversionNodesForInputs(node: Node): List[Node] = {
		// Try to add missing conversions for inputs which are not JsValues 
		val l = node.input_l.filterNot(kco => kco.kc.isJsValue || kcNode_m.contains(kco.kc)).flatMap(kco => {
			makeConversionNodesForInput(node, kco)
		})
		// Register the conversion nodes
		l.foreach(node => kcNode_m(node.kc) = node)
		println("node.input_l: "+node.input_l)
		println("l: "+l)
		registerNodes(l)
		l
	}
	
	private def makeConversionNodesForInput(node: Node, kco: KeyClassOpt): List[Node_Conversion] = {
		if (kco.kc.isJsValue)
			return Nil
		
		val kc = kco.kc
		kco.conversion_? match {
			// If a user-supplied conversion function was supplied:
			case Some(fnargs) =>
				val input2_l = concretizeArgs(fnargs._2, Some(node), 0)
				List(Node_Conversion(None, Some(kc.id), None, node.time, kc, input2_l, fnargs._1))
			// Otherwise we'll 
			case None =>
				if (kc.clazz.typeSymbol.name.decoded == "Option") {
					// type parameter of option (e.g, if Option[String], clazz2 will be String)
					val clazz2 = kc.clazz.asInstanceOf[ru.TypeRefApi].args.head
					val kc2 = kc.copy(clazz = clazz2)
					val fn = (l: List[Object]) => l match {
						case List(o) =>
							RqSuccess(List(ConversionItem_Object(o)))
					}
					val kco2 = KeyClassOpt(kc2, true)
					Node_Conversion(None, Some(kc.id), None, node.time, kc, List(kco2), fn) ::
						makeConversionNodesForInput(node, kco2)
				}
				else {
					conversion_m.get(kc.clazz) match {
						case Some(conversion) =>
							val kc0 = kc.changeClassToJsValue
							val fn = (l: List[Object]) => l match {
								case List(jsval: JsValue) =>
									conversion(jsval)
							}
							List(Node_Conversion(None, Some(kc.id), None, node.time, kc, List(KeyClassOpt(kc0, false)), fn))
						case None =>
							// FIXME: should put this message in a map so it only shows up once
							internalMessage_l += RqError[Unit]("No converter registered for "+node+" "+kco)
							Nil
					}
				}
		}
	}
	
	def setEntity(key: TKP, time: List[Int], jsval: JsValue) {
		db.set(key, time, jsval)
		val kc = KeyClass(key, ru.typeOf[JsValue])
		registerEntity(kc)
		//val jsChanged_l = db.popChanges.map(KeyClass(_, ru.typeOf[JsValue]))
		//entityChanged_l ++= jsChanged_l
		/*val changed_l = db.popChanges
		changed_l.foreach(tkp => {
			val kc = KeyClass(tkp, ru.typeOf[JsValue])
			// Queue the computations for which all inputs are available 
			dep_m.get(kc).map(_.foreach(updateComputationStatus))
		})*/
	}
	
	private def registerEntity(kc: KeyClass) {
		//entityStatus_m(kc) = Status.Success
	}
	
	def setEntityObj(kc: KeyClass, obj: Object) {
		assert(kc.clazz != ru.typeOf[JsValue])
		cache_m(kc) = obj
		//entityChanged_l += kc
		registerEntity(kc)
		// Queue the computations for which all inputs are available 
		//dep_m.get(kc).map(_.foreach(updateComputationStatus))
	}

	/*
	// For each of the node's inputs, add the node to the input's dependency list
	private def addDependencies(node: Node) {
		node.input_l.foreach(kco => {
			val kc = kco.kc
			dep_m.addBinding(kc, node)
			if (kc.clazz == ru.typeOf[JsValue])
				db.addWatch(kc.key)

			// Schedule lookups and create conversion nodes
			for (kco <- node.input_l) {
				val kc = kco.kc
				val kc0 = kc.copy(clazz = ru.typeOf[JsValue])
				
				// JsValue lookup
				if (!entityStatus_m.contains(kc0))
					entityStatus_m(kc0) = Status.Ready
					
				// Conversion
				if (!entityStatus_m.contains(kc)) {
					assert(!kcNode_m.contains(kc))
					entityStatus_m(kc) = Status.NotReady
					conversion_m.get(kc.clazz) match {
						case Some(conversion) =>
							val fn = (l: List[Object]) => l match {
								case List(jsval: JsValue) =>
									conversion(jsval)
							}
							val node = new Node_Conversion(None, Some(kc.toString), 0, kc, List(KeyClassOpt(kc0)), fn)
							kcNode_m(kc) = node
							registerNode(node)
						case None =>
							conversionMessage_m(kc) = RqError[Unit]("No converter registered for "+kc.clazz)
					}
				}
			}
		})
	}
	*/
	
	/*
	private def updateComputationStatus(node: Node) {
		println("entityStatus_m: "+entityStatus_m)
		val b = node.input_l.forall(entityStatus_m.get(_) == Some(Status.Success))
		if (b) {
			if (status_m.get(node) != Some(Status.Success)) {
				status_m(node) = Status.Ready
				/*val input_l = node.input_l.map(entity_m)
				node match {
					case n: Node_Command =>					
						workerRouter ! ActorMessage_CommandLookup(n)
					case n: Node_Computation =>
						workerRouter ! ActorMessage_ComputationInput(n, input_l)
					case n: Node_Conversion =>
						workerRouter ! ActorMessage_ConversionInput(n, input_l)
				}*/
			}
		}
		else {
			status_m(node) = Status.NotReady
		}
	}
	*/
	
	// Match "tbl[key]"
	private val Rx1 = """^([a-zA-Z]+)\[([0-9.]+)\]$""".r
	// Match "tbl[key].field"
	private val Rx2 = """^([a-zA-Z]+)\[([0-9.]+)\]\.(.+)$""".r
	
	// REFACTOR: turn entity lookups into computations, somehow
	def run() {
		while (runStep()) { }
		//makeMessagesForMissingInputs()
	}
	
	private def runStep(): Boolean = {
		val state0_m = state_m.toMap
		
		// Get all KeyClassOpt from all nodes
		val kco_l = state0_m.keySet.flatMap(_.input_l)
		// Find whether KeyClass is optional for all nodes
		val opt_m = kco_l.groupBy(_.kc).mapValues(l => l.find(_.opt == false).isEmpty)

		// TODO: at this point we should perhaps lookup the KeyClasses in opt_m

		// Try to get values for all kcos
		val kcToValue_m = opt_m.keys.toList.map(kc => kc -> getEntity(kc)).toMap
		val kcoToValue_m = kco_l.toList.map(kco => {
			val value0 = kcToValue_m(kco.kc)
			val value = {
				if (kco.opt) value0.map(Some.apply).orElse(RqSuccess(None))
				else value0
			}
			kco -> value
		}).toMap
		
		// Update status for all nodes
		state0_m.values.foreach(_.updateStatus(kcoToValue_m.apply _))
		
		/*
		cmd[1].id: JsValue = P1
		cmd[1].id: String = P1
		plate[P1] ...
		param[1#1]: Plate = fn(cmd[1].id: JsValue) 
		*/
		
		println()
		println("runStep")
		println("=======")
		println()
		println("Entities")
		println("--------")
		// First print JsValue entities, then others
		val (e1, e2) = kcToValue_m.toList.partition(_._1.isJsValue)
		//val (e1, e2) = kcoToValue_m.toList.partition(_._1.kc.clazz == ru.typeOf[JsValue])
		def getEntityString(pair: (KeyClass, RqResult[Object])): String = {
			val (kc, result) = pair
			(if (opt_m(kc)) "*" else "") + ": " + result.getOrElse("...")
		}
		e1.toList.sortBy(_._1.key.id).map(pair => pair._1.key.id + getEntityString(pair)).foreach(println)
		println()
		e2.toList.sortBy(_._1.key.id).map(pair => pair._1.id + getEntityString(pair)).foreach(println)
		println()
		println("Nodes")
		println("-----")
		state0_m.values.toList.sortBy(_.node.label).map(state => state.node.label + ": " + state.status).foreach(println)

		val pending_l = makePendingComputationList
		pending_l.foreach(state => runComputation(state.node, kcoToValue_m))

		// Add conversion nodes
		var bConversionAdded = false
		/*
		// Add "parameter transformation" conversion nodes
		kco_l.filter(_.conversion_?.isDefined).foreach(kco => {
			val fnargs = kco.conversion_?.get
			val kc = kco.kc
			if (!kcNode_m.contains(kc)) {
				val node = new Node_Conversion(None, Some(kc.toString), 0, kc, fnargs._2, fnargs._1)
				kcNode_m(kc) = node
				registerNode(node)
				bConversionAdded = true
			}
		})
		// Add more conversion nodes
		opt_m.foreach(pair => {
			val (kc, opt) = pair
			val kc0 = kc.changeClassToJsValue
			if (kc != kc0 && !kcNode_m.contains(kc)) {
				conversion_m.get(kc.clazz) match {
					case Some(conversion) =>
						val fn = (l: List[Object]) => l match {
							case List(jsval: JsValue) =>
								conversion(jsval)
						}
						val node = new Node_Conversion(None, Some(kc.toString), 0, kc, List(KeyClassOpt(kc0, opt)), fn)
						kcNode_m(kc) = node
						registerNode(node)
						bConversionAdded = true
					case None =>
						internalMessage_l += RqError[Unit]("No converter registered for "+kc.clazz)
				}
			}
		})
		*/
		
		println()
		println("Computations")
		println("------------")
		pending_l.map(state => state.node.label + ": " + state.status).foreach(println)
		println()
		// TODO: show added conversions
		
		!pending_l.isEmpty || bConversionAdded 
	}

	/**
	 * Return all ready nodes which don't depend on state,
	 * plus the next node which depends on state after exclusely successful nodes.
	 */
	private def makePendingComputationList: List[NodeState] = {
		val order_l = state_m.toList.sortBy(_._1.id)(ListIntOrdering).map(_._2).dropWhile(_.status == Status.Success)
		order_l match {
			case Nil => Nil
			case next :: rest_l =>
				val ready_l = rest_l.filter(state => {
					// Node is ready
					state.status == Status.Ready &&
					// And it doesn't depend on state
					state.node.input_l.forall(kco => !kco.kc.key.table.endsWith("State"))
				})
				// Take next node if it's ready (regardless of whether it depends on state)
				if (next.status == Status.Ready) next :: ready_l
				else ready_l
		}
	}

	/*
	private def runComputations(node_l: List[Node]) {
		println("run")
		printNodesAndStatus()
		if (!node_l.isEmpty) {
			val status_l = node_l.map(runComputation)
			println("status_l: "+status_l)
			//println("status_m: "+status_m)
			runComputations(makePendingComputationList)
		}
	}
	*/
	
	// REFACTOR: This should return the results, and let the results be processed in another function
	//  That way we could run the computations in parallel.
	private def runComputation(node: Node, kcoToValue_m: Map[KeyClassOpt, RqResult[Object]]): Status.Value = {
		//println(s"try node ${node.name}")
		RqResult.toResultOfList(node.input_l.map(kcoToValue_m)) match {
			case RqSuccess(input_l, _) =>
				node match {
					case n: Node_Command =>
						val fn = createCommandFn(n)
						val f = scala.util.Try(fn(Nil))// future { fn(Nil) }
						f match {
							case tryResult => setComputationResult(n, tryResult)
						}
					case n: Node_Computation =>
						val f = scala.util.Try { n.fn(input_l) }
						f match {
							case tryResult => setComputationResult(n, tryResult)
						}
					case n: Node_Conversion =>
						val f = scala.util.Try { n.fn(input_l) }
						f match {
							case tryResult => setConversionResult(n, tryResult)
						}
				}
			case _ =>
				assert(false)
				Status.Error
		}
	}
	
	private def getEntity(kc: KeyClass): RqResult[Object] = {
		if (kc.clazz == ru.typeOf[JsValue]) {
			db.get(kc.key)
		}
		else {
			cache_m.get(kc).asRq(s"object not found `$kc`")
		}
	}
	
	private def getEntity(kco: KeyClassOpt): RqResult[Object] = {
		val res = getEntity(kco.kc)
		if (kco.opt)
			res map(Some(_)) orElse RqSuccess(None)
		else
			res
	}
	
	private def createCommandFn(node: Node_Command): (List[Object] => ComputationResult) = {
		val cmd_? : Option[String] = node.cmd.fields.get("cmd").flatMap(_ match {
			case JsString(s) => Some(s)
			case _ => None
		})
		cmd_? match {
			case None => _ => RqError("missing field `cmd`")
			case Some(name) =>
				handler_m.get(name) match {
					case None => _ => RqError(s"no handler found for `cmd = ${name}`")
					case Some(handler) => _ => handler.getResult
				}
		}
	}
	
	private def makeMessagesForMissingInputs() {
		/*entityStatus_m.toList.filterNot(_._2 == Status.Success).map(pair => {
			val (kc, status) = pair
			conversionMessage_m(kc) =
				conversionMessage_m.getOrElse(kc, RqResult.zero) flatMap {_ => RqError[Unit](s"missing entity `${kc.key.id}` of class `${kc.clazz}`")}
		})*/
	}
	
	def getCommandNodeList(): List[Node] = {
		def step(node: Node): List[Node] = {
			val state = state_m(node)
			node :: state.child_l.flatMap(step)
		}
		cmd1_l.flatMap(step)
	}
	
	private def getConversionNodeList(node: Node_Conversion): List[Node_Conversion] = {
		val state = state_m(node)
		node :: (state.child_l.collect({case n: Node_Conversion => n}).flatMap(getConversionNodeList))
	}
	
	def getConversionNodeList(): List[Node_Conversion] = {
		kcNode_m.toList.sortBy(_._1.toString).flatMap(pair => getConversionNodeList(pair._2))
	}
	
	def getTokenList(): List[(List[Int], Token)] = {
		token_m.toList.sortBy(_._1)(ListIntOrdering)
	}
	
	def getMessages(): List[String] = {
		def makeMessages(id: String, result: RqResult[_]): List[String] = {
			result.getErrors.map(id+": ERROR: "+_) ++ result.getWarnings.map(id+": WARNING: "+_)
		}
		lookupMessage_m.toList.sortBy(_._1).flatMap(pair => makeMessages(pair._1, pair._2)) ++
		getConversionNodeList.map(state_m).sortBy(_.node.label).flatMap(state => makeMessages(state.node.label, state.result)) ++
		getCommandNodeList.map(state_m).sortBy(_.node.id)(ListIntOrdering).flatMap(state => makeMessages(state.node.label, state.result)) ++
		internalMessage_l.map(_.toString)
	}
	
	//def getIdString(node: Node): String = getIdString(node.id)
	def getIdString(id: List[Int]): String = id.mkString(".")

	/*
	def printNodesAndStatus() {
		getConversionNodeList().foreach(node => {
			println(node.label+" "+status_m.getOrElse(node, "<missing>"))
		})
		getCommandNodeList().foreach(_ match {
			case node: Node_Command =>
				val class_s = node.getClass().getName().dropWhile(_ != '_').tail
				println(node.label+" "+class_s+" "+status_m.getOrElse(node, "<missing>")+" "+node.cmd)
			case node: Node =>
				//val prefix = "  " * (node.id.length - 1)
				//println(prefix+node)
				val class_s = node.getClass().getName().dropWhile(_ != '_').tail
				println(node.label+" "+class_s+" "+status_m.getOrElse(node, "<missing>"))
			case _ =>
		})
	}
	*/
	
	/*def compComputation(cn1: Computation, cn2: Computation): Boolean =
		compId(cn1.id, cn2.id)
	def compMessage(cn1: (List[Int], List[String]), cn2: (List[Int], List[String])): Boolean =
		compId(cn1._1, cn2._1)
	def compId(l1: List[Int], l2: List[Int]): Boolean = {
		if (l2.isEmpty) true
		else if (l1.isEmpty) true
		else {
			val n = l1.head - l2.head
			if (n < 0) true
			else if (n > 0) false
			else compId(l1.tail, l2.tail)
		}
	}*/
	
	implicit val NodeOrdering = new Ordering[Node] {
		def compare(a: Node, b: Node): Int = {
			ListIntOrdering.compare(a.id, b.id)
		}
	}
}

class PrintCommandHandler extends CommandHandler {
	val cmd_l = List[String]("print") 
	
	def getResult =
		handlerRequire (as[String]('text)) { (text) =>
			handlerReturn(Token_Comment(text))
		}
}

class Print2CommandHandler extends CommandHandler {
	val cmd_l = List[String]("print2") 
	
	def getResult =
		handlerRequire (as[Integer]('number)) { (n) =>
			handlerReturn(Token_Comment(n.toString))
		}
}

/**
 * Steps to test:
 * 
 * - nodes with dependencies, but no children
 * - caching errors and producing a sensible list of errors and warnings
 * - cache transformations of JsValues (e.g. Plate objects)
 * - deal with states
 */
object ApplicativeMain2 extends App {
	val cmd1 = JsonParser("""{ "cmd": "print", "text": "Hello, World!" }""").asJsObject
	val cmd2 = JsonParser("""{ "cmd": "print2", "number": 3 }""").asJsObject
	val cmd3 = JsonParser("""{ "cmd": "movePlate", "plate": "P1", "dest": "cooled2", "deviceId": "ROMA2" }""").asJsObject
	
	val h1 = new PrintCommandHandler
	val h2 = new Print2CommandHandler
	val h3 = new MovePlateHandler

	val p = new ProcessorData(List(h1, h2, h3))
	
	p.setEntity(TKP("plateModel", "PCR", Nil), Nil, JsonParser("""{ "id": "PCR", "rows": 8, "cols": 12, "wellVolume": "100ul" }"""))
	p.setEntity(TKP("plateLocation", "cooled1", Nil), Nil, JsonParser("""{ "id": "cooled1", "plateModels": ["PCR"], "cooled": true }"""))
	p.setEntity(TKP("plateLocation", "cooled2", Nil), Nil, JsonParser("""{ "id": "cooled2", "plateModels": ["PCR"], "cooled": true }"""))
	p.setEntity(TKP("plate", "P1", Nil), Nil, JsonParser("""{ "id": "P1", "idModel": "PCR" }"""))
	p.setEntity(TKP("plateState", "P1", Nil), Nil, JsonParser("""{ "id": "P1", "location": "cooled1" }"""))
	p.setCommands(List(cmd1, cmd2, cmd3))
	
	//println(p.db.get(TKP("plate", "P1", Nil)))
	p.run()

	/*def compComputation(cn1: Computation, cn2: Computation): Boolean =
		compId(cn1.id, cn2.id)
	def compMessage(cn1: (List[Int], List[String]), cn2: (List[Int], List[String])): Boolean =
		compId(cn1._1, cn2._1)
	def compId(l1: List[Int], l2: List[Int]): Boolean = {
		if (l2.isEmpty) true
		else if (l1.isEmpty) true
		else {
			val n = l1.head - l2.head
			if (n < 0) true
			else if (n > 0) false
			else compId(l1.tail, l2.tail)
		}
	}*/

	println()
	println("Computations:")
	p.getCommandNodeList.map( _ match {
		case node: Node_Command => p.getIdString(node.id)+": "+node.cmd
		case node: Node_Computation => p.getIdString(node.id)+": "+node.input_l
		case _ =>
	}).foreach(println)

	println()
	println("Entities:")
	println(p.db)
	//p.entity_m.toList.sortBy(_._1.id).foreach(pair => if (pair._1.clazz == ru.typeOf[JsValue]) println(pair._1.id+": "+pair._2))
	
	println()
	println("Conversions:")
	p.cache_m.toList.sortBy(_._1.key.toList)(ListStringOrdering).foreach(println)
	
	println()
	println("Tokens:")
	p.getTokenList./*map(_._2).*/foreach(println)
	
	println()
	println("Messages:")
	p.getMessages.foreach(println)
}
