package roboliq.processor2

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer
import scala.collection._
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
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

case class IdClass(id: String, clazz: Class[_])

sealed trait ComputationItem
case class ComputationItem_Event(event: Event) extends ComputationItem
case class ComputationItem_EntityRequest(id: String) extends ComputationItem
case class ComputationItem_Computation(
	entity_l: List[IdClass],
	fn: (List[Object]) => ComputationResult
) extends ComputationItem
case class ComputationItem_Command(cmd: JsObject) extends ComputationItem
case class ComputationItem_Token(token: Token) extends ComputationItem
case class ComputationItem_Entity(id: String, jsval: JsValue) extends ComputationItem
//case class ComputationItem_Object(idclass: IdClass, obj: Object) extends ComputationItem

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

class ProcessorData(
	handler_l: List[CommandHandler]
) {
	private val handler_m: Map[String, CommandHandler] = handler_l.flatMap(handler => handler.cmd_l.map(_ -> handler)).toMap
	
	// Root of computation hierarchy
	//val root = new Node_Computation(null, 0, Nil, (_: List[Object]) => RqError("hmm"), Nil)
	// List of conversions
	var cmd1_l: List[Node_Computes] = Nil
	val idclassNode_m = new HashMap[IdClass, Node_Conversion]
	val status_m = new HashMap[Node_Computes, Status.Value]
	val dep_m: MultiMap[String, Node_Computes] = new HashMap[String, mutable.Set[Node_Computes]] with MultiMap[String, Node_Computes]
	val children_m = new HashMap[Node, List[Node]]
	val result_m = new HashMap[Node_Computes, RqResult[_]]
	val entity_m = new HashMap[IdClass, Object]
	val entityStatus_m = new HashMap[IdClass, Status.Value]
	//val entityMessages_m = new HashMap[IdClass, RqResult[_]]
	val token_m = new HashMap[List[Int], Token]
	val lookupMessage_m = new HashMap[String, RqResult[Unit]]
	val computationMessage_m = new HashMap[List[Int], RqResult[Unit]]
	val conversionMessage_m = new HashMap[IdClass, RqResult[Unit]]
	
	val conversion_m = new HashMap[Class[_], (JsValue) => ConversionResult]
	conversion_m(classOf[String]) = Conversions.asString
	conversion_m(classOf[Integer]) = Conversions.asInteger
	conversion_m(classOf[PlateModel]) = Conversions.asPlateModel
	conversion_m(classOf[Plate]) = Conversions.asPlate
	
	def setCommands(cmd_l: List[JsObject]) {
		cmd1_l = handleComputationItems(None, cmd_l.map(js => ComputationItem_Command(js)))
	}
	
	private def setComputationResult(node: Node_Computes, result: scala.util.Try[ComputationResult]): Status.Value = {
		result match {
			case scala.util.Success(RqSuccess(l, _)) =>
				val child_l = handleComputationItems(Some(node), l)
				setChildren(node, child_l)
			case _ =>
		}
		val (s, r) = updateComputationStatusAndResult(node, result)
		computationMessage_m(node.id) = r
		s
	}
	
	private def updateComputationStatusAndResult(node: Node_Computes, result: scala.util.Try[RqResult[_]]): (Status.Value, RqResult[Unit]) = {
		val (s, r: RqResult[_]) = result match {
			case scala.util.Success(r) =>
				r match {
					case RqSuccess(l, _) => (Status.Success, r)
					case RqError(_, _) => (Status.Error, r)
				}
			case scala.util.Failure(e) =>
				(Status.Error, RqError[Unit](e.getMessage()))
		}
		status_m(node) = s
		result_m(node) = r
		println("node: "+node+", status: "+s)
		val r2: RqResult[Unit] = r.map{ _ => () }
		(s, r2)
	}
	
	private def handleComputationItems(parent_? : Option[Node_Computes], l: List[ComputationItem]): List[Node_Computes] = {
		val idParent = parent_?.map(_.id).getOrElse(Nil)
		val child_l: List[Node_Computes] = l.zipWithIndex.flatMap { pair => 
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
					val idEntity = s"cmd[${node.label}]"
					setEntity(idEntity, cmd)
					Some(node)
				case result: ComputationItem_Computation =>
					val keyCmd = findCommandParent(parent_?).map(_.label).getOrElse("")
					val entity2_l = result.entity_l.map(idclass => {
						// Substitute in full path for command parameters starting with '$'
						val idclass2 = {
							if (idclass.id.startsWith("$")) {
								idclass.copy(id = s"cmd[${keyCmd}].${idclass.id.tail}")
							}
							else idclass
						}
						idclass2
					})
					
					//val result2 = result.copy(entity_l = entity2_l)
					Some(Node_Computation(parent_?, index, entity2_l, result.fn))
				case ComputationItem_Token(token) =>
					val id = parent_?.map(_.id).getOrElse(Nil) ++ List(index)
					//println("token: "+Node_Token(node, index + 1, token))
					token_m(id) = token
					None
				case ComputationItem_Entity(id, jsval) =>
					setEntity(id, jsval)
					//new Node_Result(node, index, r)
					None
				case _ =>
					//new Node_Result(node, index, r)
					None
			}
		}
		registerNodes(child_l)
		child_l
	}
	
	private def findCommandParent(node_? : Option[Node]): Option[Node] = {
		node_? match {
			case None => None
			case Some(node: Node_Command) => Some(node)
			case Some(node) => findCommandParent(node.parent_?)
		}
	}
	
	private def setConversionResult(node: Node_Conversion, result: scala.util.Try[ConversionResult]): Status.Value = {
		result match {
			case scala.util.Success(RqSuccess(l, _)) =>
				val child_l: List[Node_Computes] = l.zipWithIndex.flatMap { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ConversionItem_Conversion(input_l, fn) =>
							Some(Node_Conversion(Some(node), None, index, node.idclass, input_l, fn))
						case ConversionItem_Object(obj) =>
							setEntityObj(node.idclass, obj)
							None
					}
				}
				registerNodes(child_l)
				setChildren(node, child_l)
			case _ =>
		}
		val (s, r) = updateComputationStatusAndResult(node, result)
		conversionMessage_m(node.idclass) = r
		s
	}
	
	private def setChildren(node: Node, child_l: List[Node_Computes]) {
		if (child_l.isEmpty)
			children_m -= node
		else
			children_m(node) = child_l
	}
	
	private def registerNodes(node_l: List[Node_Computes]) {
		node_l.foreach(registerNode)
	}
	
	private def registerNode(node: Node_Computes) {
		addDependencies(node)
		updateComputationStatus(node)
	}
	
	def setEntity(id: String, jsval: JsValue) {
		val idclass = IdClass(id, classOf[JsValue])
		setEntityObj(idclass, jsval)
	}

	def setEntityObj(idclass: IdClass, obj: Object) {
		entity_m(idclass) = obj
		entityStatus_m(idclass) = Status.Success
		// Queue the computations for which all inputs are available 
		dep_m.get(idclass.id).map(_.foreach(updateComputationStatus))
	}

	// Add node to dependency set for each of its inputs
	private def addDependencies(node: Node_Computes) {
		node.input_l.foreach(idclass => {
			dep_m.addBinding(idclass.id, node)

			// Schedule lookups and create conversion nodes
			for (idclass <- node.input_l) {
				val idclass0 = IdClass(idclass.id, classOf[JsValue])
				
				// JsValue lookup
				if (!entityStatus_m.contains(idclass0))
					entityStatus_m(idclass0) = Status.Ready
					
				// Conversion
				if (!entityStatus_m.contains(idclass)) {
					assert(!idclassNode_m.contains(idclass))
					entityStatus_m(idclass) = Status.NotReady
					conversion_m.get(idclass.clazz) match {
						case Some(conversion) =>
							val fn = (l: List[Object]) => l match {
								case List(jsval: JsValue) =>
									conversion(jsval)
							}
							val node = new Node_Conversion(None, Some(idclass.toString), 0, idclass, List(idclass0), fn)
							idclassNode_m(idclass) = node
							registerNode(node)
						case None =>
							conversionMessage_m(idclass) = RqError[Unit]("No converter registered for "+idclass.clazz)
					}
				}
			}
		})
	}
	
	private def updateComputationStatus(node: Node_Computes) {
		val b = node.input_l.forall(entity_m.contains)
		if (b) {
			if (status_m.getOrElse(node, Status.Ready) != Status.Success) {
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

	// Match "tbl[key]"
	private val Rx1 = """^([a-zA-Z]+)\[([0-9.]+)\]$""".r
	// Match "tbl[key].field"
	private val Rx2 = """^([a-zA-Z]+)\[([0-9.]+)\]\.(.+)$""".r
	
	// REFACTOR: turn entity lookups into computations, somehow
	def run() {
		def step() {
			val entity_l = entityStatus_m.filter(_._2 == Status.Ready).toList.map(_._1)
			//println("entity_l: "+entity_l)
			for (idclass <- entity_l if idclass.clazz == classOf[JsValue]) {
				val result = idclass.id match {
					case Rx2(t, k, f) => findEntity(t, k, f.split('.').toList)
					case Rx1(t, k) => findEntity(t, k, Nil)
					case _ => RqError(s"don't know how to find entity: `$idclass`")
				}
				lookupMessage_m(idclass.id) = result.map(_ => ())
				
				result match {
					case e: RqError[_] =>
						entityStatus_m(idclass) = Status.Error
					case RqSuccess(jsval, w) =>
						setEntityObj(idclass, jsval)
				}
			}
			val l = makePendingComputationList
			if (!l.isEmpty) {
				runComputations(l)
				step()
			}
		}
		step()
		makeMessagesForMissingInputs()
	}
	
	private def findEntity(table: String, key: String, field_l: List[String]): RqResult[JsValue] = {
		val id = (s"$table[$key]" :: field_l).mkString(".")
		val idclass = IdClass(id, classOf[JsValue])
		entity_m.get(idclass) match {
			case Some(jsval) => RqSuccess(jsval.asInstanceOf[JsValue])
			case None =>
				if (field_l.isEmpty)
					RqError(s"entity not found: `$id`")
				else {
					for {
						jsval <- findEntity(table, key, field_l.init)
						jsobj <- if (jsval.isInstanceOf[JsObject]) RqSuccess(jsval.asJsObject) else RqError(s"bad field name: `$id`")
						fieldVal <- jsobj.fields.get(field_l.last).asRq(s"field not found: `$id`")
					} yield fieldVal
				}
		}
	}
	
	private def runComputations(node_l: List[Node_Computes]) {
		println("run")
		printNodesAndStatus()
		if (!node_l.isEmpty) {
			val status_l = node_l.map(runComputation)
			println("status_l: "+status_l)
			println("status_m: "+status_m)
			runComputations(makePendingComputationList)
		}
	}
	
	private def makePendingComputationList: List[Node_Computes] = {
		//status_m.filter(_._2 == 1).keys.toList.sorted(NodeOrdering)
		status_m.filter(_._2 == Status.Ready).keys.toList
	}
	
	private def runComputation(node: Node_Computes): Status.Value = {
		//println(s"try node ${node.name}")
		val input_l: List[Object] = node.input_l.map(entity_m)
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
		entityStatus_m.toList.filterNot(_._2 == Status.Success).map(pair => {
			val (idclass, status) = pair
			conversionMessage_m(idclass) =
				conversionMessage_m.getOrElse(idclass, RqResult.zero) flatMap {_ => RqError[Unit](s"missing entity `${idclass.id}` of class `${idclass.clazz}`")}
		})
	}
	
	def getCommandNodeList(): List[Node] = {
		def step(node: Node): List[Node] = {
			node :: children_m.getOrElse(node, Nil).flatMap(step)
		}
		cmd1_l.flatMap(step)
	}
	
	private def getConversionNodeList(node: Node_Conversion): List[Node_Conversion] = {
		node :: (children_m.getOrElse(node, Nil).collect({case n: Node_Conversion => n}).flatMap(getConversionNodeList))
	}
	
	def getConversionNodeList(): List[Node_Conversion] = {
		idclassNode_m.toList.sortBy(_._1.toString).flatMap(pair => getConversionNodeList(pair._2))
	}
	
	def getTokenList(): List[(List[Int], Token)] = {
		token_m.toList.sortBy(_._1)
	}
	
	def getMessages(): List[String] = {
		def makeMessages(id: String, result: RqResult[_]): List[String] = {
			result.getErrors.map(id+": ERROR: "+_) ++ result.getWarnings.map(id+": WARNING: "+_)
		}
		lookupMessage_m.toList.sortBy(_._1).flatMap(pair => makeMessages(pair._1, pair._2)) ++
		conversionMessage_m.toList.sortBy(_._1.id).flatMap(pair => makeMessages(pair._1.toString, pair._2)) ++
		computationMessage_m.toList.sortBy(_._1).flatMap(pair => makeMessages(getIdString(pair._1), pair._2))
	}
	
	//def getIdString(node: Node): String = getIdString(node.id)
	def getIdString(id: List[Int]): String = id.mkString(".")

	def printNodesAndStatus() {
		getConversionNodeList().foreach(node => {
			println(node.label+" "+status_m.getOrElse(node, "<missing>"))
		})
		getCommandNodeList().foreach(_ match {
			case node: Node_Command =>
				val class_s = node.getClass().getName().dropWhile(_ != '_').tail
				println(node.label+" "+class_s+" "+status_m.getOrElse(node, "<missing>")+" "+node.cmd)
			case node: Node_Computes =>
				//val prefix = "  " * (node.id.length - 1)
				//println(prefix+node)
				val class_s = node.getClass().getName().dropWhile(_ != '_').tail
				println(node.label+" "+class_s+" "+status_m.getOrElse(node, "<missing>"))
			case _ =>
		})
	}
	
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
	
	implicit object ListIntOrdering extends Ordering[List[Int]] {
		def compare(a: List[Int], b: List[Int]): Int = {
			(a, b) match {
				case (Nil, Nil) => 0
				case (Nil, _) => -1
				case (_, Nil) => 1
				case (a1 :: arest, b1 :: brest) =>
					if (a1 != b1) a1 - b1
					else compare(arest, brest)
			}
		}
	}
	
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
	val cmd3 = JsonParser("""{ "cmd": "movePlate", "id": "P1" }""").asJsObject
	
	val h1 = new PrintCommandHandler
	val h2 = new Print2CommandHandler
	val h3 = new MovePlateHandler

	val p = new ProcessorData(List(h1, h2, h3))
	
	p.setEntity("plateModel[PCR]", JsonParser("""{ "id": "PCR", "rows": 8, "cols": 12, "wellVolume": "100ul" }"""))
	p.setEntity("plate[P1]", JsonParser("""{ "id": "P1", "idModel": "PCR" }"""))
	p.setEntity("a", JsString("1"))
	p.setEntity("b", JsString("2"))
	p.setEntity("c", JsString("3"))
	//p.setEntity("cmd[1].text", JsString("Hello, World"))
	//p.addComputation(cn1.entity_l, cn1.fn, Nil)
	//p.addComputation(cn2.entity_l, cn2.fn, Nil)
	//p.addCommand(cmd1, h1)
	p.setCommands(List(cmd1, cmd2, cmd3))
	
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
	p.entity_m.toList.sortBy(_._1.id).foreach(pair => if (pair._1.clazz == classOf[JsValue]) println(pair._1.id+": "+pair._2))
	
	println()
	println("Tokens:")
	p.getTokenList./*map(_._2).*/foreach(println)
	
	println()
	println("Messages:")
	p.getMessages.foreach(println)
}
