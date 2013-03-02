package roboliq.processor

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection._
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.math.Ordering
import scalaz._
import grizzled.slf4j.Logger
import roboliq.core._
import RqPimper._
import spray.json._

case class TKP(table: String, key: String, path: List[String]) {
	def id = (s"$table[$key]" :: path).mkString(".")
	def toList = table :: key :: path
}

class DataBase {
	private val logger = Logger[this.type]

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
	
	def setAt(tkp: TKP, time: List[Int], jsval: JsValue) {
		jsval match {
			case jsobj: JsObject =>
				if (jsobj.fields.isEmpty) {
					js_m.getOrElseUpdate(tkp, new HashMap[List[Int], JsValue]())(time) = jsval
					registerChild(tkp)
					handleChange(tkp)
				}
				else {
					jsobj.fields.foreach(pair => setAt(tkp.copy(path = tkp.path ++ List(pair._1)), time, pair._2))
				}
			case _ =>
				js_m.getOrElseUpdate(tkp, new HashMap[List[Int], JsValue]())(time) = jsval
				registerChild(tkp)
				handleChange(tkp)
		}
	}
	
	def set(tkp: TKP, jsval: JsValue) = setAt(tkp, Nil, jsval)
	
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
	
	def getBefore(tkp: TKP, time: List[Int]): RqResult[JsValue] = {
		logger.trace(s"getBefore($tkp, $time)")
		assert(!time.isEmpty)
		def chooseTime(time_l: List[List[Int]]): Option[List[Int]] =
			time_l.takeWhile(ListIntOrdering.compare(_, time) < 0).lastOption
		getWith(tkp, time, chooseTime _)
	}
	
	def getAt(tkp: TKP, time: List[Int]): RqResult[JsValue] = {
		logger.trace(s"getAt($tkp, $time)")
		def fnChooseTime(time_l: List[List[Int]]): Option[List[Int]] =
			time_l.takeWhile(ListIntOrdering.compare(_, time) <= 0).lastOption
		getWith(tkp, time, fnChooseTime _)
	}
	
	def get(tkp: TKP): RqResult[JsValue] = getAt(tkp, Nil)

	def get(table: String, key: String, path: List[String] = Nil): RqResult[JsValue] = get(TKP(table, key, path))
	
	private def getWith(tkp: TKP, time: List[Int], fnChooseTime: (List[List[Int]]) => Option[List[Int]]): RqResult[JsValue] = {
		logger.trace(s"getWith($tkp, $time)")
		js_m.get(tkp) match {
			// Not a JsValue, maybe a JsObject?
			case None =>
				children_m.get(tkp) match {
					case None =>
						//println("js_m: "+js_m)
						//println("children_m: "+children_m)
						RqError[JsValue](s"didn't find data for `${tkp.id}`")
					case Some(child_l) =>
						val result = child_l.toList.map(tkp => getWith(tkp, time, fnChooseTime).map(tkp.path.last -> _))
						RqResult.toResultOfList(result).map { pair_l =>
							JsObject(pair_l.toMap)
						}
				}
				
			case Some(timeToValue_m) =>
				get(timeToValue_m, time, fnChooseTime).asRq(s"didn't find data for `$tkp`" + (if (time.isEmpty) "" else " @ " +time.mkString("/")))
		}
	}

	private def get(timeToValue_m: Map[List[Int], JsValue], time: List[Int], fnChooseTime: List[List[Int]] => Option[List[Int]]): Option[JsValue] = {
		if (timeToValue_m.size == 0) {
			None
		}
		else {
			val time_l = timeToValue_m.keys.toList.sorted(ListIntOrdering)
			//println("timeToValue_m: "+timeToValue_m)
			//println("time: "+time)
			//println("time_l: "+time_l)
			//println("comp: "+time_l.map(ListIntOrdering.compare(_, time)))
			//println("choosen: "+fnChooseTime(time_l))
			fnChooseTime(time_l).flatMap(timeToValue_m.get)
		}
	}
	
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
