package roboliq.common

class ObjMapper(
	val configL1: Map[Obj, AbstractConfigL1],
	val configL3: Map[Obj, AbstractConfigL3],
	val state0L1: Map[Obj, AbstractStateL1],
	val state0L3: Map[Obj, AbstractStateL3]
	)