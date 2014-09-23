package roboliq.entities

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.reflect.runtime.universe

import roboliq.core.RqResult
import roboliq.core.RsError
import roboliq.core.RsResult
import roboliq.core.RsSuccess
import roboliq.core.pimpedOption
import scalax.collection.Graph
import scalax.collection.Graph.apply$default$3
import scalax.collection.edge.LkUnDiEdge

// TODO: need to check for naming conflict between entities and non-entities (e.g. wellGroupToWells_m) -- in general, all names should be tracked in a central location so that naming conflicts can be detected and/or resolved.
class EntityBase {
	var atoms = AtomBase(Set())
	val aliases = new HashMap[String, String]
	/**
	 * Map from entity to its JSHOP identifier
	 */
	val entityToIdent_m = new HashMap[Entity, String]
	/**
	 * Map from JSHOP identifier to entity
	 */
	val identToEntity_m = new HashMap[String, Entity]
	/**
	 * Map from database key to entity
	 */
	val keyToEntity_m = new HashMap[String, Entity]
	/**
	 * List of agents
	 */
	val agents = new ArrayBuffer[Agent]
	/**
	 * Map of agents and their devices
	 */
	val agentToDevices_m = new HashMap[Agent, mutable.Set[Device]] with MultiMap[Agent, Device]
	/**
	 * LabwareModels that devices can use
	 */
	val deviceToModels_m = new HashMap[Device, mutable.Set[LabwareModel]] with MultiMap[Device, LabwareModel]
	/**
	 * Sites that devices can access
	 */
	val deviceToSites_m = new HashMap[Device, mutable.Set[Site]] with MultiMap[Device, Site]
	/**
	 * Specs that devices accept
	 */
	val deviceToSpecs_m = new HashMap[Device, mutable.Set[Entity]] with MultiMap[Device, Entity]
	/**
	 * Models which another model can have stacked on top of it
	 */
	val stackables_m = new HashMap[LabwareModel, mutable.Set[LabwareModel]] with MultiMap[LabwareModel, LabwareModel]
	/**
	 * Each labware's model
	 */
	val labwareToModel_m = new HashMap[Labware, LabwareModel]
	/**
	 * Initial location of labware
	 */
	val labwareToLocation_m = new HashMap[Labware, Entity]
	/**
	 * Map of pipetter to its tips
	 */
	val pipetterToTips_m = new HashMap[Pipetter, List[Tip]]
	/**
	 * Map of tip to the tip models which can be used by that tip
	 */
	val tipToTipModels_m = new HashMap[Tip, List[TipModel]]
	/**
	 * Map of source name to mixture
	 */
	val sourceToMixture_m = new HashMap[String, Mixture]
	/**
	 * Map of reagent name to source wells
	 * REFACTOR: Rename sourceToWells_m
	 */
	val wellGroupToWells_m = new HashMap[String, List[WellInfo]]
	/**
	 * Map of reagent name to source wells
	 * REFACTOR: Rename sourceToWells_m
	 */
	val reagentToWells_m = new HashMap[String, List[Well]]
	/**
	 * List of custom Relations
	 */
	val rel_l = new ArrayBuffer[Rel]
	/**
	 * Transport graphs.
	 * Contains site nodes with edges between sites where direct transportation is possible.
	 */
	var transportGraph = Graph[Site, LkUnDiEdge]()
	
	def addAlias(from: String, to: String) {
		// TODO: Check for loops
		aliases(from.toLowerCase) = to
	}
	
	def addEntityWithoutIdent(e: Entity) {
		if (keyToEntity_m.contains(e.key)) {
			assert(keyToEntity_m(e.key) eq e)
		}
		keyToEntity_m(e.key) = e
	}
	
	private def addEntity(e: Entity, ident: String) {
		val lower = ident.toLowerCase
		if (identToEntity_m.contains(lower)) {
			// FIXME: for debug only
			if ((identToEntity_m(lower) ne e))
				println("e, ident, lower, nameToEntity = "+(e, ident, lower, identToEntity_m(lower)))
			// ENDFIX
			assert(identToEntity_m(lower) eq e)
		}
		entityToIdent_m(e) = ident
		identToEntity_m(lower) = e
		keyToEntity_m(e.key) = e
	}
	
	def getEntity(key: String): Option[Entity] = {
		val ident = key.toLowerCase
		// TODO: improve lookup, this is very hacky.  Should use scope instead,
		// which would involve handing lookup outside of this class.
		// First try name, then ID, then alias
		identToEntity_m.get(ident).orElse(keyToEntity_m.get(key)).orElse {
			aliases.get(ident).flatMap(getEntity)
		}
	}
	
