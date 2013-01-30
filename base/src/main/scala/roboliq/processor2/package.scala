package roboliq

import roboliq.core._

package object processor2 {
	type ComputationResult = RqResult[List[ComputationItem]]
	type ConversionResult = RqResult[List[ConversionItem]]
}