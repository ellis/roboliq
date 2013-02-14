package roboliq.processor2

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
import roboliq.commands2.arm.MovePlateHandler
import roboliq.commands2.pipette.AspirateCmd

class PrintCommandHandler extends CommandHandler("print") { 
	val fnargs =
		fnRequire (as[String]('text)) { (text) =>
			fnReturn(Token_Comment(text))
		}
}

class Print2CommandHandler extends CommandHandler("print2") {
	val fnargs =
		fnRequire (as[Integer]('number)) { (n) =>
			fnReturn(Token_Comment(n.toString))
		}
}

case class Test(id: String)

class TestCommandHandler extends CommandHandler("test") {
	val fnargs =
		cmdAs[AspirateCmd] { cmd =>
			fnReturn(Token_Comment(cmd.toString))
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
	val config = List[(String, JsValue)](
		"tipModel" -> JsonParser("""{ "id": "Standard 50ul", "volume": "45ul", "volumeMin": "0.01ul" }"""),
		"tipModel" -> JsonParser("""{ "id": "Standard 1000ul", "volume": "950ul", "volumeMin": "4ul" }"""),
		
		"tip" -> JsonParser("""{ "id": "TIP1", "index": 0, "model": "Standard 1000ul" }"""),
		"tip" -> JsonParser("""{ "id": "TIP2", "index": 1, "model": "Standard 1000ul" }"""),
		"tip" -> JsonParser("""{ "id": "TIP3", "index": 2, "model": "Standard 1000ul" }"""),
		"tip" -> JsonParser("""{ "id": "TIP4", "index": 3, "model": "Standard 1000ul" }"""),
		"tip" -> JsonParser("""{ "id": "TIP5", "index": 4, "model": "Standard 50ul" }"""),
		"tip" -> JsonParser("""{ "id": "TIP6", "index": 5, "model": "Standard 50ul" }"""),
		"tip" -> JsonParser("""{ "id": "TIP7", "index": 6, "model": "Standard 50ul" }"""),
		"tip" -> JsonParser("""{ "id": "TIP8", "index": 7, "model": "Standard 50ul" }""")
	)
	
	val cmd1 = JsonParser("""{ "cmd": "print", "text": "Hello, World!" }""").asJsObject
	val cmd2 = JsonParser("""{ "cmd": "print2", "number": 3 }""").asJsObject
	val cmd3 = JsonParser("""{ "cmd": "movePlate", "plate": "P1", "dest": "cooled2", "deviceId": "ROMA2" }""").asJsObject
	val cmd4 = JsonParser("""{ "cmd": "test", "description": "my command", "items": [{"&tip": "TIP1", "well": "P1(A1)", "volume": "50ul", "policy": "Wet"}]}""").asJsObject
	
	val h1 = new PrintCommandHandler
	val h2 = new Print2CommandHandler
	val h3 = new MovePlateHandler
	val h4 = new TestCommandHandler

	/*
	case class B(name: String)
	case class C(s: String, n: Integer, l: List[Integer])//, bs: List[B])
	case class D(bs: List[B])
	case class E(s: String, n: Integer, l: List[Integer], bs: List[B])
	//{
	import scala.reflect.runtime.universe._
	import scala.reflect.runtime.{currentMirror => cm}

	import ConversionsDirect.conv
		
	println(conv(JsString("hello"), typeOf[String]))
	println(conv(JsonParser("""["a", "b", "c"]"""), typeOf[List[String]]))
	println(conv(JsonParser("""[1, 2, 3]"""), typeOf[List[Integer]]))
	println(conv(JsonParser("""[1, 2, 3]"""), typeOf[List[Int]]))
	println(conv(JsonParser("""{"name": "Ellis"}"""), typeOf[B]))
	println(conv(JsonParser("""{"s": "String", "n": 42, "l": [1,2,3]}"""), typeOf[C]))
	println(conv(JsonParser("""{"bs": []}"""), typeOf[D]))
	println(conv(JsonParser("""{"s": "String", "n": 42, "l": [1,2,3], "bs": []}"""), typeOf[E]))
	println(conv(JsonParser("""{"s": "String", "n": 42, "l": [1,2,3], "bs": [{"name": "Howard"}]}"""), typeOf[E]))
	println(RqFunctionHandler.convLookup(JsonParser("""{"description": "my command", "items": [{"tip": "TIP1", "well": "P1(A1)", "volume": "50ul", "policy": "Wet"}]}"""), typeOf[roboliq.commands2.pipette.AspirateCmd]))
	println(RqFunctionHandler.convLookup(JsonParser("""{"&description": "my command", "items": [{"tip": "TIP1", "well": "P1(A1)", "volume": "50ul", "policy": "Wet"}]}"""), typeOf[roboliq.commands2.pipette.AspirateCmd]))
	println(RqFunctionHandler.convLookup(JsonParser("""{"&tip": "TIP1", "well": "P1(A1)", "volume": "50ul", "policy": "Wet"}"""), typeOf[roboliq.commands2.pipette.SpirateCmdItem]))
	println(RqFunctionHandler.convLookup(JsonParser("""{"&description": "my command", "items": [{"&tip": "TIP1", "well": "P1(A1)", "volume": "50ul", "policy": "Wet"}]}"""), typeOf[roboliq.commands2.pipette.AspirateCmd]))
	//println(conv(JsonParser("""{"&description": "my command", "items": [{"&tip": "TIP1", "well": "P1(A1)", "volume": "50ul", "policy": "Wet"}]}"""), typeOf[roboliq.commands2.pipette.AspirateCmd], List("Looked up 1", "Looked up 2")))
	sys.exit()
	*/
	
	val p = new ProcessorData(List(h1, h2, h3, h4))
	
	config.foreach(pair => {
		val (table, jsval) = pair
		val jsobj = jsval.asJsObject
		val key = jsobj.fields("id").asInstanceOf[JsString].value
		val tkp = TKP(table, key, Nil)
		p.setEntity(tkp, Nil, jsval)
	})
	
	p.setEntity(TKP("plateModel", "PCR", Nil), Nil, JsonParser("""{ "id": "PCR", "rows": 8, "cols": 12, "wellVolume": "100ul" }"""))
	p.setEntity(TKP("plateLocation", "cooled1", Nil), Nil, JsonParser("""{ "id": "cooled1", "plateModels": ["PCR"], "cooled": true }"""))
	p.setEntity(TKP("plateLocation", "cooled2", Nil), Nil, JsonParser("""{ "id": "cooled2", "plateModels": ["PCR"], "cooled": true }"""))
	//p.setEntity(TKP("plateLocation", "cooled1", Nil), Nil, JsonParser("""{ "id": "cooled1", "plateModels": "PCR", "cooled": true }"""))
	//p.setEntity(TKP("plateLocation", "cooled2", Nil), Nil, JsonParser("""{ "id": "cooled2", "plateModels": "PCR", "cooled": true }"""))
	p.setEntity(TKP("plate", "P1", Nil), Nil, JsonParser("""{ "id": "P1", "idModel": "PCR" }"""))
	p.setEntity(TKP("plateState", "P1", Nil), List(0), JsonParser("""{ "id": "P1", "location": "cooled1" }"""))
	p.setEntity(TKP("test", "T1", Nil), List(0), JsonParser("""{ "id": "T1" }"""))
	//p.setCommands(List(cmd1, cmd2, cmd3))
	p.setCommands(List(cmd4))
	
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
		case node: Node_Command => p.getIdString(node.path)+":"
		case node: Node_Computation => p.getIdString(node.path)+": "+node.input_l
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
