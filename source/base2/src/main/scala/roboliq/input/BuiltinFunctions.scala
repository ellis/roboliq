package roboliq.input

import roboliq.core.RsResult
import spray.json.JsValue

case class BuiltinAddParams(
	numbers: List[BigDecimal] 
)

class BuiltinAdd {
	def evaluate(): ResultE[RjsValue] = {
		ResultE.context("add") {
			for {
				params <- ResultE.fromScope[BuiltinAddParams]()
			} yield {
				RjsNumber(params.numbers.sum, None)
			}
		}
	}
}
