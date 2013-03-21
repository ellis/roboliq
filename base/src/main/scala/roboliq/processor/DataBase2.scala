package roboliq.processor

import scala.language.existentials
import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection._
import scala.collection.mutable.ArrayBuffer
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

object TableCategory extends Enumeration {
	val Cmd, Entity, State, All = Value
}

// REFACTOR: Make this private again once I've merged ConversionsDirect and Conversions into Converter
case class TableInfo2(
	cat: TableCategory.Value,
	tpe: Type,
	table: String,
	fnNode_? : Option[String => RqFunctionLookups] = None
) {
	val typ = tpe
}

object TableInfo2 {
	def apply[A <: Object : TypeTag](
		cat: TableCategory.Value,
		table: String,
		fnNode_? : Option[String => RqFunctionLookups] = None
	): TableInfo2 = {
		TableInfo2(cat, ru.typeTag[A].tpe, table, fnNode_?
		)
	}
}

case class LookupKey(tpe: Type, ti: TableInfo2, id_l: List[String], time: List[Int], isOption: Boolean, isList: Boolean) {
	val name: String = {
		ti.table + 
			"[" + (if (isList) id_l.mkString("[", ",", "]") else id_l.head) + "]" +
			(if (isOption) "?" else "") +
			(if (time.isEmpty) "" else time.mkString("@(", ",", ")"))
	}
}

sealed trait Key {
	val ti: TableInfo2
	val id: String
	val time: List[Int]
	val name: String
	def tpe = ti.tpe
	def toString = name
}

case class Key_Command(time: List[Int]) extends Key {
	val ti = DataBase2.cmdTableInfo
	val id = time.mkString(".")
	val name = s"${ti.table}[$id]"
}

case class Key_Entity(ti: TableInfo2, id: String) extends Key {
	val time = Nil
	val name = s"${ti.table}[$id]"
}

case class Key_State(ti: TableInfo2, id: String, context: List[Int]) extends Key {
	val name = s"${ti.table}[$id]" + context.mkString("@(", ".", ")")
}

object KeyOrdering extends Ordering[Key] {
	def compare(a: Key, b: Key): Int = {
		List(
			implicitly[scala.math.Ordering[String]].compare(a.ti.table, b.ti.table),
			implicitly[scala.math.Ordering[String]].compare(a.id, b.id),
			ListIntOrdering.compare(a.time, b.time)
		).dropWhile(_ == 0) match {
			case Nil => 0
			case n :: _ => n
		}
	}
}

class DataBase2 {
	import DataBase2._
	
	private val logger = Logger[this.type]
	
	private val timeToCmd_m = new HashMap[List[Int], Cmd]
	// REFACTOR: make key TableInfo2 instead of Type?
	//private val typeToIdToEntity_m = new HashMap[Type, HashMap[String, Entity]]
	private val typeToIdToTimeToEntity_m = new HashMap[Type, HashMap[String, SortedMap[List[Int], Entity]]]
	private val idToSourceWells_m = new HashMap[String, ArrayBuffer[Vessel]]
	private val keyToNode_m = new HashMap[Key, Node_Entity]
	private val lookupToNode_m = new HashMap[LookupKey, Node_Lookup]
	
	def findEntityTableInfoForType(tpe: Type): RqResult[TableInfo2] = {
		entityTableInfo_l.find(tpe <:< _.typ).asRq(s"type `$tpe` has no table")
	}
	
	def findStateTableInfoForType(tpe: Type): RqResult[TableInfo2] = {
		stateTableInfo_l.find(tpe <:< _.typ).asRq(s"type `$tpe` has no table")
	}
	
	def findTableInfoForType(tpe: Type): RqResult[TableInfo2] = {
		if (tpe <:< typeOf[Cmd]) RqSuccess(cmdTableInfo)
		else entityTableInfo_l.find(tpe <:< _.tpe).orElse(stateTableInfo_l.find(tpe <:< _.tpe)).asRq(s"type `$tpe` has no table")
	}
	
	/*
	 * Get commands, entities, states
	 */
	
