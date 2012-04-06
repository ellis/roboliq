package roboliq.core

abstract class DeviceBean extends PartBean {
	def setObjBase(ob: ObjBase): Result[Unit]
}