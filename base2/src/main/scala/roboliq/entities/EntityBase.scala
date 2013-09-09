package roboliq.entities

import scala.collection._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.MultiMap
import roboliq.core._

class EntityBase {
	val aliases = new HashMap[String, String]
	val names = new HashMap[Entity, String]
	val nameToEntity = new HashMap[String, Entity]
	val idToEntity = new HashMap[String, Entity]
	val agents = new ArrayBuffer[Agent]
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
	 * Pipetter to tips
	 */
	val pipetterToTips_m = new HashMap[Pipetter, List[Tip]]
	/**
	 * Tip to tip models
	 */
	val tipToTipModels_m = new HashMap[Tip, List[TipModel]]
	/**
	 * List of custom Relations
	 */
	val rel_l = new ArrayBuffer[Rel]
	
	def addAlias(from: String, to: String) {
		// TODO: Check for loops
		aliases(from) = to
	}
	
	private def addEntity(e: Entity, name: String) {
		names(e) = name
		nameToEntity(name) = e
		idToEntity(e.key) = e
	}
	
	def getEntity(key: String): Option[Entity] = {
		// TODO: improve lookup, this is very hacky.  Should use scope instead,
		// which would involve handing lookup outside of this class.
		// First try name, then ID, then alias
		nameToEntity.get(key).orElse(idToEntity.get(key)).orElse {
			aliases.get(key).flatMap(getEntity)
		}
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
		assert(names.contains(a))
		addEntity(d, name)
		agentToDevices_m.addBinding(a, d)
	}
	
	def addDeviceModel(d: Device, m: LabwareModel) {
		assert(names.contains(d))
		assert(names.contains(m))
		deviceToModels_m.addBinding(d, m)
	}
	
	def addDeviceModels(d: Device, l: List[LabwareModel]) {
		l.foreach(e => addDeviceModel(d, e))
	}
	
	def addDeviceSite(d: Device, s: Site) {
		assert(names.contains(d))
		assert(names.contains(s))
		deviceToSites_m.addBinding(d, s)
	}
	
	def addDeviceSites(d: Device, l: List[Site]) {
		l.foreach(s => addDeviceSite(d, s))
	}
	
	def addDeviceSpec(d: Device, spec: Entity, name: String) {
		assert(names.contains(d))
		addEntity(spec, name)
		deviceToSpecs_m.addBinding(d, spec)
	}
	
	def addStackable(bot: LabwareModel, top: LabwareModel) {
		assert(names.contains(bot))
		assert(names.contains(top))
		stackables_m.addBinding(bot, top)
	}
	
	def addStackables(bot: LabwareModel, l: List[LabwareModel]) {
		l.foreach(top => addStackable(bot, top))
	}
	
	def setModel(l: Labware, m: LabwareModel) {
		assert(names.contains(l))
		assert(names.contains(m))
		labwareToModel_m(l) = m
	}
	
	def addLabware(e: Labware, name: String) {
		addEntity(e, name)
	}

	def setLocation(l: Labware, e: Entity) {
		assert(names.contains(l))
		assert(names.contains(e))
		labwareToLocation_m(l) = e
	}
	
	def addRel(rel: Rel) {
		rel_l += rel
	}
	
	def makeInitialConditionsList(): List[Rel] = {
		names.toList.flatMap(pair => pair._1.typeNames.map(typeName => Rel(s"is-$typeName", List(pair._2), pair._1.label.getOrElse(null)))).toList.sortBy(_.toString) ++
		agentToDevices_m.flatMap(pair => pair._2.toList.map(device => {
			Rel(s"agent-has-device", List(names(pair._1), names(device)))
		})).toList.sortBy(_.toString) ++
		deviceToModels_m.flatMap(pair => pair._2.map(model => {
			Rel(s"device-can-model", List(names(pair._1), names(model)))
		})).toList.sortBy(_.toString) ++
		deviceToSites_m.flatMap(pair => pair._2.map(site => {
			Rel(s"device-can-site", List(names(pair._1), names(site)))
		})).toList.sortBy(_.toString) ++
		deviceToSpecs_m.flatMap(pair => pair._2.toList.map(spec => {
			Rel(s"device-can-spec", List(names(pair._1), names(spec)))
		})).toList.sortBy(_.toString) ++
		stackables_m.flatMap(pair => pair._2.map(model => {
			Rel(s"stackable", List(names(pair._1), names(model)))
		})).toList.sortBy(_.toString) ++
		labwareToModel_m.map(pair => Rel(s"model", List(names(pair._1), names(pair._2)))).toList.sortBy(_.toString) ++
		labwareToLocation_m.map(pair => Rel(s"location", List(names(pair._1), names(pair._2)))).toList.sortBy(_.toString) ++
		rel_l.toList.sortBy(_.toString)
	}
	
	def makeInitialConditions(): String = {
		val l: List[Rel] = makeInitialConditionsList
		val l2: List[String] = l.map(r => "  " + r.toStringWithComment)
		l2.mkString("\n")
	}
	
	def lookupLiquidSource(sourceIdent: String): RsResult[List[(Labware, RowCol)]] = {
		for {
			parsed_l <- WellIdentParser.parse(sourceIdent)
			ll_? : List[RsResult[List[(Labware, RowCol)]]] = parsed_l.map(pair => {
				val (entityIdent, index_l) = pair
				getEntity(entityIdent) match {
					// For plates and tubes
					case Some(labware: Labware) =>
						for {
							// Get labware model
							model <- labwareToModel_m.get(labware).asRs(s"model not set for labware `$entityIdent`")
							// Get number of rows and cols on labware
							rowsCols <- model match {
								case m: PlateModel => RsSuccess((m.rows, m.cols))
								case m: TubeModel => RsSuccess((1, 1))
								case _ => RsError(s"model of labware `$entityIdent` must be a plate or tube")
							}
						} yield {
							// TODO: check that the RowCol values are valid for the labware model
							val l: List[(Labware, RowCol)] = index_l.flatMap(_ match {
								case WellIdentOne(rc) =>
									List((labware, rc))
								case WellIdentVertical(rc0, rc1) =>
									(for {
										col_i <- rc0.col to rc1.col
										row_i <- (if (col_i == rc0.col) rc0.row else 0) to (if (col_i == rc1.col) rc1.row else rowsCols._1)
									} yield {
										(labware, RowCol(row_i, col_i))
									}).toList
								case WellIdentHorizontal(rc0, rc1) =>
									(for {
										row_i <- rc0.row to rc1.row
										col_i <- (if (row_i == rc0.row) rc0.col else 0) to (if (row_i == rc1.row) rc1.col else rowsCols._2)
									} yield {
										(labware, RowCol(row_i, col_i))
									}).toList
								case WellIdentMatrix(rc0, rc1) =>
									(for {
										row_i <- rc0.row to rc1.row
										col_i <- rc0.col to rc1.col
									} yield {
										(labware, RowCol(row_i, col_i))
									}).toList
							})
							l
						}

					case None => RsError(s"entity not found: `$entityIdent`")
					case _ => RsError(s"require a labware entity: `$entityIdent`")
				}
			})
			ll /*: List[List[(Labware, RowCol)]]*/ <- RsResult.toResultOfList(ll_?)
		} yield ll.flatten
	}
}
