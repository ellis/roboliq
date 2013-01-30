package roboliq.processor2

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer
import scala.collection._
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
case class ComputationItem_Command(cmd: JsObject, fn: ComputationResult) extends ComputationItem
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

import akka.actor._
import akka.routing.RoundRobinRouter

class ProcessorData extends Actor {
	
	// key is a Computation.id_r
	val root = new Node_Computation(null, 0, Nil, (_: List[Object]) => RqError("hmm"), Nil)
	//val rootObj = new Node_Conversion("rootObj", null, 0, ComputationItem_Computation(Nil, (_: List[Object]) => RqError("hmm")), Nil)
	val message_m = new HashMap[Node, List[String]]
	val children_m = new HashMap[Node, List[Node]]
	val result_m = new HashMap[Node_Computes, RqResult[_]]
	val status_m = new HashMap[Node_Computes, Int]
	val dep_m: MultiMap[String, Node_Computes] = new HashMap[String, mutable.Set[Node_Computes]] with MultiMap[String, Node_Computes]
	val entity_m = new HashMap[String, JsValue]
	val entityStatus_m = new HashMap[String, Int]
	//val entityFind_l = mutable.Set[IdClass]()
	//val entityMissing_l = mutable.Set[IdClass]()
	val cmdToJs_m = new HashMap[List[Int], JsObject]
	val entityObj_m = new HashMap[IdClass, Object]
	val idclassNode_m = new HashMap[IdClass, Node_Conversion]
	
	val conversion_m = new HashMap[Class[_], (IdClass, JsValue) => ConversionResult]
	conversion_m(classOf[String]) = Conversions.toString2
	conversion_m(classOf[Integer]) = Conversions.toInteger2
	
