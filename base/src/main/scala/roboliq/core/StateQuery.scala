package roboliq.core

trait StateQuery {
	def findLiquid(id: String): Result[Liquid]
	def findTip(id: String): Result[Tip]
	def findTipState(id: String): Result[TipState]
	def findWell(id: String): Result[Well]
	def findWellState(id: String): Result[WellState]
}