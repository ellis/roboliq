package roboliq.core

/**
 * Base class for peripheral devices on a robot.
 */
abstract class DeviceBean extends PartBean {
	/**
	 * This will be called to let the device implementation know which [[roboliq.core.ObjBase]]
	 * it should use if it needs to look up data or add data to the ObjBase.
	 */
	def setObjBase(ob: ObjBase): Result[Unit]
}