	def getEntityAs[A <: Entity : Manifest](key: String): RsResult[A] = {
		val lower = key.toLowerCase
		(identToEntity_m.get(lower) match {
			case Some(entity) =>
				RsResult.asInstanceOf(entity)
			case None =>
				keyToEntity_m.get(key) match {
					case Some(entity) =>
						RsResult.asInstanceOf(entity)
					case None =>
						aliases.get(lower) match {
							case Some(ident2) => getEntityAs(ident2)
							case None => RsError(s"missing entity with key `$key`")
						}
				}
		}).prependError(s"error looking up entity `$key`")
	}
	
	def getEntityByIdent[A <: Entity : Manifest](ident0: String): RsResult[A] = {
		val ident = ident0.toLowerCase
		identToEntity_m.get(ident) match {
			case None =>
				aliases.get(ident) match {
					case None => RsError(s"missing entity with ident `$ident`")
					case Some(ident2) => getEntityByIdent(ident2)
				}
			case Some(entity) =>
				RsResult.asInstanceOf(entity)
		}
	}
	
	private def addAtom(ss: String*) {
		atoms = atoms.add(ss : _*)
	}
	
	def addAgent(e: Agent, name: String) {
		addEntity(e, name)
	}
	
	def addModel(e: LabwareModel, name: String) {
		addEntity(e, name)
	}
	
	def addSite(e: Site, name: String) {
		addEntity(e, name)
	}
	
	def addDevice(a: Agent, d: Device, name: String) {
		assert(entityToIdent_m.contains(a))
		addEntity(d, name)
		agentToDevices_m.addBinding(a, d)
	}
	
	def addDeviceModel(d: Device, m: LabwareModel) {
		assert(entityToIdent_m.contains(d))
		assert(entityToIdent_m.contains(m))
		deviceToModels_m.addBinding(d, m)
	}
	
	def addDeviceModels(d: Device, l: List[LabwareModel]) {
		l.foreach(e => addDeviceModel(d, e))
	}
	
	def addDeviceSite(d: Device, s: Site) {
		assert(entityToIdent_m.contains(d))
		assert(entityToIdent_m.contains(s))
		deviceToSites_m.addBinding(d, s)
	}
	
	def addDeviceSites(d: Device, l: List[Site]) {
		l.foreach(s => addDeviceSite(d, s))
	}
	
	def addDeviceSpec(d: Device, spec: Entity, name: String) {
		assert(entityToIdent_m.contains(d))
		addEntity(spec, name)
		deviceToSpecs_m.addBinding(d, spec)
	}
	
	def addStackable(bot: LabwareModel, top: LabwareModel) {
		assert(entityToIdent_m.contains(bot))
		assert(entityToIdent_m.contains(top))
		addAtom("stackable", bot.getName, top.getName)
		stackables_m.addBinding(bot, top)
	}
	
	def addStackables(bot: LabwareModel, l: List[LabwareModel]) {
		l.foreach(top => addStackable(bot, top))
	}
	
	def setModel(l: Labware, m: LabwareModel) {
		assert(entityToIdent_m.contains(l))
		assert(entityToIdent_m.contains(m))
		addAtom("model", l.getName, m.getName)
		labwareToModel_m(l) = m
	}
	
	def addLabware(e: Labware, name: String) {
		addEntity(e, name)
	}
	
	def addUserShakerProgram(program: Entity, ident: String) {
		val device_l = entityToIdent_m.keys.toList.collect({case x: Shaker => x})
		device_l.foreach(device => addDeviceSpec(device, program, ident))
	}

	def setLocation(l: Labware, e: Entity) {
		assert(entityToIdent_m.contains(l))
		assert(entityToIdent_m.contains(e))
		addAtom("location", l.getName, e.getName)
		labwareToLocation_m(l) = e
	}
	
	def addRel(rel: Rel) {
		addAtom((rel.name :: rel.args) : _*)
		rel_l += rel
	}
	
	def getIdent(e: Entity): RsResult[String] = entityToIdent_m.get(e).asRs(s"missing identifier for entity $e")
	
	/**
	 * Create list of (object, type) tuples for the problem definition
	 */
	def createProblemObjects: List[(String, String)] = {
		entityToIdent_m.toList.map(pair => pair._2 -> pair._1.typeNames.head)
	}
	
