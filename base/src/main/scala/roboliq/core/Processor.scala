package roboliq.core

class Processor {

}

class ProcessorContext(
	val processor: Processor,
	val ob: ObjBase,
	val states: RobotState
)
