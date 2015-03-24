package roboliq.utils

object MathUtils {
	/*
	def gcd(a: Long, b: Long): Long = {
		if (b == 0) a
		else gcd(b, a % b)
	}
	
	def gcd(l: List[Long]): Long =
		l.foldLeft(1L)(gcd)
	*/
	
	val mathContext3 = new java.math.MathContext(3)
	
	def toDecimalString3(n: BigDecimal): String = {
		n.round(mathContext3).toString
	}
	
	def toChemistString3(n: BigDecimal, suffix: String = ""): String = {
		val zeros = n.scale - n.precision
		val a = n.round(mathContext3).bigDecimal
		val s = {
			if (zeros >= 12)
				a.toEngineeringString()
			else if (zeros >= 9)
				a.movePointRight(12).toString + "p"
			else if (zeros >= 6)
				a.movePointRight(9).toString + "n"
			else if (zeros >= 3)
				a.movePointRight(6).toString + "u"
			else if (zeros >= 0)
				a.movePointRight(3).toString + "m"
			else
				a.toEngineeringString()
		}
		s + suffix
	}
}
