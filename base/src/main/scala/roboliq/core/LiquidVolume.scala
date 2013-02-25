package roboliq.core


/** Represents a volume of liquid. */
class LiquidVolume private (val _nl: Int) extends Ordered[LiquidVolume] {
	/** Volume in nanoliters [nl] */
	def nl: BigDecimal = _nl
	/** Volume in microliters [ul] */
	def ul: BigDecimal = (nl / 1000)
	/** Volume in milliliters [ml] */
	def ml: BigDecimal = (nl / 1000000)
	/** Volume in liters [l] */
	def l: BigDecimal = (nl / 1000000000)
	
	def -(that: LiquidVolume): LiquidVolume = new LiquidVolume(_nl - that._nl)
	def +(that: LiquidVolume): LiquidVolume = new LiquidVolume(_nl + that._nl)
	def *(n: BigDecimal): LiquidVolume = new LiquidVolume((n * _nl).toInt)
	def /(n: BigDecimal): LiquidVolume = new LiquidVolume((BigDecimal(_nl) / n).toInt)
	
	/** Return true if the volume is 0. */
	def isEmpty: Boolean = (_nl == 0)
	/*
	def <(that: LiquidVolume): Boolean = (_nl < that._nl)
	def <=(that: LiquidVolume): Boolean = (_nl <= that._nl)
	def >(that: LiquidVolume): Boolean = (_nl > that._nl)
	def >=(that: LiquidVolume): Boolean = (_nl >= that._nl)
	*/
	def ==(that: LiquidVolume): Boolean = (_nl == that._nl)
	def !=(that: LiquidVolume): Boolean = (_nl != that._nl)
	
	override def compare(that: LiquidVolume): Int = {
		_nl - that._nl
	}
	
	override def equals(that: Any): Boolean = {
		assert(that.isInstanceOf[LiquidVolume])
		_nl == that.asInstanceOf[LiquidVolume]._nl
	}
	override def hashCode = _nl.hashCode()
	
	override def toString = {
		if (_nl >= 1000000)
			(nl / 1000000).toString + "ml"
		else if (_nl >= 1000)
			(nl / 1000).toString + "ul"
		else
			nl.toString + "nl"
	}
}

/** Factory for [[roboliq.protocol.LiquidVolume]] */
object LiquidVolume {// extends Semigroup[LiquidVolume] {
	def nl(n: Int): LiquidVolume = new LiquidVolume(n)
	def nl(n: BigDecimal): LiquidVolume = new LiquidVolume(n.toInt)
	def ul(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000).toInt)
	def ml(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000).toInt)
	def l(n: BigDecimal): LiquidVolume = new LiquidVolume((n * 1000000000).toInt)
	
	val empty: LiquidVolume = new LiquidVolume(0)
	
	def max(a: LiquidVolume, b: LiquidVolume): LiquidVolume = {
		if (a._nl > b._nl) a else b
	}
	
	def min(a: LiquidVolume, b: LiquidVolume): LiquidVolume = {
		if (a._nl < b._nl) a else b
	}
	
	//def append(a: LiquidVolume, b: => LiquidVolume): LiquidVolume = a + b
}
