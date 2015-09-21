package roboliq.utils

object LogScale {
	def getList(min: BigDecimal, max: BigDecimal, count: Int): List[BigDecimal] = {
		// min*k^(count-1) = max
		// k^(count - 1) = max/min
		// (count - 1)*ln(k) = ln(max/min)
		// ln(k) = ln(max/min)/(count - 1)
		// k = exp(ln(max/min)/(count - 1))
		val k = math.exp(math.log((max / min).toDouble) / (count - 1))
		
		def step(v: BigDecimal, n: Int): List[BigDecimal] = {
			if (n == 0) List(v)
			else v :: step(v * k, n - 1)
		}
		step(min, count - 1)
	}
	
	//def roundForSignificantDigits(n: BigDecimal, digits: Int): BigDecimal = {
	//	n.precision
	//}
}