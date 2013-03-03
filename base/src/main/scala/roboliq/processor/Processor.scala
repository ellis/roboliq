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
import roboliq.core._
import RqPimper._
import spray.json.JsNumber
import roboliq.commands.arm.MovePlateHandler


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
	handler_l: List[CommandHandler]
) {
	val logger = Logger[this.type]

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
	val token_m = new HashMap[List[Int], CmdToken]
	val lookupMessage_m = new HashMap[String, RqResult[Unit]]
	val internalMessage_l = new ArrayBuffer[RqResult[Unit]]
	//val computationMessage_m = new HashMap[List[Int], RqResult[Unit]]
	//val conversionMessage_m = new HashMap[KeyClass, RqResult[Unit]]
	//val plateWell_m = new HashMap[String, PlateWell]
	//val well_m = new HashMap[String, Well]
	//val events_m = new HashMap[List[Int], List[Event]]
	
	val conversion_m = new HashMap[Type, RqFunctionHandler]
	/*conversion_m(ru.typeOf[String]) = Conversions.asString
	conversion_m(ru.typeOf[Integer]) = Conversions.asInteger
	conversion_m(ru.typeOf[Boolean]) = Conversions.asBoolean
	conversion_m(ru.typeOf[TipModel]) = Conversions.tipModelHandler
	conversion_m(ru.typeOf[Tip]) = Conversions.tipHandler
	conversion_m(ru.typeOf[PlateModel]) = Conversions.asPlateModel
	conversion_m(ru.typeOf[PlateLocation]) = Conversions.plateLocationHandler
	conversion_m(ru.typeOf[Plate]) = Conversions.plateHandler
	conversion_m(ru.typeOf[PlateState]) = Conversions.plateStateHandler
	conversion_m(ru.typeOf[List[String]]) = Conversions.asStringList*/
	//conversion_m(ru.typeOf[Test]) = Conversions.testHandler
	
	def loadJsonData(file: java.io.File) {
		import org.apache.commons.io.FileUtils
		val s = FileUtils.readFileToString(file)
		val config = JsonParser(s).asJsObject
		loadJsonData(config)
	}
	
	def loadJsonData(data: JsObject) {
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

	def setCommands(cmd_l: List[JsObject]) {
		cmd1_l = handleComputationItems(None, cmd_l.map(js => ComputationItem_Command(js)))
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
					val node = Node_Command(parent_?, index, fnargs0)
					// Store command
					setEntity(node.contextKey, cmd)
					List(node)
				case RqItem_Function(fnargs) =>
					//val fnargs2 = concretizeArgs(fnargs, parent_?.flatMap(_.contextKey_?), parent_?, index)
					List(Node_Computation(parent_?, index, parent_?.flatMap(_.contextKey_?), fnargs))
				case ComputationItem_Token(token) =>
					val id = parent_?.map(_.path).getOrElse(Nil) ++ List(index)
					token_m(id) = token
					Nil
				case ComputationItem_Events(event_l) =>
					List(Node_Events(parent_?, index, event_l))
				case EventItem_State(key, jsval) =>
					println("EventItem_State: "+(key, parent_?.map(_.time).getOrElse(List(0)), jsval))
					//sys.exit()
					db.setAt(key, parent_?.map(_.time).getOrElse(List(0)), jsval)
					Nil
				case _ =>
					Nil
			}
		}
	}
	
	private def concretizeArgs(
		fnargs: RqFunctionArgs,
		contextKey_? : Option[TKP],
		parent_? : Option[Node],
		index: Int
	): RqFunctionArgs = {
		fnargs.copy(arg_l = concretizeArgs(fnargs.arg_l, contextKey_?, parent_?, index))
	}

	// REFACTOR: Remove this, if the functionality is now is Node
	private def concretizeArgs(
		kco_l: List[KeyClassOpt],
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
							setEntityObj(node.kc, obj)
							None
						case EventItem_State(key, jsval) =>
							setState(key, node.time, jsval)
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
		state_m(node) = state
		makeConversionNodesForInputs(node)
	}
	
	private def makeConversionNodesForInputs(node: Node): List[Node] = {
		val default_l = makeConversionNodesForDefaults(node)
		
		// Try to add missing conversions for inputs which are not JsValues 
		val l0 = node.input_l.filterNot(kco => kco.kc.isJsValue || kcNode_m.contains(kco.kc)).zipWithIndex.flatMap(pair => {
			val (kco, i) = pair
			makeConversionNodesForInput(node, kco, i + 1)
		})
		val l = default_l ++ l0 
		// Register the conversion nodes
		l.foreach(node => kcNode_m(node.kc) = node)
		//println("node.input_l: "+node.input_l)
		//println("l: "+l)
		registerNodes(l)
		l
	}
	
	private def makeConversionNodesForDefaults(node: Node): List[Node_Conversion] = {
		// Create default entities
		// TODO: Create a general approach to creating default objects
		node.input_l.filter(kco => kco.kc.isJsValue).flatMap(kco => {
			val kc = kco.kc
			val id = kc.key.key
			val time = List(0)
			// If there is no initial tipState registered yet:
			if (db.getAt(kc.key, time).isError) {
				import RqFunctionHandler._
				kc.key.table match {
					case "tipState" =>
						val fnargs = fnRequire(lookup[Tip](id)) { (tip) =>
							val tipState = TipState.createEmpty(tip)
							for {
								tipStateJson <- ConversionsDirect.toJson(tipState)
							} yield {
								List(returnEvent(kc.key, tipStateJson))
							}
						}
						List(Node_Conversion(None, Some(kc.id), None, time, None, fnargs, kc))
					case "vessel" =>
						//WellSpecParser.parse(input)
						db.set(kc.key, JsObject("id" -> JsString(id)))
						Nil
					case "vesselState" =>
						val fnargs = fnRequire(lookup[Vessel](id)) { (vessel) =>
							val vesselState = VesselState(vessel, VesselContent.Empty)
							for {
								vesselStateJson <- ConversionsDirect.toJson[VesselState](vesselState)//Conversions.vesselStateToJson(vesselState)
							} yield {
								//println("vesselStateJson: "+vesselStateJson)
								//db.set(kc.key, time, vesselStateJson)
								//println("indb: "+db.get(kc.key))
								//sys.exit()
								List(returnEvent(kc.key, vesselStateJson))
							}
						}
						List(Node_Conversion(None, Some(kc.id), None, time, None, fnargs, kc))
					case _ =>
						Nil
				}
			}
			else
				Nil
		})
	}
	
	private def makeConversionNodesForInput(node: Node, kco: KeyClassOpt, index: Int): List[Node_Conversion] = {
		if (kco.kc.isJsValue)
			return Nil
		
		val kc = kco.kc
		//val contextKey_? = if (kc.isJsValue) Some(kc.key) else None
		kco.conversion_? match {
			// If a user-supplied conversion function was supplied:
			case Some(fnargs0) =>
				val fnargs = concretizeArgs(fnargs0, node.contextKey_?, Some(node), 0)
				List(Node_Conversion(Some(node), Some(kc.id), Some(index), node.time, node.contextKey_?, fnargs, kc))
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
					val node2 = Node_Conversion(None, Some(kc.id), None, node.time, None, fnargs, kc)
					// REFACTOR: Can the recursion be removed, since registerNode will be called on node2 soon anyway
					node2 :: makeConversionNodesForInput(node2, kco2, 1)
				}
				/*
				// Create well objects, which are not stored as JsValue entities
				else if (kc.clazz <:< ru.typeOf[Well]) {
					WellSpecParser.parse(kc.key.key) match {
						case roboliq.core.Error(l) =>
							internalMessage_l += RqError(l.toList, Nil)
							Nil
						case roboliq.core.Success(l) =>
							val wellInfo_l = l.flatMap(pair => {
								val (plateId, wellSpec_l) = pair
								wellSpec_l.flatMap(_ match {
									case wellSpec: WellSpecOne =>
										Some(plateId -> wellSpec.rc)
									case _ =>
										internalMessage_l += RqError("Expected wellSpec to be WellSpecOne")
										None
								})
							})
							val arg_l = wellInfo_l.map(pair =>
								KeyClassOpt(KeyClass(TKP("plate", pair._1, Nil), ru.typeOf[Plate]), false)
							)
							val fn = (l: List[Object]) => {
								val well_l = (l.asInstanceOf[List[Plate]] zip wellInfo_l).map(pair => {
									val (plate, (_, rc)) = pair
									val well = new WellPosition(
										id = WellSpecParser.wellId(plate, rc.row, rc.col),
										idPlate = plate.id,
										index = WellSpecParser.wellIndex(plate, rc.row, rc.col),
										iRow = rc.row,
										iCol = rc.col,
										indexName = WellSpecParser.wellIndexName(plate.nRows, plate.nCols, rc.row, rc.col)
									)
									ConversionItem_Object(well)
								})
								RqSuccess(well_l)
							}
							val fnargs = RqFunctionArgs(fn, arg_l)
							List(Node_Conversion(None, Some(kc.id), None, node.time, None, fnargs, kc))
					}
				}*/
				else {
					//val contextKey_? = node.contextKey_?
					val contextKey_? = if (kc.isJsValue) Some(kc.key) else None
					conversion_m.get(kc.clazz) match {
						case Some(handler: ConversionHandler1) =>
							val kc0 = kc.changeClassToJsValue
							val fnargs0 = handler.createFunctionArgs(kc0)
							val fnargs = concretizeArgs(fnargs0, contextKey_?, None, 0)
							List(Node_Conversion(None, Some(kc.id), None, node.time, contextKey_?, fnargs, kc))
						case Some(handler: ConversionHandlerN) =>
							val kc0 = kc.changeClassToJsValue
							val fnargs0 = handler.fnargs
							List(Node_Conversion(None, Some(kc.id), None, node.time, contextKey_?, fnargs0, kc))
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
							List(Node_Conversion(None, Some(kc.id), None, node.time, contextKey_?, fnargs, kc))
							// FIXME: should put this message in a map so it only shows up once
							//internalMessage_l += RqError[Unit](s"No converter registered for ${kco.kc.id}, required by ${node.id}")
							//Nil
					}
				}
		}
	}
	
	def setEntity(key: TKP, jsval: JsValue) {
		db.set(key, jsval)
		val kc = KeyClass(key, ru.typeOf[JsValue])
		registerEntity(kc)
	}
	
	def setState(key: TKP, time: List[Int], jsval: JsValue) {
		db.setAt(key, time, jsval)
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
	def run(maxLoops: Int = -1) {
		var countdown = maxLoops
		while (countdown != 0) {
			if (runStep())
				countdown -= 1
			else
				countdown = 0
		}
		//makeMessagesForMissingInputs()
	}
	
	private def runStep(): Boolean = {
		lookupMessage_m.clear

		val state0_m = state_m.toMap
		
		// Get all KeyClassOpt from all nodes
		val kco_l = state0_m.keySet.flatMap(_.input_l)
		// Find whether KeyClass is optional for all nodes
		val opt_m = kco_l.groupBy(_.kc).mapValues(l => l.find(_.opt == false).isEmpty)

		// TODO: at this point we should perhaps lookup the KeyClasses in opt_m

		// Try to get values for all kcos
		val kcToValue_m = opt_m.keys.toList.map(kc => kc -> getEntity(kc)).toMap
		//println("kcToValue_m: "+kcToValue_m)
		val kcoToValue_m = kco_l.toList.map(kco => {
			val value0 = kcToValue_m(kco.kc)
			val value = {
				if (kco.opt) value0.map(Some.apply).orElse(RqSuccess(None))
				else value0
			}
			kco -> value
		}).toMap
		
		// Set message for missing entities
		lookupMessage_m  ++= kcoToValue_m.toList.filter(pair => pair._2.isError && pair._1.kc.isJsValue).map(pair => pair._1.kc.id -> RqError("missing")).toMap
		
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
		e1.toList.sortBy(_._1.id).map(pair => pair._1.id + getEntityString(pair)).foreach(println)
		println()
		e2.toList.sortBy(_._1.id).map(pair => pair._1.id + getEntityString(pair)).foreach(println)
		println()
		println("Nodes")
		println("-----")
		state0_m.values.toList.sortBy(_.node.id).map(state => state.node.id + ": " + state.status + " " + state.node.contextKey_?.map(_.id)).foreach(println)
		//state_m.foreach(println)

		val pending_l = makePendingComputationList
		pending_l.foreach(state => runComputation(state.node, kcoToValue_m))
		
		println()
		println("Computations")
		println("------------")
		pending_l.map(state => state.node.id + ": " + state.status).foreach(println)
		println()
		// TODO: show added conversions
		
		!pending_l.isEmpty
	}

	/**
	 * Return all ready nodes which don't depend on state,
	 * plus the next node which depends on state after exclusely successful nodes.
	 */
	private def makePendingComputationList: List[NodeState] = {
		val order_l = state_m.values.toList.sortBy(_.node.time)(ListIntOrdering).dropWhile(_.status == Status.Success)
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
		if (kc.clazz == ru.typeOf[JsValue]) {
			if (kc.time.isEmpty)
				db.get(kc.key)
			else
				db.getBefore(kc.key, kc.time)
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

	def setObj[A: TypeTag](id: String, a: A, time: List[Int] = Nil): RqResult[Unit] = {
		val typ = ru.typeTag[A].tpe
		for {
			table <- ConversionsDirect.findTableForType(typ)
			jsval <- ConversionsDirect.toJson(a)
		} yield {
			val tkp = TKP(table, id, Nil)
			setState(tkp, time, jsval)
			()
		}
	}
	
	def getObjFromDbAt[A <: Object : TypeTag](id: String, time: List[Int]): RqResult[A] = {
		val typ = ru.typeTag[A].tpe
		logger.trace(s"getObjFromDbAt[$typ]($id, $time)")
		for {
			table <- ConversionsDirect.findTableForType(typ)
			kc = KeyClass(TKP(table, id, Nil), typ, Nil)
			//_ = println("kc: "+kc)
			obj <- cache_m.get(kc) match {
				case Some(x) => RqSuccess(x.asInstanceOf[A])
				case None => Conversions.readByIdAt[A](db, id, time)
			}
		} yield obj
	}
	
	private def createCommandFnArgs(cmd: JsObject): RqFunctionArgs = {
		val cmd_? : Option[String] = cmd.fields.get("cmd").flatMap(_ match {
			case JsString(s) => Some(s)
			case _ => None
		})
		cmd_? match {
			case None => RqFunctionArgs(_ => RqError("missing field `cmd`"), Nil)
			case Some(name) =>
				handler_m.get(name) match {
					case None => RqFunctionArgs(_ => RqError(s"no handler found for `cmd = ${name}`"), Nil)
					case Some(handler) => handler.fnargs
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
