package roboliq.core

trait StateQuery {
	def findLiquid(id: String): Result[Liquid]
	def findTip(id: String): Result[Tip]
	def findTipState(id: String): Result[TipState]
	def findPlate(id: String): Result[Plate]
	def findWell(id: String): Result[Well]
	def findWellState(id: String): Result[WellState]
	def findWellPosition(id: String): Result[WellPosition]
	
	/** get liquid from liquid or well state */
	def findSourceLiquid(id: String): Result[Liquid] =
		findLiquid(id).orElse(findWellState(id).map(_.liquid))
		
	def getWellPosList(wells: Iterable[Well]): Result[List[Tuple2[Well, WellPosition]]] = {
		Result.mapOver(wells.toList)(well => findWellPosition(well.id).map(well -> _))
	}
}