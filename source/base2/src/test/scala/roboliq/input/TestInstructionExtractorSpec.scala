package roboliq.input

import org.scalatest.FunSpec
import roboliq.utils.JsonUtils
import roboliq.utils.MiscUtils
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsValue
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.NativeArray
import scala.collection.immutable.ListMap
import scala.collection.JavaConversions._

class TestInstructionExtractorSpec extends FunSpec {
	import roboliq.input.ResultCWrapper._

	val input1a = JsonUtils.yamlToJson("""
    objects:
      ourlab:
        type: Namespace
        mario:
          type: Namespace
          evoware: { type: EvowareRobot }
          arm1: { type: Transporter, evowareRoma: 0 }
          P2: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 2 }
          P3: { type: Site, evowareCarrier: "MP 2Pos H+P Shake", evowareGrid: 10, evowareSite: 4 }
        model1: { type: PlateModel, evowareName: Ellis Nunc F96 MicroWell }
""").asJsObject

	val input1b = JsonUtils.yamlToJson("""
    objects:
      plate1: { type: Plate }
    steps:
      1: {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P3}
      2: {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P2}
""").asJsObject

	val input1c = JsonUtils.yamlToJson("""
    objects:
      plate1: { model: ourlab.model1, location: ourlab.mario.P2 }
""").asJsObject

	val input1 = JsonUtils.merge(List(input1a, input1b, input1c)).run().value.asJsObject
	
	describe("Test some instruction extractor") {
		it("hmm") {
			val step_m = input1.fields("steps").asJsObject
			val key_l = step_m.fields.keys.toList.sortWith((s1, s2) => MiscUtils.compareNatural(s1, s2) < 0)
			val step_l0 = key_l.map(step_m.fields)
			val output = JsObject(input1.fields + ("steps" -> JsArray(step_l0)))
			println(output.prettyPrint)
		}
		it("mmm") {
			import org.mozilla.javascript._
			val cx = Context.enter()
			try {
				cx.setLanguageVersion(Context.VERSION_ES6)
				val scope = cx.initStandardObjects()
				println(cx.getLanguageVersion())
				//val result = cx.evaluateString(scope, """1+5;""", "<infile>", 0, null);
				//val result = cx.evaluateString(scope, """var x = {"name": "bob", "age": 12}; x;""", "<infile>", 0, null);
				val result = cx.evaluateString(scope, """var x = {"name": "bob", "age": 12}; x""", "<infile>", 0, null);
				println(result)
				val x = result.asInstanceOf[Scriptable]
				println(x.get("name", x))
				
val s1 = """
map = {
	"instruction.transporter.movePlate": {
		getEffects: function(params, objects) {
			var effects = {};
			effects[params.object+".location"] = params.destination;
			return effects;
		}
	},
	"action.transporter.movePlate": {
		getEffects: function(params, objects) {
			var effects = {};
			effects[params.object+".location"] = params.destination;
			return {effects: effects};
		},
		expand: function(params, objects) {
			var expansion = {};
			var cmd1 = {
				command: "instruction.transporter.movePlate",
				agent: params.agent,
				equipment: params.equipment,
				program: params.program,
				object: params.object,
				destination: params.destination
			};
			expansion["1"] = cmd1;
			return expansion;
		}
	}
};
run = function() { return 4; };
""";
				cx.evaluateString(scope, s1, "<infile>", 0, null);
				val f0 = scope.get("run", scope)
				val f = f0.asInstanceOf[Function]
				val result2 = f.call(cx, scope, scope, Array[Object]())
				println(result2)
				
				val result3 = cx.evaluateString(scope, """
						var params = {command: "instruction.transporter.movePlate", agent: "ourlab.mario.evoware", equipment: "ourlab.mario.arm1", program: "Narrow", object: "plate1", destination: "ourlab.mario.P3"};
						var effects = map["instruction.transporter.movePlate"].getEffects(params, null);
						effects;
					""", "<infile>", 0, null)
				println(toJsValue(result3))
				
				val result4 = cx.evaluateString(scope, """
						var params = {command: "instruction.transporter.movePlate", equipment: "ourlab.mario.arm1", program: "Narrow", object: "plate1", destination: "ourlab.mario.P3"};
						var expansion = map["action.transporter.movePlate"].expand(params, null);
						expansion;
					""", "<infile>", 0, null)
				println(toJsValue(result4))
			} finally { Context.exit() }
		}
		it("hhh") {
			val step_m = input1.fields("steps").asJsObject
			val key_l = step_m.fields.keys.toList.sortWith((s1, s2) => MiscUtils.compareNatural(s1, s2) < 0)
			val step_l0 = key_l.map(step_m.fields)
			val output = JsObject(input1.fields  + ("steps" -> JsArray(step_l0)))
			println(output.prettyPrint)
			import org.mozilla.javascript._
			val cx = Context.enter()
			try {
				cx.setLanguageVersion(Context.VERSION_ES6)
				val scope = cx.initStandardObjects()
			} finally { Context.exit() }
		}
	}

	private def toJsValue(input: Any): JsValue = {
		import spray.json._
		import org.mozilla.javascript._
		input match {
			case b: Boolean => JsBoolean(b)
			case i: Int => JsNumber(i)
			case l: Long => JsNumber(l)
			case f: Float => JsNumber(f)
			case d: Double => JsNumber(d)
			case s: String => JsString(s)

			case o: NativeObject => toJsObject(o)
			case a: NativeArray => toJsArray(a)
			case w: Wrapper => toJsValue(w.unwrap())

			case u: Undefined => JsNull
			case null => JsNull
			case other@_ => {
				println("Cannot convert '%s' to a JsValue. Returning None.".format(other))
				JsNull
			}
		}
	}
	
	private def toJsObject(nativeObject: NativeObject): JsObject = {
		val tuples = nativeObject.entrySet.toList.map(entry => (entry.getKey.toString, toJsValue(entry.getValue)))
		new JsObject(ListMap(tuples: _*))
	}

	private def toJsArray(nativeArray: NativeArray): JsArray = {
		new JsArray(nativeArray.iterator().map(item => toJsValue(item)).toList)
	}
}