	def getCmd(time: List[Int]): RqResult[Cmd] =
		timeToCmd_m.get(time).asRq(s"no command found with at `${time.mkString(".")}`")
	
	def getEntity(tpe: Type, id: String): RqResult[Entity] = {
		for {
			ti <- findEntityTableInfoForType(tpe)
			obj <- getAt(ti, id, Nil)
		} yield obj
	}
	
	def getState(tpe: Type, id: String, time: List[Int]): RqResult[Entity] = {
		for {
			ti <- findStateTableInfoForType(tpe)
			obj <- getAt(ti, id, time)
		} yield obj
	}
	
	def getState[A <: Entity : TypeTag](id: String, time: List[Int]): RqResult[A] =
		Try(getState(typeTag[A].tpe, id, time).map(_.asInstanceOf[A]))
	
	def getEntityAll(tpe: Type): RqResult[Iterable[Entity]] = {
		for {
			ti <- findEntityTableInfoForType(tpe)
			obj <- getAllAt(ti, Nil)
		} yield obj
	}

	def getEntityAll[A <: Entity : TypeTag](): RqResult[Iterable[A]] =
		Try(getEntityAll(typeTag[A].tpe).map(_.map(_.asInstanceOf[A])))
	
	def getStateAll(tpe: Type, time: List[Int]): RqResult[Iterable[Entity]] = {
		for {
			ti <- findStateTableInfoForType(tpe)
			obj <- getAllAt(ti, time)
		} yield obj
	}
	
	private def getAt(ti: TableInfo2, id: String, time: List[Int]): RqResult[Entity] = {
		logger.trace(s"getAt($ti, $id, $time)")
		for {
			timeToEntity_m <- typeToIdToTimeToEntity_m.get(ti.typ).flatMap(_.get(id)).asRq(s"didn't find data for type `${ti.tpe}` id `${id}`")
			obj <- getAt(ti.tpe, id, timeToEntity_m, time)
		} yield obj
	}

	private def getAt(tpe: Type, id: String, timeToEntity_m: Map[List[Int], Entity], time: List[Int]): RqResult[Entity] = {
		val state_? = for {
				timeLast <- timeToEntity_m.keys.toList.filter(time2 => ListIntOrdering.compare(time2, time) <= 0).reverse.headOption
				state <- timeToEntity_m.get(timeLast)
			} yield state
		state_?.asRq(s"didn't find data for type `${tpe}` id `${id}`")
	}
	
	private def getAllAt(ti: TableInfo2, time: List[Int]): RqResult[Iterable[Entity]] = {
		logger.trace(s"getAllAt($ti, $time)")
		RqSuccess(
			typeToIdToTimeToEntity_m.get(ti.tpe) match {
				case None => Nil
				case Some(idToTimeToEntity_m) =>
					idToTimeToEntity_m.toList.flatMap(pair => {
						val (id, timeToEntity_m) = pair
						getAt(ti.tpe, id, timeToEntity_m, time).toOption
					})
			}
		)
	}
	
	/*
	 * Set commands, entities, states
	 */
	
	def setCmd(time: List[Int], cmd: Cmd) {
		timeToCmd_m(time) = cmd
	}
	
	/** Set non-state entity */
	def setEntity(tpe: Type, entity: Entity): RqResult[Unit] = {
		for {
			ti <- findEntityTableInfoForType(tpe)
		} yield {
			setEntity(ti.tpe, entity)
			createAutomaticEntity(ti, entity)
			()
		}
	}
	
	/** Set non-state entity */
	def setEntity[A <: Entity : TypeTag](a: A): RqResult[Unit] =
		setEntity(typeTag[A].tpe, a)

	def setStateAfter(tpe: Type, state: Entity, time: List[Int]): RqResult[Unit] = {
		logger.trace(s"setAfter($tpe, $state, $time)")
		for {
			ti <- findEntityTableInfoForType(tpe)
		} yield {
			val time_r = time.reverse
			val timeAfter = time_r match {
				case Nil => List(0)
				case last :: prev => ((last + 1) :: prev).reverse
			} 
			setEntityAt(ti, state, timeAfter)
			createAutomaticState(state, timeAfter)
			()
		}
		//logger.trace(s" result: "+getAt(tkp, time))
	}
	
