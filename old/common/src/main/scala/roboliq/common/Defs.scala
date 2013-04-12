package roboliq.common

import scala.collection.mutable.ArrayBuffer

import roboliq.common._


trait RoboliqCommands {
	val cmds: ArrayBuffer[Command]
}

abstract class LiquidPropertiesFamily
object LiquidPropertiesFamily {
	case object Water extends LiquidPropertiesFamily
	case object Cells extends LiquidPropertiesFamily
	case object DMSO extends LiquidPropertiesFamily
	case object Decon extends LiquidPropertiesFamily
}

object WashIntensity extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
	
	def max(a: WashIntensity.Value, b: WashIntensity.Value): WashIntensity.Value = {
		if (a >= b) a else b
	}
	
	def max(l: Traversable[WashIntensity.Value]): WashIntensity.Value = {
		l.foldLeft(WashIntensity.None)((acc, intensity) => max(acc, intensity))
	}
}

class LiquidVolume private (_nl: Int) {
	/** Volume in nanoliters [nl] */ 
	def nl: BigDecimal = _nl
	/** Volume in microliters [ul] */ 
	def ul: BigDecimal = (nl / 1000.0)
	override def toString = {
		if (_nl > 1000000)
			(nl / 1000000).toString + " ml"
		else if (_nl > 1000)
			(nl / 1000).toString + " ul"
		else
			nl.toString + " nl"
	}
}
/** Factory for [[roboliq.protocol.LiquidVolume]] */
object LiquidVolume {
	def nl(n: Int): LiquidVolume = new LiquidVolume(n)
	def ul(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000).toInt)
	def ml(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000).toInt)
	def l(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000000).toInt)
}
