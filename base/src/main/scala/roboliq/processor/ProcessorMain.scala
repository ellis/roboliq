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
import roboliq.commands.pipette.low.AspirateCmd

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

case class Test(tip: Tip)

class TestCommandHandler extends CommandHandler("test") {
	val fnargs =
		cmdAs[Test] { cmd =>
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
	//val config = List[(String, JsValue)](
	//	"tipModel" -> JsonParser("""{ "id": "Standard 50ul", "volume": "45ul", "volumeMin": "0.01ul" }"""),
	//	"tipModel" -> JsonParser("""{ "id": "Standard 1000ul", "volume": "950ul", "volumeMin": "4ul" }"""),
	val config = JsonParser(
"""{
"tipModel": [
	{ "id": "Standard 50ul", "volume": "45ul", "volumeMin": "0.01ul" },
	{ "id": "Standard 1000ul", "volume": "950ul", "volumeMin": "4ul" }
],
"tip": [
	{ "id": "TIP1", "index": 0, "permanent": "Standard 1000ul" },
	{ "id": "TIP2", "index": 1, "permanent": "Standard 1000ul" },
	{ "id": "TIP3", "index": 2, "permanent": "Standard 1000ul" },
	{ "id": "TIP4", "index": 3, "permanent": "Standard 1000ul" },
	{ "id": "TIP5", "index": 4, "permanent": "Standard 50ul" },
	{ "id": "TIP6", "index": 5, "permanent": "Standard 50ul" },
	{ "id": "TIP7", "index": 6, "permanent": "Standard 50ul" },
	{ "id": "TIP8", "index": 7, "permanent": "Standard 50ul" }
],
"plateModel": [
	{ "id": "Reagent Cooled 8*50ml", "rows": 8, "cols": 1, "wellVolume": "50ml" },
	{ "id": "Reagent Cooled 8*15ml", "rows": 8, "cols": 1, "wellVolume": "15ml" },
	{ "id": "Block 20Pos 1.5 ml Eppendorf", "rows": 4, "cols": 5, "wellVolume": "1.5ml" },
	{ "id": "D-BSSE 96 Well PCR Plate", "rows": 8, "cols": 12, "wellVolume": "200ul" },
	{ "id": "D-BSSE 96 Well Costar Plate", "rows": 8, "cols": 12, "wellVolume": "350ul" },
	{ "id": "D-BSSE 96 Well DWP", "rows": 8, "cols": 12, "wellVolume": "1000ul" },
	{ "id": "Trough 100ml", "rows": 8, "cols": 1, "wellVolume": "100ul" },
	{ "id": "Ellis Nunc F96 MicroWell", "rows": 8, "cols": 12, "wellVolume": "400ul" }
],
"plateLocation": [
	{ "id": "trough1", "plateModels": ["Trough 100ml"] },
	{ "id": "trough2", "plateModels": ["Trough 100ml"] },
	{ "id": "trough3", "plateModels": ["Trough 100ml"] },
	{ "id": "reagents15000", "plateModels": ["Reagent Cooled 8*15ml"], "cooled": true },
	{ "id": "uncooled2_low", "plateModels": ["D-BSSE 96 Well DWP", "Ellis Nunc F96 MicroWell"] },
	{ "id": "uncooled2_high", "plateModels": ["D-BSSE 96 Well Costar Plate"] },
	{ "id": "shaker", "plateModels": ["D-BSSE 96 Well Costar Plate", "D-BSSE 96 Well DWP"] },
	{ "id": "cooled1", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled2", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled3", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled4", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "cooled5", "plateModels": ["D-BSSE 96 Well PCR Plate"], "cooled": true },
	{ "id": "regrip", "plateModels": ["D-BSSE 96 Well PCR Plate", "D-BSSE 96 Well Costar Plate"] },
	{ "id": "reader", "plateModels": ["D-BSSE 96 Well Costar Plate"] }
],
"tubeModel": [
	{ "id": "Tube 50000ul", "volume": "50000ul" },
	{ "id": "Tube 15000ul", "volume": "15000ul" },
	{ "id": "Tube 1500ul", "volume": "1500ul" }
],
"tubeLocation": [
	{ "id": "reagents50", "tubeModels": ["Tube 50000ul"], "rackModel": "Reagent Cooled 8*50ml" },
	{ "id": "reagents15000", "tubeModels": ["Tube 15000ul"], "rackModel": "Reagent Cooled 8*15ml" },
	{ "id": "reagents1.5", "tubeModels": ["Tube 1500ul"], "rackModel": "Block 20Pos 1.5 ml Eppendorf" }
],
"substance": [
	{ "id": "water", "kind": "liquid", "physicalProperties": "Water", "cleanPolicy": {"enter": "Thorough", "within": "None", "exit": "Light"}} 
],
"plate": [
	{ "id": "reagents50", "model": "Reagent Cooled 8*50ml", "location": "reagents50" },
	{ "id": "reagents15000", "model": "Reagent Cooled 8*15ml", "location": "reagents15000" },
	{ "id": "reagents1.5", "model": "Block 20Pos 1.5 ml Eppendorf", "location": "reagents1.5" },
	{ "id": "P1", "model": "D-BSSE 96 Well PCR Plate" }
],
"vessel": [
	{ "id": "T1", "tubeModel": "Tube 15000ul" },
	{ "id": "P1(A01)" }
],

"plateState": [
	{ "id": "reagents15000", "location": "reagents15000" },
	{ "id": "P1", "location": "cooled1" }
],
"vesselState": [
	{ "id": "T1", "content": { "idVessel": "T1" } },
	{ "id": "P1(A01)", "content": { "idVessel": "T1", "solventToVolume": { "water": "100ul" } } }
],
"vesselSituatedState": [
	{ "id": "T1", "position": { "plate": "reagents15000", "index": 0 } },
	{ "id": "P1(A01)", "position": { "plate": "P1", "index": 0 } }
]
}""").asJsObject
	
	val cmd1 = JsonParser("""{ "cmd": "print", "text": "Hello, World!" }""").asJsObject
	val cmd2 = JsonParser("""{ "cmd": "print2", "number": 3 }""").asJsObject
	val movePlate = JsonParser("""{ "cmd": "arm.movePlate", "&plate": "P1", "&dest": "cooled2", "deviceId": "ROMA2" }""").asJsObject
	val aspirate = JsonParser("""{ "cmd": "pipetter.aspirate", "items": [{"tip": "TIP1", "well": "P1(A01)", "volume": "50ul", "policy": { "id": "Wet", "pos": "WetContact" }}] }""").asJsObject
	//val cmd4 = JsonParser("""{ "cmd": "test", "description": "my command", "items": [{"&tip": "TIP1", "well": "P1(A1)", "volume": "50ul", "policy": "Wet"}]}""").asJsObject
	val cmd4 = JsonParser("""{ "cmd": "test", "tip": "TIP1"}""").asJsObject
	
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
	
	val p = new ProcessorData(List(
		new PrintCommandHandler,
		new Print2CommandHandler,
		new MovePlateHandler,
		new roboliq.commands.pipette.low.AspirateHandler,
		new TestCommandHandler
	))
	
	p.loadJsonData(config)

	//p.setEntity(TKP("test", "T1", Nil), List(0), JsonParser("""{ "id": "T1" }"""))
	//p.setCommands(List(cmd1, cmd2, cmd3))
	p.setCommands(List(aspirate))
	//p.setCommands(List(cmd4))
	
	//println(p.db.get(TKP("plate", "P1", Nil)))
	p.run(17)

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
