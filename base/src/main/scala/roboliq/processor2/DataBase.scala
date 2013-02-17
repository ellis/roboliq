package roboliq.processor2

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection._
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.math.Ordering
import scalaz._
import roboliq.core._
import RqPimper._
import spray.json._

case class TKP(table: String, key: String, path: List[String]) {
	def id = (s"$table[$key]" :: path).mkString(".")
	def toList = table :: key :: path
}

class DataBase {
	private val js_m = new HashMap[TKP, HashMap[List[Int], JsValue]]
	private val watch_l = mutable.Set[TKP]()
	private val change_l = mutable.Set[TKP]()
	private val children_m = new HashMap[TKP, mutable.Set[TKP]] 
	
	def addWatch(tkp: TKP) {
		watch_l += tkp
	}
	
	/**
	 * Return the accumulated list of changes, clearing the internal list.
	 */
	def popChanges(): List[TKP] = {
		val l = change_l.toList
		change_l.clear
		l
	}
	
	def set(tkp: TKP, time: List[Int], jsval: JsValue) {
		jsval match {
			case jsobj: JsObject =>
				if (jsobj.fields.isEmpty) {
					js_m.getOrElseUpdate(tkp, new HashMap[List[Int], JsValue]())(time) = jsval
					registerChild(tkp)
					handleChange(tkp)
				}
				else {
					jsobj.fields.foreach(pair => set(tkp.copy(path = tkp.path ++ List(pair._1)), time, pair._2))
				}
			case _ =>
				js_m.getOrElseUpdate(tkp, new HashMap[List[Int], JsValue]())(time) = jsval
				registerChild(tkp)
				handleChange(tkp)
		}
	}
	
	/**
	 * Register the tkp as a child of its parent, recursively.
	 */
	private def registerChild(tkp: TKP) {
		if (!tkp.path.isEmpty) {
			val tkpParent = tkp.copy(path = tkp.path.init)
			val child_l = children_m.getOrElseUpdate(tkpParent, mutable.Set[TKP]())
			if (!child_l.contains(tkp)) {
				children_m.getOrElseUpdate(tkpParent, mutable.Set[TKP]()) += tkp
				registerChild(tkpParent)
			}
		}
	}
	
	/**
	 * Register value change for watched tkps (recursively).
	 */
	private def handleChange(tkp: TKP) {
		if (watch_l.contains(tkp))
			change_l += tkp
		if (!tkp.path.isEmpty)
			handleChange(tkp.copy(path = tkp.path.init))
	}
	
	def getBefore(tkp: TKP, time: List[Int] = Nil): RqResult[JsValue] = {
		def fn(time_l: List[List[Int]]): Int =
			time_l.indexWhere(ListIntOrdering.compare(_, time) >= 0) - 1
		getWith(tkp, time, fn _)
	}
	
	def getAt(tkp: TKP, time: List[Int]): RqResult[JsValue] = {
		def fn(time_l: List[List[Int]]): Int =
			time_l.indexWhere(ListIntOrdering.compare(_, time) >= 0)
		getWith(tkp, time, fn _)
	}
	
	def get(tkp: TKP): RqResult[JsValue] = getAt(tkp, Nil)
	
	private def getWith(tkp: TKP, time: List[Int], fn: (List[List[Int]]) => Int): RqResult[JsValue] = {
		js_m.get(tkp) match {
			// Not a JsValue, maybe a JsObject?
			case None =>
				children_m.get(tkp) match {
					case None => RqError[JsValue](s"didn't find data for `$tkp`")
					case Some(child_l) =>
						val result = child_l.toList.map(tkp => get(tkp).map(tkp.path.last -> _))
						RqResult.toResultOfList(result).map { pair_l =>
							JsObject(pair_l.toMap)
						}
				}
				
			case Some(timeToValue_m) =>
				get(timeToValue_m, time, fn).asRq(s"didn't find data for `$tkp`" + (if (time.isEmpty) "" else " @ " +time.mkString("/")))
		}
	}

