package roboliq.processor

import spray.json._

import roboliq.core._
import roboliq.core.RqPimper._


class Environment(
	val obj_m: Map[String, String],
	val cmd_m: Map[String, String],
	val table_m: Map[String, Map[String, JsObject]]
) {
	def lookupObject(table: String, key: String): RqResult[JsObject] = {
		for {
			key_m <- table_m.get(table).asRq(s"table not found: `$table`")
			jsobj <- key_m.get(key).asRq(s"key not found: `$key` in table `$table`")
		} yield jsobj
	}
	
	def lookupField(table: String, key: String, field: String): RqResult[JsValue] = {
		for {
			jsobj <- lookupObject(table, key)
			jsfield <- jsobj.fields.get(field).asRq(s"field not found: `$field` of key `$key` in table `$table`")
		} yield jsfield
	}
}
