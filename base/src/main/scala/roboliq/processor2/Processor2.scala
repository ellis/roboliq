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

sealed trait ComputationResult
case class ComputationResult_Event(event: Event) extends ComputationResult
case class ComputationResult_EntityRequest(id: String) extends ComputationResult
case class ComputationResult_Computation(
	entity_l: List[IdClass],
	fn: (List[Object]) => RqResult[List[ComputationResult]]
) extends ComputationResult
case class ComputationResult_Command(cmd: JsObject, fn: RqResult[List[ComputationResult]]) extends ComputationResult
case class ComputationResult_Token(token: Token) extends ComputationResult
case class ComputationResult_Entity(id: String, jsval: JsValue) extends ComputationResult
case class ComputationResult_Object(idclass: IdClass, obj: Object) extends ComputationResult

/*case class ComputationSpec(
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationResult]]
)

case class Computation(
	id_r: List[Int],
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationResult]]
) {
	val id: List[Int] = id_r.reverse
}

case class ComputationNode(
	computation: Computation
)*/

class Event
class Command
trait Token

case class Token_Comment(s: String) extends Token

class Node(val parent: Node, val index: Int) {
	lazy val id_r = getId_r
	lazy val id = id_r.reverse
	
	private def getId: List[Int] = {
		getId_r.reverse
	}
	
	private def getId_r: List[Int] = {
		if (parent == null)
			Nil
		else
			index :: parent.getId
	}
}

class Node_Result(
	parent: Node,
	index: Int,
	val result: ComputationResult
) extends Node(parent, index)

class Node_Computation(
	val name: String,
	parent: Node,
	index: Int,
	val result: ComputationResult_Computation,
	val idCmd: List[Int]
) extends Node(parent, index)

class Node_Token(
	parent: Node,
	index: Int,
	val token: Token
) extends Node(parent, index)

class ProcessorData {
	// key is a Computation.id_r
	val root = new Node_Computation("root", null, 0, ComputationResult_Computation(Nil, (_: List[Object]) => RqError("hmm")), Nil)
	val rootObj = new Node_Computation("rootObj", null, 0, ComputationResult_Computation(Nil, (_: List[Object]) => RqError("hmm")), Nil)
	val message_m = new HashMap[Node, List[String]]
	val children_m = new HashMap[Node, List[Node]]
	val status_m = new HashMap[Node_Computation, Int]
	val dep_m: MultiMap[String, Node_Computation] = new HashMap[String, mutable.Set[Node_Computation]] with MultiMap[String, Node_Computation]
	val entity_m = new HashMap[String, JsValue]
	val entityStatus_m = new HashMap[String, Int]
	//val entityFind_l = mutable.Set[IdClass]()
	//val entityMissing_l = mutable.Set[IdClass]()
	val cmdToJs_m = new HashMap[List[Int], JsObject]
	val entityObj_m = new HashMap[IdClass, Object]
	val idclassNode_m = new HashMap[IdClass, Node_Computation]
	
	val conversion_m = new HashMap[Class[_], (IdClass, JsValue) => HandlerResult]
	conversion_m(classOf[String]) = Conversions.toString2
	conversion_m(classOf[Integer]) = Conversions.toInteger2
	
