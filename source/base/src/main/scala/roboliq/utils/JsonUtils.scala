package roboliq.utils

import spray.json.JsValue
import com.google.gson.Gson

object JsonUtils {
	def textToJson(s: String): JsValue = {
		import spray.json._
		s.parseJson
	}

	def yamlToJson(s: String): JsValue = {
		val s1 = yamlToJsonText(s)
		textToJson(s1)
	}
	
	def yamlToJsonText(s: String): String = {
		import org.yaml.snakeyaml._
		val yaml = new Yaml()
		//val o = yaml.load(s).asInstanceOf[java.util.Map[String, Object]]
		val o = yaml.load(s)
		val gson = new Gson
		val s_~ = gson.toJson(o)
		//println("gson: " + s_~)
		s_~
	}
}
