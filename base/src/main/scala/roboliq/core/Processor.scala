package roboliq.core

class Processor {

}

class ProcessorContext(
	val processor: Processor,
	val ob: ObjBase,
	val builder_? : Option[StateBuilder],
	val states: RobotState
)