	def setComputationResult(node: Node_Computation, result: HandlerResult) {
		val child_l: List[Node] = result match {
			case RqSuccess(l, warning_l) =>
				setMessages(node, warning_l)
				l.zipWithIndex.map { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ComputationResult_Command(cmd, fn) =>
							val idCmd = node.id ++ List(index)
							val idCmd_s = getIdString(idCmd)
							val idEntity = s"cmd[${idCmd_s}]"
							cmdToJs_m(idCmd) = cmd
							setEntity(idEntity, cmd)

							val result = ComputationResult_Computation(Nil, (_: List[Object]) => fn)
							new Node_Computation("cmd: "+cmd.fields.getOrElse("cmd", "<MISSING>"), node, index, result, idCmd)
						case result: ComputationResult_Computation =>
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
							val result2 = result.copy(entity_l = entity2_l)
							new Node_Computation(result2.entity_l.toString, node, index, result2, node.idCmd)
						case ComputationResult_Token(token) =>
							new Node_Token(node, index + 1, token)
						case ComputationResult_Entity(id, jsval) =>
							setEntity(id, jsval)
							new Node_Result(node, index, r)
						case ComputationResult_Object(idclass, obj) =>
							setEntityObj(idclass, obj)
							new Node_Result(node, index, r)
						case _ =>
							new Node_Result(node, index, r)
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
						val result = ComputationResult_Computation(List(IdClass(idclass.id, classOf[JsValue])), (l: List[Object]) => l match {
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
		println(child_l, child2_l)
		setChildren(node, child2_l)
		status_m(node) = 2
	}
	
	private def setMessages(node: Node, message_l: List[String]) {
		if (message_l.isEmpty)
			message_m -= node
		else
			message_m(node) = message_l
	}
	
	private def setChildren(node: Node, child_l: List[Node]) {
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
				//case ComputationResult_EntityRequest(idEntity) =>
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
	private def addDependencies(node: Node_Computation) {
		node.result.entity_l.foreach(idclass => {
			dep_m.addBinding(idclass.id, node)
			if (!entityStatus_m.contains(idclass.id))
				entityStatus_m(idclass.id) = 0
		})
	}
	
	private def updateComputationStatus(node: Node_Computation) {
		status_m(node) = if (node.result.entity_l.forall(entityObj_m.contains)) 1 else 0
	}

	// Match "tbl[key]"
	private val Rx1 = """^([a-zA-Z]+)\[([0-9.]+)\]$""".r
	// Match "tbl[key].field"
	private val Rx2 = """^([a-zA-Z]+)\[([0-9.]+)\]\.(.+)$""".r
	
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
			case ComputationResult_Computation(entity_l, fn) =>
				val input_l: List[Object] = entity_l.map(entityObj_m)
				setComputationResult(node, fn(input_l))
			case _ =>
		}
	}
	
	private def makeMessagesForMissingInputs() {
		for ((node, status) <- status_m if status == 0) {
			node.result match {
				case ComputationResult_Computation(entity_l, _) =>
					val message_l = entity_l.filterNot(entityObj_m.contains).map(idfn => s"ERROR: missing entity `${idfn.id}` of class `${idfn.clazz}`")
					setMessages(node, message_l) 
				case _ =>
			}
		}
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
		message_m.toList.sortBy(_._1).flatMap { pair =>
			val id = getIdString(pair._1)
			pair._2.map(s => s"$id: $s")
		}
	}
	
	def getIdString(node: Node): String = getIdString(node.id)
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
	
	implicit val NodeOrdering = new Ordering[Node] {
		def compare(a: Node, b: Node): Int = {
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
			(idclass: IdClass, jsval: JsValue): HandlerResult =
		fn(jsval).map(obj => List(ComputationResult_Object(idclass, obj)))

	val toString2 = makeConversion(toString) _
	
	def toInteger(jsval: JsValue): RqResult[Integer] = {
		jsval match {
			case JsNumber(n) => RqSuccess(n.toInt)
			case _ => RqError("expected JsNumber")
		}
	}

	val toInteger2 = makeConversion(toInteger) _
	def toInteger2(idclass: IdClass, jsval: JsValue): HandlerResult =
		toInteger(jsval).map(obj => List(ComputationResult_Object(idclass, obj)))
	
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
	
	val p = new ProcessorData
	
	p.setEntity("a", JsString("1"))
	p.setEntity("b", JsString("2"))
	p.setEntity("c", JsString("3"))
	//p.setEntity("cmd[1].text", JsString("Hello, World"))
	//p.addComputation(cn1.entity_l, cn1.fn, Nil)
	//p.addComputation(cn2.entity_l, cn2.fn, Nil)
	val h1 = new PrintCommandHandler
	//p.addCommand(cmd1, h1)
	p.setComputationResult(p.root, RqSuccess(List(ComputationResult_Command(cmd1, h1.getResult))))
	
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
			case _: ComputationResult_Computation =>
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
