package roboliq.entities

import scala.collection._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.MultiMap

class EntityBase {
	var names = new HashMap[Entity, String]
	var nameToEntity = new HashMap[String, Entity]
	var idToEntity = new HashMap[String, Entity]
	var agents = new ArrayBuffer[Agent]
	var agentToDevices_m = new HashMap[Agent, mutable.Set[Device]] with MultiMap[Agent, Device]
	/**
	 * LabwareModels that devices can use
	 */
	var deviceToModels_m = new HashMap[Device, mutable.Set[LabwareModel]] with MultiMap[Device, LabwareModel]
	/**
	 * Sites that devices can access
	 */
	var deviceToSites_m = new HashMap[Device, mutable.Set[Site]] with MultiMap[Device, Site]
	/**
	 * Specs that devices accept
	 */
	var deviceToSpecs_m = new HashMap[Device, mutable.Set[Entity]] with MultiMap[Device, Entity]
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
	
	private def addEntity(e: Entity, name: String) {
		names(e) = name
		nameToEntity(name) = e
		idToEntity(e.id) = e
	}
	
	def getEntity(key: String): Option[Entity] = {
		nameToEntity.get(key).orElse(idToEntity.get(key))
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
		names(spec) = name
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
	
	def makeInitialConditionsList(): List[Rel] = {
		names.toList.flatMap(pair => pair._1.typeNames.map(typeName => Rel(s"is-$typeName", List(pair._2)))) ++
		agentToDevices_m.flatMap(pair => pair._2.toList.map(device => {
			Rel(s"agent-has-device", List(names(pair._1), names(device)))
		})) ++
		deviceToModels_m.flatMap(pair => pair._2.map(model => {
			Rel(s"device-can-model", List(names(pair._1), names(model)))
		})) ++
		deviceToSites_m.flatMap(pair => pair._2.map(site => {
			Rel(s"device-can-site", List(names(pair._1), names(site)))
		})) ++
		deviceToSpecs_m.flatMap(pair => pair._2.toList.map(spec => {
			Rel(s"device-can-spec", List(names(pair._1), names(spec)))
		})) ++
		stackables_m.flatMap(pair => pair._2.map(model => {
			Rel(s"stackable", List(names(pair._1), names(model)))
		})) ++
		labwareToModel_m.map(pair => Rel(s"model", List(names(pair._1), names(pair._2)))) ++
		labwareToLocation_m.map(pair => Rel(s"location", List(names(pair._1), names(pair._2))))
	}
	
	def makeInitialConditions(): String = {
		val l: List[Rel] = makeInitialConditionsList
		val l2: List[String] =
			" ; initial conditions" ::
			" (" ::
			l.map(r => "  " + r) ++
			List(" )")
		l2.sorted.mkString("\n")
	}
}