	def setStateAfter[A <: Entity : TypeTag](a: A, time: List[Int]): RqResult[Unit] =
		setStateAfter(typeTag[A].tpe, a, time)
	
	private def setEntity(ti: TableInfo2, entity: Entity) {
		setEntityAtVerified(ti.tpe, entity, Nil)
	}
	
	private def setEntityAt(ti: TableInfo2, entity: Entity, time: List[Int]) {
		setEntityAtVerified(ti.tpe, entity, time)
	}
	
	private def setEntityAtVerified(tpe: Type, entity: Entity, time: List[Int]) {
		val idToTimeToEntity_m = typeToIdToTimeToEntity_m.getOrElseUpdate(tpe, new HashMap[String, SortedMap[List[Int], Entity]])
		val timeToEntity_m = idToTimeToEntity_m.getOrElseUpdate(entity.id, collection.immutable.TreeMap[List[Int], Entity]()(ListIntOrdering))
		idToTimeToEntity_m(entity.id) = timeToEntity_m.updated(time, entity)
	}
	
	/*
	 * Lookup functions
	 */
	
	/*trait LookupResult {
		def getObject_? : Option[Object]
	}
	case class LookupResult_Command(cmd: Cmd) extends LookupResult {
		def getObject_? = Some(cmd)
	}
	case class LookupResult_Entity(entity: Entity, node_? : Option[Node]) extends LookupResult {
		def getObject = Some(entity)
	}
	case class LookupResult_EntityOption(entity_? : Option[Entity], node_? : Option[Node]) extends LookupResult {
		def getObject = entity_?
	}
	case class LookupResult_EntityList(entity_l: List[Entity], node_l: List[Node]) extends LookupResult
	*/
	def lookup(lk: LookupKey): RqResult[Object] = {
		// Lookup a single command
		if (lk.ti == cmdTableInfo) {
			getCmd(lk.time)
		}
		// Lookup all entities/states in the given table
		else if (lk.isList && lk.id_l.isEmpty) {
			getAllAt(lk.ti, lk.time).map(_.toList)
		}
		// Lookup entity/state as a single, list, or option
		else if (lk.isOption) {
			assert(!lk.isList)
			getAt(lk.ti, lk.id_l.head, lk.time).map(Some(_))
				.orElse(
					//createLookupOptionNode(lk)
					RqSuccess(None)
				)
		}
		else {
			for {
				// Get table
				idToTimeToEntity_m <- typeToIdToTimeToEntity_m.get(lk.ti.tpe).asRq(s"table `${lk.ti.table}` is empty")
				// Try to lookup all ids
				l1: List[RqResult[Entity]] = lk.id_l.map(id => for {
					timeToEntity_m <- idToTimeToEntity_m.get(id).asRq(s"id `$id` not found in table `${lk.ti.table}`")
					obj <- getAt(lk.ti.tpe, id, timeToEntity_m, lk.time)
				} yield obj)
				// If any aren't found, try to get/create a lookup node
				l2: List[RqResult[Either[Node_Lookup, Entity]]] = l1.map(res => {
					res.map(entity => Right(entity)).orElse(createLookupNode(lk).map(node => Left(node)))
				})
				either_l <- RqResult.toResultOfList(l2) : RqResult[List[Either[Node_Lookup, Entity]]]
				node_l = either_l.collect { case Left(node) => node }
				entity_l = either_l.collect { case Right(entity) => entity }
			} yield {
				// If entities are optional:
				if (lk.isOption) {
					val option_l = either_l.map(_ match {
						case Left(node) => None
						case Right(obj) => Some(obj)
					})
					if (lk.isList) {
						option_l
					}
					else
						option_l.head
				}
				// Else entities are required:
				else {
					// If all entities were available
					if (node_l.isEmpty) {
						if (lk.isList)
							Right(entity_l.toList)
						else
							Right(entity_l.head)
					}
					// Otherwise, there were missing entities
					else {
						Left(node_l.toList)
					}
				}
			}
		}
	}
	
