package roboliq.processor

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer
import scala.collection._
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.typeTag
import scala.util.Try
import scalaz._
import grizzled.slf4j.Logger
import spray.json.JsArray
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsNull
import spray.json.JsonParser
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.math.Ordering
import roboliq.core._, roboliq.entity._, roboliq.events._
import spray.json.JsNumber


/**
 * A command is a JSON object that represents a command and its parameters.
 * A command handler produces a Computation which requires the command's 
 * json parameters and objects in the database or programmatically generated objects,
 * and it returns a list of computations, events, commands, and tokens.  
 */
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

case class Token_Comment(s: String) extends CmdToken

class ProcessorData(
	handler_l: List[CommandHandler[_ <: Object]]
) {
	val logger = Logger[this.type]

	private val eventHandler_l0 = List[EventHandler](
		new PlateLocationEventHandler,
		new TipAspirateEventHandler,
		new TipDispenseEventHandler,
		new TipMixEventHandler,
		new TipCleanEventHandler,
		new VesselAddEventHandler,
		new VesselRemoveEventHandler
	)
	
	private val handler_m: Map[String, CommandHandler[_ <: Object]] = handler_l.map(handler => handler.id -> handler).toMap
	private val eventHandler_m: Map[Class[_], EventHandler] = eventHandler_l0.map(handler => handler.eventClass -> handler).toMap
	
	val db = new DataBase2
	// Top levels command nodes
	var cmd1_l: List[Node] = Nil
	// Conversion nodes
	//val kcNode_m = new HashMap[KeyClass, Node_Conversion]
	val state_m = new HashMap[Node, NodeState]
	// REFACTOR: this is a hack...
	//val defaultKc_l = new ArrayBuffer[KeyClass]
	//val cache_m = new HashMap[KeyClass, Object]
	///** Map from source ID to vessel IDs containing that source */
	//val sources_m = new HashMap[String, List[String]]
	val token_m = new HashMap[List[Int], CmdToken]
	val lookupMessage_m = new HashMap[String, RqResult[Unit]]
	val internalMessage_l = new ArrayBuffer[RqResult[Unit]]
	
	val conversion_m = new HashMap[Type, RqFunctionHandler]
	
	def loadJsonData(file: java.io.File): RqResult[Unit] = {
		Try {
			import org.apache.commons.io.FileUtils
			val s = FileUtils.readFileToString(file)
			val config = JsonParser(s).asJsObject
			loadJsonData(config)
		}
	}
	
	def loadJsonData(data: JsObject): RqResult[Unit] = {
		Try {
			data.fields.foreach(pair => {
				val (table, JsArray(elements)) = pair
				if (table != "cmd") {
					elements.foreach(jsval => {
						val jsobj = jsval.asJsObject
						val idField = ConversionsDirect.findIdFieldForTable(table)
						val key = jsobj.fields("id").asInstanceOf[JsString].value
						val tkp = TKP(table, key, Nil)
						val time = if (table.endsWith("State")) List(0) else Nil
						setState(tkp, time, jsval)
					})
				}
			})
			data.fields.get("cmd").foreach(jsval => {
				val JsArray(elements) = jsval
				val cmd_l = elements.map(_.asInstanceOf[JsObject])
				setCommands(cmd_l)
			})
		}
	}
	
	def setPipetteDevice(device: roboliq.devices.pipette.PipetteDevice) {
		val kc = KeyClass(TKP("pipetteDevice", "default", Nil), ru.typeOf[roboliq.devices.pipette.PipetteDevice])
		cache_m(kc) = device
	}

	def setCommands(cmd_l: List[Cmd]) {
		cmd1_l = handleComputationItems(None, cmd_l.map(cmd => ComputationItem_Command(cmd)))
		registerNodes(cmd1_l)
	}

	private def setComputationResult(node: Node, result: scala.util.Try[RqReturn]): Status.Value = {
		//println("setComputationResult()")
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
	
	private def handleComputationItems(parent_? : Option[Node], l: List[RqItem]): List[Node] = {
		val idParent = parent_?.map(_.path).getOrElse(Nil)
		l.zipWithIndex.flatMap { pair => 
			val (r, index0) = pair
			val index = index0 + 1
			r match {
				case ComputationItem_Command(cmd) =>
					// Find handler for this command
					val fnargs0 = createCommandFnArgs(cmd)
					// Create node
					val node = Node_Command(parent_?, index, fnargs0, desc=cmd.toString)
					// Store command
					db.addCmd(node.contextKey.key, cmd)
					List(node)
				case RqItem_Function(fnargs) =>
					List(Node_Computation(parent_?, index, parent_?.flatMap(_.contextKey_?), fnargs))
				case ComputationItem_Token(token) =>
					val id = parent_?.map(_.path).getOrElse(Nil) ++ List(index)
					token_m(id) = token
					Nil
				case ComputationItem_Events(event_l) =>
					List(Node_Events(parent_?, index, event_l, eventHandler_m.toMap))
				case ComputationItem_Entity(tpe, entity) =>
					logger.trace(s"ComputationItem_Entity: $tpe, $entity")
					//sys.exit()
					//5 / 0
					db.set(tpe, entity)
					Nil
				case EventItem_State(tpe, state) =>
					val time = parent_?.map(_.time).getOrElse(Nil)
					logger.trace(s"EventItem_State: $tpe, $state, $time")
					//sys.exit()
					db.setAfter(tpe, state, time)
					Nil
				case _ =>
					Nil
			}
		}
	}
	
	private def concretizeArgs(
		fnlookups: RqFunctionLookups,
		contextKey_? : Option[TKP],
		parent_? : Option[Node],
		index: Int
	): RqFunctionArgs = {
		RqFunctionArgs(
			fn = fnlookups.fn,
			arg_l = fnlookups.lookup_l.map(_ match {
				case Lookup_Command
case class Lookup_Command(tpe: Type, context: List[Int] = Nil) extends Lookup {
	val keyName = s"cmd[${context.mkString("/")}]"
}
case class Lookup_Entity(tpe: Type, id: String) extends Lookup {
	val keyName = s"$tpe[$id]"
}
case class Lookup_EntityOption(tpe: Type, id: String) extends Lookup {
	val keyName = s"$tpe[$id]?"
}
case class Lookup_EntityList(tpe: Type, id_l: List[String]) extends Lookup {
	val keyName = s"$tpe[${id_l.mkString(",")}]"
}
case class Lookup_EntityAll(tpe: Type) extends Lookup {
			})
		)
	}

	// REFACTOR: Remove this, if the functionality is now is Node
	private def concretizeArgs(
		arg_l: List[Lookup],
		contextKey_? : Option[TKP],
		parent_? : Option[Node],
		index: Int
	): List[KeyClassOpt] = {
		val idPrefix = (parent_?.map(_.path).getOrElse(Nil) ++ List(index)).mkString("", "/", "#")
		val time = parent_?.map(_.time).getOrElse(Nil)
		kco_l.zipWithIndex.map(pair => {
			val (kco0, i) = pair
			// Set time if this is a state variables
			val kco = if (kco0.kc.key.table.endsWith("State")) kco0.changeTime(time) else kco0
			// Substitute in full path for "context" arguments
			if (kco.kc.key.key == "$" && contextKey_?.isDefined) {
				val contextKey = contextKey_?.get
				val key2 = contextKey.copy(path = contextKey.path ++ kco.kc.key.path)
				kco.copy(kc = kco.kc.copy(key = key2))
			}
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
	
	private def setConversionResult(node: Node_Conversion, result: scala.util.Try[RqReturn]): Status.Value = {
		val state = state_m(node)
		val child_l = result match {
			case scala.util.Success(RqSuccess(l, _)) =>
				l.zipWithIndex.flatMap { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case RqItem_Function(fnargs) =>
							val fnargs2 = concretizeArgs(fnargs, node.contextKey_?, Some(node), index)
							Some(Node_Conversion(Some(node), None, Some(index), node.time, node.contextKey_?, fnargs2, node.kc))
						case ConversionItem_Object(obj) =>
							db.set(node.kc.clazz, obj)
							None
						case EventItem_State(tpe, state) =>
							logger.trace(s"EventItem_State: $tpe, $state, $time")
							val time = if (node.time == List(0)) Nil else node.time
							db.setAfter(tpe, state, time)
							None
						case _ =>
							internalMessage_l += RqError("invalid return item for a conversion node")
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
		// FIXME: for debug only
		//if (node .id == "pipettePolicy[Water free dispense]<PipettePolicy>")
		//	assert(false)
		if (state_m.exists(_._1.id == node.id)) {
			logger.warn(s"Node with id `${node.id}` was already registered and will not be registered again")
			logger.warn("node: "+node)
			logger.warn("previous: "+state_m.find(_._1.id == node.id))
			5 / 0
			return
		}
		// ENDFIX
		state_m(node) = state
		makeConversionNodesForInputs(node)
	}
	
	private def makeConversionNodesForInputs(node: Node) {
		// Try to add missing conversions for inputs which are not JsValues 
		val l = node.input_l.filter(kco => !kco.kc.isJsValue && !kcNode_m.contains(kco.kc)).zipWithIndex.flatMap(pair => {
			val (kco, i) = pair
			makeConversionNodesForInput(node, kco, i + 1)
		})
		// FIXME: for debug only
		l.foreach(node => assert(kcNode_m.contains(node.kc)))
		//println("node.input_l: "+node.input_l)
		//println("l: "+l)
		//registerNodes(l)
		//l
	}
	
	private def regconv(node: Node_Conversion): Node_Conversion = {
		kcNode_m.get(node.kc) match {
			case None =>
				registerNode(node)
				kcNode_m(node.kc) = node
				node
			case Some(node2) =>
				node2
		}
	}
	
	private def makeConversionNodesForInput(node: Node, kco: KeyClassOpt, index: Int): List[Node_Conversion] = {
		if (kco.kc.isJsValue)
			return Nil
		
		val kc = kco.kc
		//val contextKey_? = if (kc.isJsValue) Some(kc.key) else None
		val time2 = if (kc.key.table.endsWith("State")) node.time else Nil
		kco.conversion_? match {
			// If a user-supplied conversion function was supplied:
			case Some(fnargs0) =>
				val fnargs = concretizeArgs(fnargs0, node.contextKey_?, Some(node), 0)
				List(regconv(Node_Conversion(Some(node), Some(kc.id), Some(index), node.time, node.contextKey_?, fnargs, kc)))
			// Otherwise we'll try to automatically figure out which conversion to apply. 
			case None =>
				// If this is an Option[A], create a conversion from A to Option[A],
				// then recurse into creating a conversion for A.
				if (kc.clazz <:< ru.typeOf[Option[Any]]) {
					// type parameter of option (e.g, if Option[String], clazz2 will be String)
					val clazz2 = kc.clazz.asInstanceOf[ru.TypeRefApi].args.head
					val kc2 = kc.copy(clazz = clazz2)
					val fn = (l: List[Object]) => l match {
						case List(o) =>
							RqSuccess(List(ConversionItem_Object(o)))
					}
					val kco2 = KeyClassOpt(kc2, true)
					val fnargs = RqFunctionArgs(fn, List(kco2))
					val node2 = regconv(Node_Conversion(None, Some(kc.id), None, time2, None, fnargs, kc))
					// REFACTOR: Can the recursion be removed, since registerNode will be called on node2 soon anyway
					node2 :: makeConversionNodesForInput(node2, kco2, 1)
				}
				// If we are supposed to return a list of all entities in the given table:
				else if (kc.clazz <:< ru.typeOf[List[_]] && kc.key.key == "*") {
					// FIXME: this will not update when database updates!
					val fnargs1 = RqFunctionArgs(
						arg_l = Nil,
						fn = (l: List[Object]) => {
							RqSuccess(List(ConversionItem_Object(db.getAll(kc.clazz))))
						}
					)
					List(regconv(Node_Conversion(None, Some(kc.id), None, time2, None, fnargs1, kc)))
				}
				// REFACTOR: HACK: this kind of thing should be taken care of in a general fashion
				else if (kc.clazz <:< ru.typeOf[roboliq.devices.pipette.PipetteDevice]) {
					Nil
				}
				// Otherwise, neither and Option[A] nor a list of all entities:
				else {
					//val contextKey_? = node.contextKey_?
					val contextKey_? = if (kc.isJsValue) Some(kc.key) else None
					val l = conversion_m.get(kc.clazz) match {
						case Some(handler: ConversionHandler1) =>
							val kc0 = kc.changeClassToJsValue
							val fnargs0 = handler.createFunctionArgs(kc0)
							val fnargs = concretizeArgs(fnargs0, contextKey_?, None, 0)
							List(regconv(Node_Conversion(None, Some(kc.id), None, time2, contextKey_?, fnargs, kc)))
						case Some(handler: ConversionHandlerN) =>
							val kc0 = kc.changeClassToJsValue
							val fnargs0 = handler.fnargs
							List(regconv(Node_Conversion(None, Some(kc.id), None, time2, contextKey_?, fnargs0, kc)))
						case Some(handler) =>
							// FIXME: should put this message in a map so it only shows up once
							internalMessage_l += RqError[Unit]("Unhandled converter registered for "+node+" "+kco)
							Nil
						case None =>
							// REFACTOR: Is practically a duplicate of RqFunctionHandler.cmdAs
							val fn: RqFunction = (l: List[Object]) => l match {
								case List(jsval: JsValue) =>
									ConversionsDirect.convRequirements(jsval, kc.clazz).map(_ match {
										case Left(pathToKey_m) =>
											val pathToKey_l = pathToKey_m.toList
											val arg_l = pathToKey_l.map(_._2)
											List(RqItem_Function(RqFunctionArgs(
												arg_l = arg_l,
												fn = (input_l) => {
													val lookup_m = (pathToKey_l.map(_._1) zip input_l).toMap
													ConversionsDirect.conv(jsval, kc.clazz, lookup_m).map(o =>
														List(ConversionItem_Object(o.asInstanceOf[Object])))
												}
											)))
										case Right(o) =>
											List(ConversionItem_Object(o.asInstanceOf[Object]))
									})
									/*ConversionsDirect.convLookup(jsval, kc.clazz).map { arg_l =>
										List(RqItem_Function(RqFunctionArgs(
											arg_l = arg_l,
											fn = (input_l) => {
												ConversionsDirect.conv(jsval, kc.clazz, input_l).map(o => 
													List(ConversionItem_Object(o.asInstanceOf[Object])))
											}
										)))
									}*/
								case _ =>
									RqError("Expected JsValue")
							}
							val arg_l = List(KeyClassOpt(kc.changeClassToJsValue, false))
							val fnargs = RqFunctionArgs(fn, arg_l)
							List(regconv(Node_Conversion(None, Some(kc.id), None, time2, contextKey_?, fnargs, kc)))
							// FIXME: should put this message in a map so it only shows up once
							//internalMessage_l += RqError[Unit](s"No converter registered for ${kco.kc.id}, required by ${node.id}")
							//Nil
					}
					l.foreach(node2 => kcNode_m(node2.kc) = node2)
					l
				}
		}
	}
	
	def setEntity[A <: Entity : TypeTag](a: A): RqResult[Unit] =
		setEntity(ru.typeTag[A].tpe, a)
	
	def setEntity(tpe: Type, entity: Entity): RqResult[Unit] = {
		db.set(tpe, entity)
	}
	
	def setState(tpe: Type, state: Entity, time: List[Int]) {
		//logger.trace(s"setState($tpe, $state, $time)")
		db.setAfter(tpe, state, time)
	}

	// Match "tbl[key]"
	private val Rx1 = """^([a-zA-Z]+)\[([0-9.]+)\]$""".r
	// Match "tbl[key].field"
	private val Rx2 = """^([a-zA-Z]+)\[([0-9.]+)\]\.(.+)$""".r
	
	// REFACTOR: turn entity lookups into computations, somehow
	def run(maxLoops: Int = -1): ProcessorGraph = {
		var countdown = maxLoops
		var step_i = 0
		val g = new ProcessorGraph
		while (countdown != 0) {
			g.setStep(step_i)
			if (runStep(g, step_i))
				countdown -= 1
			else
				countdown = 0
			step_i += 1
		}
		//makeMessagesForMissingInputs()
		g
	}
	
	private class NodeListBuilder {
		val node_l = new mutable.HashSet[Node]
		val kco_l = new mutable.HashSet[KeyClassOpt]
		val kc_l = new mutable.HashSet[KeyClass]
		
		def addNode(node: Node) {
			if (!node_l.contains(node)) {
				node_l += node
				
				// Add child nodes
				val state = state_m(node)
				state.child_l.foreach(addNode)
				
				node.input_l.foreach(addKco)
			}
		}
		
		private def addKco(kco: KeyClassOpt) {
			if (!kco_l.contains(kco)) {
				kco_l += kco
				val kc = kco.kc
				if (!kc_l.contains(kc)) {
					kc_l += kc
					kcNode_m.get(kc).map(addNode)
				}
			}
		}
	}
	
	private def runStep(g: ProcessorGraph, step_i: Int): Boolean = {
		lookupMessage_m.clear

		val nodeListBuilder = new NodeListBuilder
		cmd1_l.foreach(nodeListBuilder.addNode)
		defaultKc_l.foreach(kc => nodeListBuilder.addNode(kcNode_m(kc)))

		val node_l = nodeListBuilder.node_l.toList.sortBy(_.id)
		val kco_l = nodeListBuilder.kco_l.toSet
		val kc_l = nodeListBuilder.kc_l.toList.sortBy(_.id)

		val state_l = node_l.map(state_m)
		// Find whether KeyClass is optional for all nodes
		val opt_m = kco_l.groupBy(_.kc).mapValues(l => l.find(_.opt == false).isEmpty)

		// Try to get values for all kcos
		val kcToValue_m = kc_l.map(kc => kc -> getEntity(kc)).toMap
		//println("kcToValue_m: "+kcToValue_m)
		val kcoToValue_m = kco_l.toList.map(kco => {
			val value0 = kcToValue_m(kco.kc)
			val value = {
				if (kco.opt) value0.map(Some.apply).orElse(RqSuccess(None))
				else value0
			}
			kco -> value
		}).toMap
		
		/*val nodeToState_m = node_l.map(node => node -> {
			state_m.get(node) match {
				case None => node -> new NodeState(node)
				case Some(state) => state
			}
		}).toMap*/

		//val state0_m = state_m.toMap
		
		// Update status for all nodes
		state_l.foreach(_.updateInput(kcoToValue_m.apply _))
		
		/*
		cmd[1].id: JsValue = P1
		cmd[1].id: String = P1
		plate[P1] ...
		param[1#1]: Plate = fn(cmd[1].id: JsValue) 
		*/

		g.setEntities(kcoToValue_m)
		state_l.foreach(g.setNode)
		//println("dot:")
		//println(g.toDot)
		
		println()
		println("runStep "+step_i)
		println("=========")
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
		e1.toList.sortBy(_._1.id).map(pair => pair._1.id + getEntityString(pair)).foreach(println)
		println()
		e2.toList.sortBy(_._1.id).map(pair => pair._1.id + getEntityString(pair)).foreach(println)
		
		println()
		println("Nodes")
		println("-----")
		state_l.sortBy(_.node.id).map(state => {
			val status = state.status match {
				case Status.NotReady => "N"
				case Status.Ready => "R"
				case Status.Success => "S"
				case Status.Error => "E"
			}
			status + " " + state.node.id + ": " + state.node.contextKey_?.map(_.id + " ").getOrElse("") + state.node.desc
		}).foreach(println)
		//state_m.foreach(println)

		val pending_l = makePendingComputationList(node_l)
		pending_l.foreach(state => runComputation(state.node, kcoToValue_m))
		
		println()
		println("Computations")
		println("------------")
		pending_l.map(state => state.node.id + ": " + state.status).foreach(println)
		println()
		// TODO: show added conversions
		
		// Set message for missing entities
		if (pending_l.isEmpty && getMessages.isEmpty)
			lookupMessage_m ++= kcoToValue_m.toList.filter(pair => pair._2.isError && pair._1.kc.isJsValue).map(pair => pair._1.kc.id -> RqError("missing")).toMap

		!pending_l.isEmpty
	}

	/**
	 * Return all ready nodes which don't depend on state,
	 * plus the next node which depends on state after exclusively successful nodes.
	 */
	private def makePendingComputationList(node_l: List[Node]): List[NodeState] = {
		val order_l = state_m.values.toList.sortBy(_.node.time)(ListIntOrdering).dropWhile(_.status == Status.Success)
		// Find first node which isn't ready and depends on state
		val blocker_? = order_l.find(state => state.status == Status.NotReady && state.node.input_l.exists(_.kc.key.table.endsWith("State")))
		val timeEnd = blocker_?.map(_.node.time).getOrElse(List(Int.MaxValue))
		println()
		println(s"makePending (<= $timeEnd)")
		order_l.foreach(state => 
			println(state.node.time + " " + state.status.toString.take(1) + " " + state.node.id + ": " + state.node.contextKey_?.map(_.id + " ").getOrElse("") + state.node.desc)
		)
		println()
		
		order_l.filter(state => state.status == Status.Ready && ListIntOrdering.compare(state.node.time, timeEnd) <= 0)
		/*
		//val order_l = state_m.toList.sortBy(_._1.path)(ListIntOrdering).map(_._2).dropWhile(_.status == Status.Success)
		order_l match {
			case Nil => Nil
			case next :: _ =>
				val timeNext = next.node.time
				val (prefix_l, suffix_l) = order_l.span(_.node.time == timeNext)
				val next_l = prefix_l.filter(_.status == Status.Ready)
				val ready_l = suffix_l.filter(state => {
					// Node is ready
					state.status == Status.Ready &&
					// And it doesn't depend on state
					state.node.input_l.forall(kco => !kco.kc.key.table.endsWith("State"))
				})
				// Take next node if it's ready (regardless of whether it depends on state)
				next_l ++ ready_l
		}*/
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
				val tryResult = scala.util.Try { node.fnargs.fn(input_l) }
				node match {
					case n: Node_Command =>
						setComputationResult(n, tryResult)
					case n: Node_Computation =>
						setComputationResult(n, tryResult)
					case n: Node_Conversion =>
						setConversionResult(n, tryResult)
					case n: Node_Events =>
						setComputationResult(n, tryResult)
				}
			case e =>
				println("Error:")
				println(node)
				println(e)
				assert(false)
				Status.Error
		}
	}
	
	private def getEntity(kc: KeyClass): RqResult[Object] = {
		logger.trace(s"getEntity($kc)")
		if (kc.time.isEmpty)
			db.get(kc.clazz, kc.key.key)
		else
			db.getAt(kc.clazz, kc.key.key, kc.time)
	}
	
	private def getEntity(kco: KeyClassOpt): RqResult[Object] = {
		val res = getEntity(kco.kc)
		if (kco.opt)
			res map(Some(_)) orElse RqSuccess(None)
		else
			res
	}
	
	private def createCommandFnArgs(cmd: Cmd): RqFunctionArgs = {
		handler_m.get(cmd.cmd) match {
			case None => RqFunctionArgs(_ => RqError(s"no handler found for `cmd = ${cmd.cmd}`"), Nil)
			case Some(handler) => handler.fnargs
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
	
	def getTokenList(): List[(List[Int], CmdToken)] = {
		token_m.toList.sortBy(_._1)(ListIntOrdering)
	}
	
	def getMessages(): List[String] = {
		def makeMessages(id: String, result: RqResult[_]): List[String] = {
			result.getErrors.map(id+": ERROR: "+_) ++ result.getWarnings.map(id+": WARNING: "+_)
		}
		lookupMessage_m.toList.sortBy(_._1).flatMap(pair => makeMessages(pair._1, pair._2)) ++
		getConversionNodeList.map(state_m).sortBy(_.node.id).flatMap(state => makeMessages(state.node.id, state.result)) ++
		getCommandNodeList.map(state_m).sortBy(_.node.path)(ListIntOrdering).flatMap(state => makeMessages(state.node.id, state.result)) ++
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
			ListIntOrdering.compare(a.path, b.path)
		}
	}
}
