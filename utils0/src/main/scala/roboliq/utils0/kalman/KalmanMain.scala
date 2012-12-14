package roboliq.utils0.kalman

// Questions:
//  Adding liquids multiple times without measurements
//  Measuring multiple times without adding volumes
//  Initial level known with *much* less accuracy, but level changes should be more accurate

object KalmanMain {
	
	case class State(vol: BigDecimal, variance: BigDecimal) {
		def plusLiquid(vol: BigDecimal, variance: BigDecimal): State = {
			State(this.vol + vol, this.variance + variance)
		}
	
		def plusMeasurement(vol: BigDecimal, variance: BigDecimal): State = {
			val residual = vol - this.vol
			val r = this.variance + variance
			val k = this.variance / r
			State(this.vol + k * residual, this.variance - k * k * r)
		}
	}

	def run(
		Y: List[BigDecimal], S: List[BigDecimal], m: List[BigDecimal], Q: List[BigDecimal],
		state0: State = State(0, 0)
	): State = {
		if (Y.isEmpty)
			state0
		else {
			val stateA = state0.plusLiquid(m.head, Q.head)
			println("A: "+stateA)
			val stateB = stateA.plusMeasurement(Y.head, S.head)
			println("B: "+stateB)
			run(Y.tail, S.tail, m.tail, Q.tail, stateB)
		}
	}
	
	
	def main(args: Array[String]) {
		val Y = List[BigDecimal](2, 3.6, 6.2, 8.8, 14.7)
		val S = List[BigDecimal](0.1, 0.1, 0.1, 0.1, 0.01)
		val m = List[BigDecimal](2, 2, 2, 2, 2)
		val Q = List[BigDecimal](0.2, 0.2, 0.2, 0.2, 0.2)
		//val lowLim = List[BigDecimal](1, 3, 5, 7, 9)
		//val uppLim = List[BigDecimal](3, 5, 7, 9, 11)
		
		run(Y, S, m, Q)

		
		val measurement_stddev: BigDecimal = 1.0
	}
}