	/*
	 * Helper functions
	 */
	
	private def createAutomaticEntity(ti: TableInfo2, entity: Entity) {
		val time0 = List(0)
		entity match {
			case plate: Plate =>
				val plateState = PlateState(plate, None)
				setEntityAtVerified(typeOf[PlateState], plateState, time0)
				for (well_i <- 0 until plate.nWells) {
					val wellId = WellSpecParser.wellId(plate, well_i)
					val vessel = Vessel(wellId, None)
					val vesselState = VesselState(vessel)
					val vesselPosition = VesselPosition(plateState, well_i)
					val vss = VesselSituatedState(vesselState, vesselPosition)
					setEntity(typeOf[Vessel], vessel)
					setEntityAtVerified(typeOf[Source], roboliq.entity.Source(wellId, List(vss)), time0)
					setEntityAtVerified(typeOf[VesselState], vesselState, time0)
					setEntityAtVerified(typeOf[VesselSituatedState], vss, time0)
				}
			case o: Vessel =>
				setEntityAtVerified(typeOf[Source], roboliq.entity.Source(o.id, List(o)), time0)
				setEntityAtVerified(typeOf[VesselState], VesselState(o), time0)
				if (o.tubeModel_?.isDefined) {
					val key = Key_State(ti, entity.id, time0)
					createEntityNode(key, makeTubeVesselSituatedState(entity.id))
				}
			case o: Tip =>
				setInitialState(TipState.createEmpty(o))
			case _ =>
		}
	}
	
	private def makeTubeVesselSituatedState(id: String): RqFunctionLookups = {
		import RqFunctionHandler._
		RqFunctionHandler.lookup(as[VesselState](id), as[InitialLocation](id)) { (vesselState, loc) =>
			// The tube's plate must be set
			for {
				position <- loc.position_?.asRq(s"the tube `$id` must have its initial `position` set on a plate")
				well = VesselSituatedState(vesselState, position)
			} yield {
				setInitialState(well)
				List()
			}
		}
	}
	
	private def createAutomaticState(state: Entity, time: List[Int]) {
		state match {
			case vesselState: VesselState =>
				if (vesselState.isSource) {
					addSource(vesselState.liquid.id, vesselState.vessel)
					for ((substance, _) <- vesselState.content.substanceToMol) {
						addSource(substance.id, vesselState.vessel)
					}
				}

			case _ =>
		}
	}
	
	private def addSource(id: String, vessel: Vessel) {
		idToSourceWells_m.getOrElseUpdate(id, new ArrayBuffer[Vessel]) += vessel
	}

	/*
	def get[A <: Entity : TypeTag](id: String): RqResult[Either[Node_Entity, A]] =
		Try {
			get(typeTag[A].tpe, id).map(_ match {
				case Left(node) => Left(node)
				case Right(obj) => Right(obj.asInstanceOf[A])
			})
		}
	*/
	
	private def getDefaultEntityNode(ti: TableInfo2, id: String, time: List[Int]): RqResult[Node_Entity] = {
		val key = Key_Entity(ti, id)
		keyToNode_m.get(key) match {
			case Some(node) => RqSuccess(node)
			case None =>
				ti.fnNode_? match {
					case None => RqError(s"no object in table `${ti.typ}` with id `$id`")
					case Some(fnNode) =>
						val fnargs = fnNode(id)
						//val fnargs = RqFunctionArgs(arg_l = Nil, fn = _ => RqSuccess(ret))
						createEntityNode(key, fnargs)
				}
		}
	}
	
	private def createEntityNode(key: Key, fnlookups: RqFunctionLookups): RqResult[Node_Entity] = {
		val arg_l_? : List[RqResult[LookupKey]] = fnlookups.lookup_l.map(lookup => createLookupKey(lookup, key.time))
		for {
			arg_l <- RqResult.toResultOfList(arg_l_?)
		} yield {
			val fnargs = RqFunctionArgs(fn = fnlookups.fn, arg_l = arg_l)
			val node = Node_Entity(key, fnargs)
			keyToNode_m(key) = node
			node
		}
	}
	
