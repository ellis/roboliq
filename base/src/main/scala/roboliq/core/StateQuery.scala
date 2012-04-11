package roboliq.core

trait StateQuery {
	def findTipModel(id: String): Result[TipModel]
	
	def findSubstance(id: String): Result[Substance]
	def findLiquid(id: String): Result[Liquid]
	def findTip(id: String): Result[Tip]
	def findPlate(id: String): Result[Plate]
	def findWell(id: String): Result[Well]
	
	def findTipState(id: String): Result[TipState]
	def findWellState(id: String): Result[WellState]
	def findWellPosition(id: String): Result[Well2]
	
	/**
	 * Split @param ids into a list of ids which can be looked up.
	 * Substance, plate, and tube ids will remain unchanged,
	 * whereas well lists of the form P1(A01 d H01) will be expanded into
	 * individual well ids.
	 */
	def expandIdList(ids: String): Result[List[String]]
	
	/**
	 * Map @param id onto a list of [[roboliq.core.Well2]].
	 * A liquid id will be mapped onto a list of one or more Well2 objects,
	 * depending on how many wells on the bench contain that liquid.
	 * A tube id will be mapped to a singleton Well2 list for the rack it's in and its index in the rack.
	 * A well id will be mapped to a singleton Well list (Well extends Well2).
	 * A plate id will not be mapped to a Well2 list, and will return an error.
	 * All other ids will also return a error. 
	 */
	def mapIdToWell2List(id: String): Result[List[Well2]]
	
	/**
	 * Map @param ids onto a list of [[roboliq.core.Well2]] lists.
	 * A liquid id will be mapped onto a list of one or more Well2 objects,
	 * depending on how many wells on the bench contain that liquid.
	 * A tube id will be mapped to a singleton Well2 list for the rack it's in and its index in the rack.
	 * A well id will be mapped to a singleton Well list (Well extends Well2).
	 * A plate id will not be mapped to a Well2 list, and will return an error.
	 * All other ids will also return a error. 
	 */
	def mapIdsToWell2Lists(ids: String): Result[List[List[Well2]]]
	
	/** get liquid from liquid or well state */
	def findSourceLiquid(id: String): Result[Liquid] =
		findLiquid(id).orElse(findWellState(id).map(_.liquid))
		
	def getWellPosList(wells: Iterable[Well]): Result[List[Tuple2[Well, Well2]]] = {
		Result.mapOver(wells.toList)(well => findWellPosition(well.id).map(well -> _))
	}
}