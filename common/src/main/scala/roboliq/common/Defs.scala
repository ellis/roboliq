package roboliq.common

import scala.collection.mutable.ArrayBuffer

import roboliq.common._


trait RoboliqCommands {
	val cmds: ArrayBuffer[Command]
}

object WashIntensity extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
	
	def max(a: WashIntensity.Value, b: WashIntensity.Value): WashIntensity.Value = {
		if (a >= b) a else b
	}
}
