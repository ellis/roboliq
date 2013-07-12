package roboliq.entities

import scala.collection._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.MultiMap

class EntityBase {
	var names = new HashMap[Entity, String]
	var agents = new ArrayBuffer[Agent]
	var agentToDevices_m = new HashMap[Agent, mutable.Set[Device]] with MultiMap[Agent, Device]
	/**
	 * LabwareModels that devices can use
	 */
	var deviceToModels_m = new HashMap[Device, List[LabwareModel]]
	/**
	 * Sites that devices can access
	 */
	var deviceToSites_m = new HashMap[Device, List[Site]]
	/**
	 * Specs that devices accept
	 */
	var deviceToSpecs_m = new HashMap[Device, mutable.Set[Entity]] with MultiMap[Device, Entity]
	/**
	 * Models which another model can have stacked on top of it
	 */
	val stackables_m = new HashMap[LabwareModel, List[LabwareModel]]
	/**
	 * Each labware's model
	 */
	val labwareToModel_m = new HashMap[Labware, LabwareModel]
	/**
	 * Initial location of labware
	 */
	val labwareToLocation_m = new HashMap[Labware, Entity]
	
	def addAgent(e: Agent, name: String) {
		names(e) = name
	}
	
	def addModel(e: LabwareModel, name: String) {
		names(e) = name
	}
	
	def addSite(e: Site, name: String) {
		names(e) = name
	}
	
	def addDevice(a: Agent, d: Device, name: String) {
		assert(names.contains(a))
		names(d) = name
		agentToDevices_m.addBinding(a, d)
	}
	
	def setDeviceModels(d: Device, l: List[LabwareModel]) {
		assert(l.forall(names.contains))
		deviceToModels_m(d) = l
	}
	
	def setDeviceSites(d: Device, l: List[Site]) {
		assert(l.forall(names.contains))
		deviceToSites_m(d) = l
	}
	
	def addDeviceSpec(d: Device, spec: Entity, name: String) {
		assert(names.contains(d))
		names(spec) = name
		deviceToSpecs_m.addBinding(d, spec)
	}
	
	def setStackable(m: LabwareModel, l: List[LabwareModel]) {
		assert(names.contains(m))
		assert(l.forall(names.contains))
		stackables_m(m) = l
	}
	
	def setModel(l: Labware, m: LabwareModel) {
		assert(names.contains(l))
		assert(names.contains(m))
		labwareToModel_m(l) = m
	}
	
	def addLabware(e: Labware, name: String) {
		names(e) = name
	}

	def setLocation(l: Labware, e: Entity) {
		assert(names.contains(l))
		assert(names.contains(e))
		labwareToLocation_m(l) = e
	}
	
	def makeInitialConditionsList(): List[Rel] = {
		names.toList.map(pair => Rel(s"is-${pair._1.typeName}", List(pair._2))) ++
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
		labwareToModel_m.map(pair => Rel(s"stackable", List(names(pair._1), names(pair._2)))) ++
		labwareToLocation_m.map(pair => Rel(s"location", List(names(pair._1), names(pair._2))))
	}
	
	def makeInitialConditions(): String = {
		val l: List[Rel] = makeInitialConditionsList
		val l2: List[String] =
			" ; initial conditions" ::
			" (" ::
			l.map(r => "  " + r) ++
			List(" )")
		l2.mkString("\n")
	}
}
