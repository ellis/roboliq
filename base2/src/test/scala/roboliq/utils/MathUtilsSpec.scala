package roboliq.utils

import org.scalatest.FunSpec


class MathUtilsSpec extends FunSpec {
	describe("MathUtils") {
		import MathUtils._
		
		it("toChemistString3 should round up") {
			assert(toChemistString3(BigDecimal("1.543")) === "1.54")
			assert(toChemistString3(BigDecimal("1.545")) === "1.55")
			assert(toChemistString3(BigDecimal("1.535")) === "1.54")
		}

		it("toChemistString3 should use m,u,n,p notation") {
			assert(toChemistString3(BigDecimal("0.1543")) === "154m")
			assert(toChemistString3(BigDecimal("0.01543")) === "15.4m")
			assert(toChemistString3(BigDecimal("0.001543")) === "1.54m")
			assert(toChemistString3(BigDecimal("0.0001543")) === "154u")
			assert(toChemistString3(BigDecimal("0.00001543")) === "15.4u")
			assert(toChemistString3(BigDecimal("0.000001543")) === "1.54u")
			assert(toChemistString3(BigDecimal("0.0000001543")) === "154n")
			assert(toChemistString3(BigDecimal("0.00000001543")) === "15.4n")
			assert(toChemistString3(BigDecimal("0.000000001543")) === "1.54n")
			assert(toChemistString3(BigDecimal("0.0000000001543")) === "154p")
			assert(toChemistString3(BigDecimal("0.00000000001543")) === "15.4p")
			assert(toChemistString3(BigDecimal("0.000000000001543")) === "1.54p")
			assert(toChemistString3(BigDecimal("0.0000000000001543")) === "154E-15")
		}
	}
}