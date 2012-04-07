package roboliq.core

trait StateQuery {
	def findLiquid(id: String): Result[Liquid]
	def findWell(id: String): Result[Well]
	def findWellState(id: String): Result[WellState]
}