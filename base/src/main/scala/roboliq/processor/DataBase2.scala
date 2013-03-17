package roboliq.processor

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection._
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.math.Ordering
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeOf
import scala.reflect.runtime.universe.typeTag
import scala.util.Try
import scalaz._
import grizzled.slf4j.Logger
import roboliq.core._, roboliq.entity._


class DataBase2 {
	private val logger = Logger[this.type]

	val entityTableInfo_l = List[TableInfo[_ <: Object]](
		TableInfo[TipModel]("tipModel", None, None),
		TableInfo[PipettePolicy]("pipettePolicy"),
		TableInfo[PlateModel]("plateModel", None, None),
		TableInfo[TubeModel]("tubeModel", None, None),
		TableInfo[PlateLocation]("plateLocation", None, None),
		TableInfo[Tip]("tip", Some("id"), None),
		TableInfo[Substance]("substance", None, None),
		TableInfo[Plate]("plate", None, None),
		TableInfo[Vessel]("vessel", None, None),
		TableInfo[WashProgram]("washProgram"),
		TableInfo[roboliq.devices.pipette.PipetteDevice]("pipetteDevice"),
		TableInfo[InitialLocation]("initialLocation"),
		TableInfo[Source]("source")
	)
	val stateTableInfo_l = List[TableInfo[_ <: Object]](
		TableInfo[TipState]("tipState", Some("conf"), None, None),
		TableInfo[PlateState]("plateState", Some("plate"), None, None),
		TableInfo[VesselState]("vesselState", Some("vessel"), None, None),
		TableInfo[VesselSituatedState]("vesselSituatedState", Some("vesselState"), None, None)
	)
	
	private val typeToIdToObj_m = new HashMap[Type, HashMap[String, Entity]]
	private val typeToIdToTimeToState_m = new HashMap[Type, HashMap[String, HashMap[List[Int], Entity]]]
	
	private def findEntityTableInfoForType(tpe: Type): RqResult[TableInfo[_]] = {
		entityTableInfo_l.find(tpe <:< _.typ).asRq(s"type `$tpe` has no table")
	}
	
	private def findStateTableInfoForType(tpe: Type): RqResult[TableInfo[_]] = {
		stateTableInfo_l.find(tpe <:< _.typ).asRq(s"type `$tpe` has no table")
	}
	
	def set[A <: Entity : TypeTag](a: A): RqResult[Unit] =
		set(typeTag[A].tpe, a)
	
	def set(tpe: Type, entity: Entity): RqResult[Unit] = {
		for {
			ti <- findEntityTableInfoForType(tpe)
		} yield {
			typeToIdToObj_m.getOrElseUpdate(ti.typ, new HashMap[String, Entity])(entity.id) = entity
			()
		}
	}
	
	def get[A <: Entity : TypeTag](id: String): RqResult[A] = {
		val tpeA = typeTag[A].tpe
		Try(
			for {
				entity <- get(tpeA, id)
			} yield entity.asInstanceOf[A]
		)
	}
	
	def get(tpe: Type, id: String): RqResult[Entity] = {
		for {
			ti <- findEntityTableInfoForType(tpe)
			obj_? = for {
				idToObj_m <- typeToIdToObj_m.get(ti.typ)
				obj <- idToObj_m.get(id)
			} yield obj
			obj <- obj_?.asRq(s"object in table `${ti.typ}` with id `$id`")
		} yield obj
	}
	
	def setAfter[A <: Entity : TypeTag](a: A, time: List[Int]): RqResult[Unit] =
		setAfter(typeTag[A].tpe, a, time)
	
	def setAfter(tpe: Type, state: Entity, time: List[Int]): RqResult[Unit] = {
		logger.trace(s"setAfter($tpe, $state, $time)")
		for {
			ti <- findEntityTableInfoForType(tpe)
		} yield {
			val time_r = time.reverse
			val timeAfter = time_r match {
				case Nil => List(0)
				case last :: prev => ((last + 1) :: prev).reverse
			} 
			typeToIdToTimeToState_m.
				getOrElseUpdate(ti.typ, new HashMap[String, HashMap[List[Int], Entity]]).
				getOrElseUpdate(state.id, new HashMap[List[Int], Entity]) +=
				(timeAfter -> state)
			()
		}
		//logger.trace(s" result: "+getAt(tkp, time))
	}
	
	def getAt(tpe: Type, id: String, time: List[Int]): RqResult[Entity] = {
		logger.trace(s"getAt($tpe, $state, $time)")
		for {
			ti <- findEntityTableInfoForType(tpe)
		} yield {
			for {
				timeToState_m <- typeToIdToTimeToState_m.get(ti.typ).flatMap(_.get(id))
			} yield {
				timeToState_m.keys.toList.filter(time2 => ListIntOrdering.compart(time2, time) <= 0).reverse
			}
			
				getOrElseUpdate(ti.typ, new HashMap[String, HashMap[List[Int], Entity]]).
				
				getOrElseUpdate(id, new HashMap[List[Int], Entity]) +=
				(time -> state)
			()
		}
		//logger.trace(s" result: "+getAt(tkp, time))
	}
	
	def getBefore(tkp: TKP, time: List[Int]): RqResult[JsValue] = {
		logger.trace(s"getBefore($tkp, $time)")
		assert(!time.isEmpty)
		// REFACTOR: HACK: This is done to let conversion nodes for default initial states access other initial state values -- should probably fix in Processor instead. 
		if (time == List(0))
			getAt(tkp, time)
		else {
			def chooseTime(time_l: List[List[Int]]): Option[List[Int]] =
				time_l.takeWhile(ListIntOrdering.compare(_, time) < 0).lastOption
			getWith(tkp, time, chooseTime _)
		}
	}
	
	def getAt(tkp: TKP, time: List[Int]): RqResult[JsValue] = {
		logger.trace(s"getAt($tkp, $time)")
		def fnChooseTime(time_l: List[List[Int]]): Option[List[Int]] =
			time_l.takeWhile(ListIntOrdering.compare(_, time) <= 0).lastOption
		getWith(tkp, time, fnChooseTime _)
	}
	
	def get(tkp: TKP): RqResult[JsValue] = getAt(tkp, Nil)

	def get(table: String, key: String, path: List[String] = Nil): RqResult[JsValue] = get(TKP(table, key, path))
	
	/**
	 * Get all keys for the given table.
	 */
	def getAllKeys(table: String): List[TKP] = {
		logger.trace(s"getAllKeys($table)")
		// Get list of root keys in table
		children_m.filterKeys(_ match {
			case TKP(table_#, id, Nil) if table_# == table => true
			case _ => false
		}).keys.toList
	}
	
	def getAll(table: String): List[JsValue] = {
		logger.trace(s"getAll($table)")
		val tkp_l = getAllKeys(table)
		// Try to get all entities
		tkp_l.map(tkp => {
			get(tkp).map(Option(_)).getOrElse(None)
		}).flatten
	}
	
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
						val result: List[(String, JsValue)] = child_l.toList.map(tkp => getWith(tkp, time, fnChooseTime) match {
							case RqSuccess(x, _) => Some(tkp.path.last -> x)
							case _ => None
						}).flatten
						if (result.isEmpty && !child_l.isEmpty)
							RqError[JsValue](s"couldn't find any fields for `${tkp.id}` at time $time")
						else
							RqSuccess(JsObject(result.toMap))
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