	private def createLookupKey(lookup: Lookup, time: List[Int]): RqResult[LookupKey] = {
		lookup match {
			case Lookup_Command(tpe) =>
				RqSuccess(LookupKey(tpe, cmdTableInfo, Nil, time, false, false))
			case Lookup_Entity(tpe, id) =>
				for { ti <- findTableInfoForType(tpe) }
				yield LookupKey(tpe, ti, List(id), time, false, false)
			case Lookup_EntityOption(tpe, id) => 
				for { ti <- findTableInfoForType(tpe) }
				yield LookupKey(tpe, ti, List(id), time, true, false)
			case Lookup_EntityList(tpe, id_l) => 
				for { ti <- findTableInfoForType(tpe) }
				yield LookupKey(tpe, ti, id_l, time, false, true)
			case Lookup_EntityAll(tpe) => 
				for { ti <- findTableInfoForType(tpe) }
				yield LookupKey(tpe, ti, Nil, time, false, true)
		}
	}
	
	private def setInitialState[A <: Entity : TypeTag](state: A) {
		setEntityAtVerified(typeTag[A].tpe, state, List(0))
	}
	
	def getKey(key: Key): RqResult[Object] = {
		key match {
			case Key_Command(time) => getCmd(time)
			case _: Key_Entity => getEntity(key.ti.tpe, key.id)
			case _: Key_State => getState(key.ti.tpe, key.id, key.time)
		}
	}
	
	//def createEntityNode(parent_? : Option[Node], )
	
	/*private def keyName(ti: TableInfo, id: String): String = {
		s"${ti.table}[$id]" 
	}*/
	
	override def toString(): String = {
		val l: List[(String, List[Entity])] = typeToIdToTimeToEntity_m.toList.map(pair => {
			val (tpe, idToTimeToEntity_m) = pair
			val table: String = entityTableInfo_m(tpe).table
			table -> idToTimeToEntity_m.values.toList.sortBy(_.id)
		}).sortBy(_._1)
		l.flatMap(pair => {
			val (table, entity_l) = pair
			entity_l.map(o => {
				s"$table[${o.id}] = $o"
			})
		}).mkString("\n")
	}
}

object DataBase2 {
	val cmdTableInfo = TableInfo2[Cmd](TableCategory.Cmd, "cmd")
	val entityTableInfo_l = List[TableInfo2](
		TableInfo2[TipModel](TableCategory.Entity, "tipModel"),
		TableInfo2[PipettePolicy](TableCategory.Entity, "pipettePolicy"),
		TableInfo2[PlateModel](TableCategory.Entity, "plateModel"),
		TableInfo2[TubeModel](TableCategory.Entity, "tubeModel"),
		TableInfo2[PlateLocation](TableCategory.Entity, "plateLocation"),
		TableInfo2[Tip](TableCategory.Entity, "tip", None),
		TableInfo2[Substance](TableCategory.Entity, "substance"),
		TableInfo2[Plate](TableCategory.Entity, "plate"),
		TableInfo2[Vessel](TableCategory.Entity, "vessel"),
		TableInfo2[WashProgram](TableCategory.Entity, "washProgram"),
		TableInfo2[roboliq.devices.pipette.PipetteDevice](TableCategory.Entity, "pipetteDevice"),
		TableInfo2[InitialLocation](TableCategory.Entity, "initialLocation"),
		TableInfo2[Source](TableCategory.Entity, "source")
	)
	val stateTableInfo_l = List[TableInfo2](
		TableInfo2[TipState](TableCategory.State, "tipState"),
		TableInfo2[PlateState](TableCategory.State, "plateState"),
		TableInfo2[VesselState](TableCategory.State, "vesselState"),
		TableInfo2[VesselSituatedState](TableCategory.State, "vesselSituatedState")
	)
	
	val entityTableInfo_m = entityTableInfo_l.map(ti => ti.typ -> ti).toMap
	val stateTableInfo_m = stateTableInfo_l.map(ti => ti.typ -> ti).toMap
}