	val nrOfWorkers = 20
	val workerRouter = context.actorOf(Props[ComputationActor].withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter")
	
	def receive = {
		case ActorMessage_Start() =>
			
		case ActorMessage_ComputationOutput(node, result) =>
			setComputationResult(node, result)
			
		case ActorMessage_ConversionOutput(node, result) =>
	}
	
	def setComputationResult(node: Node_Computation, result: ComputationResult) {
		result_m(node) = result
		result match {
			case RqSuccess(l, _) =>
				val child_l: List[Node_Computes] = l.zipWithIndex.flatMap { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ComputationItem_Command(cmd, fn) =>
							/*val idCmd = node.id ++ List(index)
							val idCmd_s = getIdString(idCmd)
							val idEntity = s"cmd[${idCmd_s}]"
							cmdToJs_m(idCmd) = cmd
							setEntity(idEntity, cmd)
	
							val result = ComputationItem_Computation(Nil, (_: List[Object]) => fn)*/
							Some(Node_Command(node, index, cmd))
						case result: ComputationItem_Computation =>
							val entity2_l = result.entity_l.map(idclass => {
								// Substitute in full path for command parameters starting with '$'
								val idclass2 = {
									if (idclass.id.startsWith("$")) {
										val idCmd_s = getIdString(node.idCmd)
										idclass.copy(id = s"cmd[${idCmd_s}].${idclass.id.tail}")
									}
									else idclass
								}
								idclass2
							})
							//val result2 = result.copy(entity_l = entity2_l)
							Some(Node_Computation(node, index, entity2_l, result.fn, node.idCmd))
						case ComputationItem_Token(token) =>
							println("token: "+Node_Token(node, index + 1, token))
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
				setChildren(node, child_l)
				status_m(node) = 2
			case _ =>
				status_m(node) = -1
		}
	}
	
	def setConversionResult(node: Node_Conversion, result: ConversionResult) {
		result_m(node) = result
		result match {
			case RqSuccess(l, _) =>
				val child_l: List[Node_Computes] = l.zipWithIndex.flatMap { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ConversionItem_Conversion(input_l, fn) =>
							Some(Node_Conversion(input_l, fn))
						case ConversionItem_Object(idclass, obj) =>
							setEntityObj(idclass, obj)
							None
					}
				}
				setChildren(node, child_l)
				status_m(node) = 2
			case _ =>
				status_m(node) = -1
		}
	}
	
	/*
	def setComputationItem(node: Node_Computation, result: ComputationResult) {
		val child_l: List[Node] = result match {
			case RqSuccess(l, warning_l) =>
				setMessages(node, warning_l)
				l.zipWithIndex.flatMap { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ComputationItem_Command(cmd, fn) =>
							/*val idCmd = node.id ++ List(index)
							val idCmd_s = getIdString(idCmd)
							val idEntity = s"cmd[${idCmd_s}]"
							cmdToJs_m(idCmd) = cmd
							setEntity(idEntity, cmd)

							val result = ComputationItem_Computation(Nil, (_: List[Object]) => fn)*/
							Some(Node_Command(node, index, cmd))
						case result: ComputationItem_Computation =>
							val entity2_l = result.entity_l.map(idclass => {
								// Substitute in full path for command parameters starting with '$'
								val idclass2 = {
									if (idclass.id.startsWith("$")) {
										val idCmd_s = getIdString(node.idCmd)
										idclass.copy(id = s"cmd[${idCmd_s}].${idclass.id.tail}")
									}
									else idclass
								}
								idclass2
							})
							//val result2 = result.copy(entity_l = entity2_l)
							Some(Node_Computation(node, index, entity2_l, result.fn, node.idCmd))
						case ComputationItem_Token(token) =>
							Some(Node_Token(node, index + 1, token))
						case ComputationItem_Entity(id, jsval) =>
							setEntity(id, jsval)
							//new Node_Result(node, index, r)
							None
						case ComputationItem_Object(idclass, obj) =>
							setEntityObj(idclass, obj)
							//new Node_Result(node, index, r)
							None
						case _ =>
							//new Node_Result(node, index, r)
							None
					}
				}
			case RqError(error_l, warning_l) =>
				setMessages(node, error_l ++ warning_l)
				Nil
		}
		
		val child2_l = child_l.flatMap(_ match {
			case child: Node_Computation =>
				child.result.entity_l.flatMap(idclass => {
					//
					if (
						idclass.clazz != classOf[JsValue] &&
						!idclassNode_m.contains(idclass) &&
						conversion_m.contains(idclass.clazz)
					) {
						val conversion = conversion_m(idclass.clazz)
						val result = ComputationItem_Computation(List(IdClass(idclass.id, classOf[JsValue])), (l: List[Object]) => l match {
							case List(jsval: JsValue) =>
								conversion(idclass, jsval)
						})
						val node = new Node_Computation(idclass.toString, rootObj, 0, result, Nil)
						idclassNode_m(idclass) = node
						Some(node)
					}
					else {
						None
					}
				}) ++ List(child)
			case child => child :: Nil
		})
		//println(child_l, child2_l)
		setChildren(node, child2_l)
		status_m(node) = 2
	}
	*/
	
	/*
	private def setMessages(node: Node, message_l: List[String]) {
		if (message_l.isEmpty)
			message_m -= node
		else
			message_m(node) = message_l
	}*/
	
	private def setChildren(node: Node, child_l: List[Node_Computes]) {
		if (child_l.isEmpty)
			children_m -= node
		else
			children_m(node) = child_l
		
		// Handle addition of the child nodes to the Processor
		for (child <- child_l) {
			child match {
				case n: Node_Computation =>
					addDependencies(n)
					updateComputationStatus(n)
				//case ComputationItem_EntityRequest(idEntity) =>
				//	setEntity(idEntity, JsString("my "+idEntity))
				case _ =>
			}
		}
		//node_l ++= child_l
	}
	
	def setEntity(id: String, jsval: JsValue) {
		entity_m(id) = jsval
		entityStatus_m(id) = 1
		val idclass = IdClass(id, classOf[JsValue])
		entityObj_m(idclass) = jsval
		// Queue the computations for which all inputs are available 
		dep_m.get(id).map(_.foreach(updateComputationStatus))
	}

	def setEntityObj(idclass: IdClass, obj: Object) {
		assert(idclass.clazz != classOf[JsValue])
		entityObj_m(idclass) = obj
		// Queue the computations for which all inputs are available 
		dep_m.get(idclass.id).map(_.foreach(updateComputationStatus))
	}

	// Add node to dependency set for each of its inputs
	private def addDependencies(node: Node_Computes) {
		node.input_l.foreach(idclass => {
			dep_m.addBinding(idclass.id, node)
			if (!entityStatus_m.contains(idclass.id))
				entityStatus_m(idclass.id) = 0
		})
	}
	
	private def updateComputationStatus(node: Node_Computes) {
		val b = node.input_l.forall(entityObj_m.contains)
		if (b) {
			if (status_m.getOrElse(node, 0) != 1) {
				status_m(node) = 1
				val input_l = node.input_l.map(entityObj_m)
				node match {
					case n: Node_Command =>
					case n: Node_Computation =>
					case n: Node_Conversion =>
						workerRouter ! ActorMessage_ConversionInput(n, input_l)
				}
			}
		}
		else {
			status_m(node) = 0
		}
	}

	// Match "tbl[key]"
	private val Rx1 = """^([a-zA-Z]+)\[([0-9.]+)\]$""".r
	// Match "tbl[key].field"
	private val Rx2 = """^([a-zA-Z]+)\[([0-9.]+)\]\.(.+)$""".r
	
	/*
	// REFACTOR: turn entity lookups into computations, somehow
	def run() {
		def step() {
			val entity_l = entityStatus_m.filter(_._2 == 0).toList.map(_._1)
			for (id <- entity_l) {
				(id match {
					case Rx2(t, k, f) => findEntity(t, k, f.split('.').toList)
					case Rx1(t, k) => findEntity(t, k, Nil)
					case _ => RqError(s"don't know how to find entity: `$id`")
				}) match {
					case e: RqError[_] =>
						entityStatus_m(id) = 2
						System.err.println(e)
					case RqSuccess(jsval, w) =>
						w.foreach(System.err.println)
						setEntity(id, jsval)
				}
			}
			val l = makePendingComputationList
			if (!l.isEmpty) {
				run(l)
				step()
			}
		}
		step()
		makeMessagesForMissingInputs()
	}
	*/
	
	private def findEntity(table: String, key: String, field_l: List[String]): RqResult[JsValue] = {
		val id = (s"$table[$key]" :: field_l).mkString(".")
		entity_m.get(id) match {
			case Some(jsval) => RqSuccess(jsval)
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
	
	/*
	private def run(node_l: List[Node_Computation]) {
		println("run")
		if (!node_l.isEmpty) {
			node_l.foreach(handleComputation)
			run(makePendingComputationList)
		}
	}
	
	private def makePendingComputationList: List[Node_Computation] = {
		status_m.filter(_._2 == 1).keys.toList.sorted(NodeOrdering)
	}
	
	private def handleComputation(node: Node_Computation) {
		println(s"try node ${node.name}")
		node.result match {
			case ComputationItem_Computation(entity_l, fn) =>
				val input_l: List[Object] = entity_l.map(entityObj_m)
				setComputationItem(node, fn(input_l))
			case _ =>
		}
	}
	*/
	
	private def makeMessagesForMissingInputs(): List[String] = {
		status_m.toList.flatMap(pair => {
			val (node, status) = pair
			node.input_l.filterNot(entityObj_m.contains).map(idfn => s"ERROR: missing entity `${idfn.id}` of class `${idfn.clazz}`")
		})
	}
	
	def getNodeList(): List[Node] = {
		def step(node: Node): List[Node] = {
			node :: children_m.getOrElse(node, Nil).flatMap(step)
		}
		step(root).tail
	}
	
	def getComputationList(): List[Node_Computation] = {
		getNodeList.collect { case n: Node_Computation => n }
	}
	
	def getTokenList(): List[Node_Token] = {
		getNodeList.collect { case n: Node_Token => n }
	}
	
	def getMessages(): List[String] = {
		val a = result_m.toList.collect({case pair@(n: HasComputationHierarchy, r) => (n.id, r)})
		a.sortBy(_._1).flatMap { pair =>
			val id = getIdString(pair._1) + ": "
			pair._2.getWarnings.map(id+"WARNING: "+_) ++ pair._2.getErrors.map(id+"ERROR: "+_)
		}
	}
	
	//def getIdString(node: Node): String = getIdString(node.id)
	def getIdString(id: List[Int]): String = id.mkString(".")

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
	
	implicit val NodeOrdering = new Ordering[HasComputationHierarchy] {
		def compare(a: HasComputationHierarchy, b: HasComputationHierarchy): Int = {
			ListIntOrdering.compare(a.id, b.id)
		}
	}
}

object Conversions {
	def toJsValue(jsval: JsValue): RqResult[JsValue] =
		RqSuccess(jsval)
	
	def toString(jsval: JsValue): RqResult[String] = {
		jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		}
	}
	
	private def makeConversion
			(fn: JsValue => RqResult[Object])
			(idclass: IdClass, jsval: JsValue): ConversionResult =
		fn(jsval).map(obj => List(ConversionItem_Object(idclass, obj)))

	val toString2 = makeConversion(toString) _
	
	def toInteger(jsval: JsValue): RqResult[Integer] = {
		jsval match {
			case JsNumber(n) => RqSuccess(n.toInt)
			case _ => RqError("expected JsNumber")
		}
	}

	val toInteger2 = makeConversion(toInteger) _
	def toInteger2(idclass: IdClass, jsval: JsValue): ConversionResult =
		toInteger(jsval).map(obj => List(ConversionItem_Object(idclass, obj)))
	
	private val RxVolume = """([0-9]*)(\.[0-9]*)?([mun]?l)""".r
	def toLiquidVolume(jsval: JsValue): RqResult[LiquidVolume] = {
		jsval match {
			case JsString(RxVolume(a,b,c)) =>
				val s = List(Option(a), Option(b)).flatten.mkString
				val n = BigDecimal(s)
				val v = c match {
					case "l" => LiquidVolume.l(n)
					case "ml" => LiquidVolume.ml(n)
					case "ul" => LiquidVolume.ul(n)
					case "nl" => LiquidVolume.nl(n)
					case _ => return RqError(s"invalid volume suffix '$c'")
				}
				RqSuccess(v)
			case JsNumber(n) => RqSuccess(LiquidVolume.l(n))
			case _ => RqError("expected JsString in volume format")
		}
	}
	
	def toJsObject(jsval: JsValue): RqResult[JsObject] =
		jsval match {
			case jsobj: JsObject => RqSuccess(jsobj)
			case _ => RqError("required a JsObject")
		}
	
	def toPlateModel(jsval: JsValue): RqResult[PlateModel] = {
		for {
			jsobj <- toJsObject(jsval)
			id <- getString('id, jsobj)
			rows <- getInteger('rows, jsobj)
			cols <- getInteger('cols, jsobj)
			wellVolume <- getVolume('wellVolume, jsobj)
		} yield {
			new PlateModel(id, rows, cols, wellVolume)
		}
	}
	
	def getString(symbol: Symbol, jsobj: JsObject): RqResult[String] = {
		jsobj.fields.get(symbol.name) match {
			case None => RqError("missing field `${symbol.name}`")
			case Some(jsval) => toString(jsval)
		}
	}
	
	def getInteger(symbol: Symbol, jsobj: JsObject): RqResult[Integer] = {
		jsobj.fields.get(symbol.name) match {
			case None => RqError("missing field `${symbol.name}`")
			case Some(jsval) => toInteger(jsval)
		}
	}
	
	def getVolume(symbol: Symbol, jsobj: JsObject): RqResult[LiquidVolume] = {
		jsobj.fields.get(symbol.name) match {
			case None => RqError("missing field `${symbol.name}`")
			case Some(jsval) => toLiquidVolume(jsval)
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
/*
object ApplicativeMain2 extends App {
	val cmd1 = JsonParser("""{ "cmd": "print", "text": "Hello, World!" }""").asJsObject
	val cmd2 = JsonParser("""{ "cmd": "print2", "number": 3 }""").asJsObject
	
	val p = new ProcessorData
	
	p.setEntity("a", JsString("1"))
	p.setEntity("b", JsString("2"))
	p.setEntity("c", JsString("3"))
	//p.setEntity("cmd[1].text", JsString("Hello, World"))
	//p.addComputation(cn1.entity_l, cn1.fn, Nil)
	//p.addComputation(cn2.entity_l, cn2.fn, Nil)
	val h1 = new PrintCommandHandler
	val h2 = new Print2CommandHandler
	//p.addCommand(cmd1, h1)
	p.setComputationItem(p.root, RqSuccess(List(
		ComputationItem_Command(cmd1, h1.getResult),
		ComputationItem_Command(cmd2, h2.getResult)
	)))
	
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
	p.getComputationList.map({ node =>
		node.result match {
			case _: ComputationItem_Computation =>
				p.getIdString(node)+": "+node.name
			case _ =>
		}
	}).foreach(println)

	println()
	println("Entities:")
	p.entity_m.toList.sortBy(_._1).foreach(pair => println(pair._1+": "+pair._2))
	
	println()
	println("Tokens:")
	p.getTokenList.map(_.token).foreach(println)
	
	println()
	println("Messages:")
	p.getMessages.foreach(println)
}
*/