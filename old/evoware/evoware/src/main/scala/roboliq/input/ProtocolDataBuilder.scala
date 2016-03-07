package roboliq.input

import roboliq.ai.plan.Unique
import roboliq.ai.strips
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import roboliq.core.ResultC


class ProtocolDataBuilder {
	private val labObjects = new HashMap[String, RjsBasicMap]
	private val planningDomainObjects = new HashMap[String, String]
	private val planningInitialState = new ArrayBuffer[strips.Literal]
	
	def get: ProtocolData = {
		new ProtocolData(
			labObjects = labObjects.toMap,
			planningDomainObjects = planningDomainObjects.toMap,
			planningInitialState = strips.Literals(Unique(planningInitialState.toList : _*))
		)
	}
	
	def addObject(name: String, value: RjsBasicMap) {
		labObjects(name) = value
	}
	
	def addObject(name: String, value: LabObject) {
		val rjs = RjsValue.fromObject(value).run()._2.get.asInstanceOf[RjsBasicMap]
		addObject(name, rjs)
	}
	
	def addPlanningDomainObject(name: String, obj: RjsBasicMap) {
		(
			for {
				typ_? <- RjsConverterC.fromRjs[Option[String]](obj, "type")
			} yield {
				typ_?.foreach { typ =>
					planningDomainObjects(name) = typ
				}
			}
		).run()
	}
	
	def addPlanningDomainObject(name: String, typ: String) {
		planningDomainObjects(name) = typ
	}
	
	def addPlateModel(plateModelName: String, plateModel: PlateModelObject) {
		val rjsPlateModel = RjsValue.fromObject(plateModel).run()._2.get.asInstanceOf[RjsBasicMap]
		addObject(plateModelName, rjsPlateModel)
		addPlanningDomainObject(plateModelName, rjsPlateModel)
	}
	
	def addSiteModel(siteModelName: String) {
		planningDomainObjects(siteModelName) = "SiteModel"
	}
	
	def addSite(siteName: String) {
		planningDomainObjects(siteName) = "Site"
	}
	
	def addSite(name: String, value: SiteObject) {
		val rjs = RjsValue.fromObject(value).run()._2.get.asInstanceOf[RjsBasicMap]
		addObject(name, rjs)
		addPlanningDomainObject(name, rjs)
	}
	
	/**
	 * Indicates that the 'top' model can be stacked on top of the 'bottom' model
	 */
	def appendStackable(modelNameBottom: String, modelNameTop: String) {
		planningInitialState += strips.Literal(true, "stackable", modelNameBottom, modelNameTop)
	}
	
	/**
	 * Indicates that the 'top' model can be stacked on top of the 'bottom' model
	 */
	def appendStackables(modelNameBottom: String, modelNameTop_l: Iterable[String]) {
		modelNameTop_l.foreach { modelNameTop =>
			planningInitialState += strips.Literal(true, "stackable", modelNameBottom, modelNameTop)
		}
	}
	
	/**
	 * Indicates that given agent can operate the given device
	 */
	def appendAgentDevice(agentName: String, deviceName: String) {
		planningInitialState += strips.Literal(true, "agent-has-device", agentName, deviceName)
	}
	
	/**
	 * Indicates that given device can handle the given model
	 */
	def appendDeviceModel(deviceName: String, modelName: String) {
		planningInitialState += strips.Literal(true, "device-can-model", deviceName, modelName)
	}
	
	/**
	 * Indicates that given device can handle the given site
	 */
	def appendDeviceSite(deviceName: String, siteName: String) {
		planningInitialState += strips.Literal(true, "device-can-site", deviceName, siteName)
	}
	
	/**
	 * Indicates that transporter can handle the given site using the given program
	 */
	def appendTransporterCan(deviceName: String, siteName: String, programName: String) {
		planningInitialState += strips.Literal(true, "transporter-can", deviceName, siteName, programName)
	}
	
	def setModel(elementName: String, modelName: String) {
		planningInitialState += strips.Literal(true, "model", elementName, modelName)
	}
}