	def createProblemState: List[Rel] = {
		agentToDevices_m.flatMap(pair => pair._2.toList.map(device => {
			Rel(s"agent-has-device", List(entityToIdent_m(pair._1), entityToIdent_m(device)))
		})).toList.sortBy(_.toString) ++
		deviceToModels_m.flatMap(pair => pair._2.map(model => {
			Rel(s"device-can-model", List(entityToIdent_m(pair._1), entityToIdent_m(model)))
		})).toList.sortBy(_.toString) ++
		deviceToSites_m.flatMap(pair => pair._2.map(site => {
			Rel(s"device-can-site", List(entityToIdent_m(pair._1), entityToIdent_m(site)))
		})).toList.sortBy(_.toString) ++
		deviceToSpecs_m.flatMap(pair => pair._2.toList.map(spec => {
			Rel(s"device-can-spec", List(entityToIdent_m(pair._1), entityToIdent_m(spec)))
		})).toList.sortBy(_.toString) ++
		stackables_m.flatMap(pair => pair._2.map(model => {
			Rel(s"stackable", List(entityToIdent_m(pair._1), entityToIdent_m(model)))
		})).toList.sortBy(_.toString) ++
		labwareToModel_m.map(pair => Rel(s"model", List(entityToIdent_m(pair._1), entityToIdent_m(pair._2)))).toList.sortBy(_.toString) ++
		labwareToLocation_m.map(pair => Rel(s"location", List(entityToIdent_m(pair._1), entityToIdent_m(pair._2)))).toList.sortBy(_.toString) ++
		rel_l.toList.sortBy(_.toString)
	}
	
	def makeInitialConditionsList(): List[Rel] = {
		entityToIdent_m.toList.flatMap(pair => pair._1.typeNames.map(typeName => Rel(s"is-$typeName", List(pair._2), pair._1.label.getOrElse(null)))).toList.sortBy(_.toString) ++
		agentToDevices_m.flatMap(pair => pair._2.toList.map(device => {
			Rel(s"agent-has-device", List(entityToIdent_m(pair._1), entityToIdent_m(device)))
		})).toList.sortBy(_.toString) ++
		deviceToModels_m.flatMap(pair => pair._2.map(model => {
			Rel(s"device-can-model", List(entityToIdent_m(pair._1), entityToIdent_m(model)))
		})).toList.sortBy(_.toString) ++
		deviceToSites_m.flatMap(pair => pair._2.map(site => {
			Rel(s"device-can-site", List(entityToIdent_m(pair._1), entityToIdent_m(site)))
		})).toList.sortBy(_.toString) ++
		deviceToSpecs_m.flatMap(pair => pair._2.toList.map(spec => {
			Rel(s"device-can-spec", List(entityToIdent_m(pair._1), entityToIdent_m(spec)))
		})).toList.sortBy(_.toString) ++
		stackables_m.flatMap(pair => pair._2.map(model => {
			Rel(s"stackable", List(entityToIdent_m(pair._1), entityToIdent_m(model)))
		})).toList.sortBy(_.toString) ++
		labwareToModel_m.map(pair => Rel(s"model", List(entityToIdent_m(pair._1), entityToIdent_m(pair._2)))).toList.sortBy(_.toString) ++
		labwareToLocation_m.map(pair => Rel(s"location", List(entityToIdent_m(pair._1), entityToIdent_m(pair._2)))).toList.sortBy(_.toString) ++
		rel_l.toList.sortBy(_.toString)
	}
	
	def makeInitialConditions(): String = {
		val l: List[Rel] = makeInitialConditionsList
		val l2: List[String] = l.map(r => "  " + r.toStringWithComment)
		l2.mkString("\n")
	}
	
	def lookupLiquidSource(text: String, state: WorldState): RsResult[PipetteSources] = {
		for {
			sources <- lookupLiquidSources(text, state)
			_ <- RqResult.assert(sources.sources.size <= 1, "only one liquid source may be specified in this context")
		} yield sources
	}
	
	def lookupLiquidSources(text: String, state: WorldState): RsResult[PipetteSources] = {
		for {
			wellInfo_ll <- lookupWellInfo(text, state)
		} yield PipetteSources(wellInfo_ll.map(LiquidSource))
	}
	
	def lookupLiquidDestinations(text: String, state: WorldState): RsResult[PipetteDestinations] = {
		for {
			wellInfo_ll <- lookupWellInfo(text, state)
		} yield PipetteDestinations(wellInfo_ll.flatten)
	}
	