	private def get(timeToValue_m: Map[List[Int], JsValue], time: List[Int], fn: List[List[Int]] => Int): Option[JsValue] = {
		if (timeToValue_m.size == 0) {
			None
		}
		else {
			val time_l = timeToValue_m.keys.toList.sorted(ListIntOrdering)
			val i0 = fn(time_l)
			val i = if (i0 >= 0) i0 else time_l.size - 1
			Some(timeToValue_m(time_l(i)))
		}
	}
	
	/*
	
	// The JsValues (but no JsObjects) in the database
	private val js_m = new HashMap[List[Int], HashMap[String, JsValue]]
	// All known fields for a JsObject (time independent)
	private val fields_m = new HashMap[String, mutable.Set[String]]
	// Cached conversions to non-Js objects
	private val obj_m = new HashMap[List[Int], HashMap[String, Object]]
	// Time points which have been registered
	private val time_l = mutable.SortedSet[List[Int]]()(ListIntOrdering)
	private var time2_l: Seq[List[Int]] = Nil
	
	def set(key: DataKey, jsval: JsValue) {
		jsval match {
			case jsobj: JsObject =>
				fields_m(key.idWithoutTime, )
				jsobj.fields.foreach(pair => set(key.copy(path = key.path ++ List(pair._1)), pair._2))
			case _ =>
				js_m.getOrElseUpdate(key.time, new HashMap[String, JsValue])(key.idWithoutTime) = jsval
				if (!time_l.contains(key.time)) {
					time_l += key.time
					time2_l = time_l.toSeq
				}
		}
	}

	def get(key: DataKey): RqResult[JsValue] = {
		val i0 = time2_l.indexWhere(ListIntOrdering.compare(_, key.time) >= 0)
		var i = if (i0 > 0) i0 else time_l.size - 1
		val id = key.idWithoutTime
		
		while (i >= 0) {
			val time = time2_l(i)
			val m2 = js_m(time)
			if (m2.contains(id))
				return RqSuccess(m2(id))
			else {
				val jsobj_? = getJsObject(id, )
			}
			i -= 1
		}
		
		return RqError[JsValue](s"didn't find data for `$id`")
	}
	
	private def makeJsObject(key: DataKey): RqResult[JsObject] = {
		val i0 = time2_l.indexWhere(ListIntOrdering.compare(_, key.time) >= 0)
		var i = if (i0 > 0) i0 else time_l.size - 1
		val id = key.idWithoutTime
		
		while (i >= 0) {
			val time = time2_l(i)
			val m2 = js_m(time)
			if (m2.contains(id))
				return RqSuccess(m2(id))
			else {
				val field_l = getFields(id, m2)
				if (!field_l.isEmpty) {
					
				}
			}
			i -= 1
		}
		
		return RqError[JsValue](s"didn't find data for `$id`")
	}
	
	private def getFields(id: String, m2: HashMap[String, JsValue]): Iterable[JsValue] = {
		val id2 = id + "."
		m2.filterKeys(_ startsWith id2).values
	}
	
	def setObj(kc: KeyClass, obj: Object): RqResult[Unit] = {
		if (kc.clazz == classOf[JsValue]) {
			set(kc.key, obj.asInstanceOf[JsValue])
			RqSuccess(())
		}
		else {
			for {
				jsval <- get(kc.key)
			} yield {
				obj_m((jsval, kc.clazz)) = obj
			}
		}
	}
	
	def getObj(kc: KeyClass): RqResult[Object] = {
		if (kc.clazz == classOf[JsValue]) {
			get(kc.key)
		}
		else {
			for {
				jsval <- get(kc.key)
				obj <- obj_m.get((jsval, kc.clazz)).asRq("no object of `${kc.clazz}` found for `$key`")
			} yield obj
		}
	}
	
	def contains(kc: KeyClass): Boolean = {
		if (kc.clazz == classOf[JsValue]) {
		}
	}
	*/
	
	override def toString(): String = {
		js_m.toList.sortBy(_._1.toList)(ListStringOrdering).map(pair => {
			val (tkp, timeToValue_m) = pair
			s"${tkp.id} " +
				(if (watch_l.contains(tkp)) "*" else "") +
				(if (change_l.contains(tkp)) "!" else "") +
				": " +
				timeToValue_m.toList.map(pair => pair._2.toString + (if (pair._1.isEmpty) "" else "@" + pair._1.mkString("."))).mkString("; ")
		}).mkString("\n")
	}
}
