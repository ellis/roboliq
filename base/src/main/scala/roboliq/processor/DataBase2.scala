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

	val entityTableInfo_l = List[TableInfo](
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
	val stateTableInfo_l = List[TableInfo](
		TableInfo[TipState]("tipState", Some("conf"), None, None),
		TableInfo[PlateState]("plateState", Some("plate"), None, None),
		TableInfo[VesselState]("vesselState", Some("vessel"), None, None),
		TableInfo[VesselSituatedState]("vesselSituatedState", Some("vesselState"), None, None)
	)
	
	private val entityTableInfo_m = entityTableInfo_l.map(ti => ti.typ -> ti).toMap
	private val stateTableInfo_m = stateTableInfo_l.map(ti => ti.typ -> ti).toMap
	
	private val typeToIdToEntity_m = new HashMap[Type, HashMap[String, Entity]]
	private val typeToIdToTimeToState_m = new HashMap[Type, HashMap[String, HashMap[List[Int], Entity]]]
	
	private def findEntityTableInfoForType(tpe: Type): RqResult[TableInfo] = {
		entityTableInfo_l.find(tpe <:< _.typ).asRq(s"type `$tpe` has no table")
	}
	
	private def findStateTableInfoForType(tpe: Type): RqResult[TableInfo] = {
		stateTableInfo_l.find(tpe <:< _.typ).asRq(s"type `$tpe` has no table")
	}
	
	def set[A <: Entity : TypeTag](a: A): RqResult[Unit] =
		set(typeTag[A].tpe, a)
	
	def set(tpe: Type, entity: Entity): RqResult[Unit] = {
		for {
			ti <- findEntityTableInfoForType(tpe)
		} yield {
			typeToIdToEntity_m.getOrElseUpdate(ti.typ, new HashMap[String, Entity])(entity.id) = entity
			()
		}
	}
	
	def get[A <: Entity : TypeTag](id: String): RqResult[A] =
		Try(get(typeTag[A].tpe, id).map(_.asInstanceOf[A]))
	
	def get(tpe: Type, id: String): RqResult[Entity] = {
		for {
			ti <- findEntityTableInfoForType(tpe)
			obj_? = for {
				idToObj_m <- typeToIdToEntity_m.get(ti.typ)
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
	
	def getAt[A <: Entity : TypeTag](id: String, time: List[Int]): RqResult[A] =
		Try(getAt(typeTag[A].tpe, id, time).map(_.asInstanceOf[A]))
	
	def getAt(tpe: Type, id: String, time: List[Int]): RqResult[Entity] = {
		logger.trace(s"getAt($tpe, $id, $time)")
		for {
			ti <- findEntityTableInfoForType(tpe)
			state_? : Option[Entity] = for {
					timeToState_m <- typeToIdToTimeToState_m.get(ti.typ).flatMap(_.get(id))
					timeLast <- timeToState_m.keys.toList.filter(time2 => ListIntOrdering.compare(time2, time) <= 0).reverse.headOption
					state <- timeToState_m.get(timeLast)
				} yield state
			state <- state_?.asRq(s"didn't find data for type `$tpe` id `${id}`")
		} yield state
	}
	
	def getAll(tpe: Type): RqResult[Iterable[Entity]] = {
		logger.trace(s"getAll($tpe)")
		for {
			ti <- findEntityTableInfoForType(tpe)
		} yield {
			val idToObj_m = typeToIdToEntity_m.getOrElse(ti.typ, Map())
			idToObj_m.values
		}
	}
	
	override def toString(): String = {
		val l: List[(String, List[Entity])] = typeToIdToEntity_m.toList.map(pair => {
			val (tpe, idToEntity_m) = pair
			val table: String = entityTableInfo_m(tpe).table
			table -> idToEntity_m.values.toList.sortBy(_.id)
		}).sortBy(_._1)
		l.flatMap(pair => {
			val (table, entity_l) = pair
			entity_l.map(o => {
				s"$table[${o.id}] = $o"
			})
		}).mkString("\n")
	}
}
