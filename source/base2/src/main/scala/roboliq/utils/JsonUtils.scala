package roboliq.utils

import spray.json.JsValue
import com.google.gson.Gson
import roboliq.core.ResultC
import spray.json.JsObject
import spray.json.JsArray
import spray.json.JsNull

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
	
	def jsonToPrettyText(jsval: JsValue): String = {
		jsval.prettyPrint
	}
	
	def merge(v1: JsValue, v2: JsValue): ResultC[JsValue] = {
		(v1, v2) match {
			case (m1: JsObject, m2: JsObject) =>
				mergeMaps(m1, m2)
			case (a: JsArray, b: JsArray) =>
				ResultC.unit(JsArray(a.elements ++ b.elements))
			case _ => ResultC.unit(v2)
		}
	}
	
	def mergeMaps(m1: JsObject, m2: JsObject): ResultC[JsObject] = {
		val key_l = m1.fields.keySet ++ m2.fields.keySet
		for {
			merged_l <- ResultC.map(key_l) { key =>
				val value_? : ResultC[JsValue] = (m1.fields.get(key), m2.fields.get(key)) match {
					case (None, None) => ResultC.error(s"internal merge error on key $key") // Won't happen
					case (Some(a), None) => ResultC.unit(a)
					case (None, Some(b)) => ResultC.unit(b)
					case (Some(a), Some(b)) =>
						(a, b) match {
							case (ma: JsObject, mb: JsObject) =>
								for {
									//_ <- ResultC.check(ma.typ_? == mb.typ_?, s"Changing TYPE from ${ma.typ_?} to ${mb.typ_?}")
									mc <- merge(ma, mb)
								} yield mc
							case _ =>
								merge(a, b)
						}
				}
				value_?.map(key -> _)
			}
		} yield JsObject(merged_l.toMap)
	}
	
	def merge(value_l: Iterable[JsValue]): ResultC[JsValue] = {
		var a: JsValue = JsNull
		for {
			_ <- ResultC.foreach(value_l) { b =>
				for {
					c <- merge(a, b)
				} yield {
					a = c
				}
			}
		} yield a
	}
}
