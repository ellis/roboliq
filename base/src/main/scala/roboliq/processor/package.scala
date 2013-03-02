package roboliq

import roboliq.core._

package object processor {
	type RqArgs = List[KeyClassOpt]
	type RqInputs = List[Object]
	type RqReturn = RqResult[List[RqItem]]
	type RqFunction = List[Object] => RqReturn
	type RqFunctionInputs = (RqFunction, RqInputs)
}