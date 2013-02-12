package roboliq

import roboliq.core._

package object processor2 {
	type ComputationResult = RqResult[List[ComputationItem]]
	type ConversionResult = RqResult[List[ConversionItem]]
	type RqArgs = List[KeyClassOpt]
	type RqInputs = List[Object]
	type RqReturn = RqResult[List[RqItem]]
	type RqFunction = List[Object] => RqReturn
	type RqFunctionArgs = (RqFunction, RqArgs)
	type RqFunctionInputs = (RqFunction, RqInputs)
}