	private def lookupWellInfo(text: String, state: WorldState): RqResult[List[List[WellInfo]]] = {
		for {
			parsed_l <- WellIdentParser.parse(text)
			ll <- RqResult.mapAll(parsed_l)(pair => {
				val (entityIdent, wellIdent_l) = pair
				wellIdentParserResultToWellInfo(state, entityIdent, wellIdent_l)
			}).map(_.flatten)
			_ <- RqResult.assert(!ll.isEmpty, s"No wells found for `${text}`")
		} yield ll
	}
	
	private def wellIdentParserResultToWellInfo(
		state: WorldState,
		entityIdent: String,
		wellIdent_l: List[WellIdent]
	): RqResult[List[List[WellInfo]]] = {
		getEntity(entityIdent) match {
			// For plates and tubes
			case Some(labware: Labware) =>
				for {
					wellInfo_l <- wellIdentsToWellInfo(state, entityIdent, labware, wellIdent_l)
				} yield wellInfo_l.map(x => List(x))
			case None =>
				reagentToWells_m.get(entityIdent).asRs(s"entity not found: `$entityIdent`")
				if (reagentToWells_m.contains(entityIdent)) {
					for {
						well_l <- reagentToWells_m.get(entityIdent).asRs(s"entity not found: `$entityIdent`")
						l1 <- RsResult.mapAll(well_l)(well => wellToWellInfo(state, well))
					} yield List(l1)
				}
				else if (wellGroupToWells_m.contains(entityIdent)) {
					RsSuccess(wellGroupToWells_m(entityIdent).map(List(_)))
				}
				else {
					RsError(s"entity not found: `$entityIdent`")
				}
			case _ => RsError(s"require a labware entity or reagent: `$entityIdent`")
		}
	}
		
	private def wellIdentsToWellInfo(state: WorldState, labwareName: String, labware: Labware, wellIdent_l: List[WellIdent]): RqResult[List[WellInfo]] = {
		for {
			rowcol_l <- RqResult.mapAll(wellIdent_l)(x => wellIdentToRowCol(labwareName, labware, x)).map(_.flatten)
			well_l <- RqResult.mapAll(rowcol_l)(rowcol => wellAt(state, labwareName, labware, rowcol))
		} yield {
			(well_l zip rowcol_l).map(pair => WellInfo(labware, labwareName, pair._1, pair._2))
		}
	}

	private def wellIdentToRowCol(labwareName: String, labware: Labware, wellIdent: WellIdent): RqResult[List[RowCol]] = {
		for {
			// Get labware model
			model <- labwareToModel_m.get(labware).asRs(s"model not set for labware `$labwareName`")
			// Get number of rows and cols on labware
			rowsCols <- model match {
				case m: PlateModel => RsSuccess((m.rows, m.cols))
				case m: TubeModel => RsSuccess((1, 1))
				case _ => RsError(s"model of labware `$labwareName` must be a plate or tube")
			}
		} yield {
			// TODO: check that the RowCol values are valid for the labware model
			wellIdent match {
				case WellIdentOne(rc) =>
					List(rc)
				case WellIdentVertical(rc0, rc1) =>
					(for {
						col_i <- rc0.col to rc1.col
						row_i <- (if (col_i == rc0.col) rc0.row else 0) to (if (col_i == rc1.col) rc1.row else rowsCols._1 - 1)
					} yield {
						RowCol(row_i, col_i)
					}).toList
				case WellIdentHorizontal(rc0, rc1) =>
					(for {
						row_i <- rc0.row to rc1.row
						col_i <- (if (row_i == rc0.row) rc0.col else 0) to (if (row_i == rc1.row) rc1.col else rowsCols._2 - 1)
					} yield {
						RowCol(row_i, col_i)
					}).toList
				case WellIdentMatrix(rc0, rc1) =>
					(for {
						row_i <- rc0.row to rc1.row
						col_i <- rc0.col to rc1.col
					} yield {
						RowCol(row_i, col_i)
					}).toList
			}
		}
	}
	
	private def wellAt(state: WorldState, labwareName: String, labware: Labware, rowcol: RowCol): RqResult[Well] = {
		state.labwareRowCol_well_m.get(labware, rowcol).asRs(s"labware `$labwareName` doesn't have a well at $rowcol")
	}
	
	def wellToWellInfo(state: WorldState, well: Well): RqResult[WellInfo] = {
		for {
			labware <- state.well_labware_m.get(well).asRs("INTERNAL: Well is missing labware information")
			labwareName <- getIdent(labware)
			rowcol <- state.well_rowcol_m.get(well).asRs("INTERNAL: well is missing row/col infomation")
		} yield {
			WellInfo(labware, labwareName, well, rowcol)
		}
	}
}
