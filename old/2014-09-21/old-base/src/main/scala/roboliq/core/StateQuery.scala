package roboliq.core


/**
 * Interface for finding objects and their states.
 */
trait StateQuery {
	/** Find tip model with ID `id`. */
	def findTipModel(id: String): Result[TipModel]
	
	/** Find substance with ID `id`. */
	def findSubstance(id: String): Result[Substance]
	/** Find liquid with ID `id`. */
	def findLiquid(id: String): Result[Liquid]
	/** Find tip with ID `id`. */
	def findTip(id: String): Result[Tip]
	/** Find location with ID `id`. */
	def findPlateLocation(id: String): Result[PlateLocation]
	/** Find plate with ID `id`. */
	def findPlate(id: String): Result[Plate]
	//def findWell(id: String): Result[Well]
	
	/** Find state of tip with ID `id`. */
	def findTipState(id: String): Result[TipState]
	/** Find state of plate with ID `id`. */
	def findPlateState(id: String): Result[PlateState]
	/** Find state of well with ID `id`. */
	def findWellState(id: String): Result[WellState]
	/** Find fully defined [[roboliq.core.Well]] of well with ID `id`. */
	def findWellPosition(id: String): Result[Well]
	
	/**
	 * Split @param ids into a list of ids which can be looked up.
	 * Substance, plate, and tube ids will remain unchanged,
	 * whereas well lists of the form P1(A01 d H01) will be expanded into
	 * individual well ids.
	 */
	def expandIdList(ids: String): Result[List[String]]
	
	/**
	 * Map @param id onto a list of [[roboliq.core.Well]].
	 * A liquid id will be mapped onto a list of one or more Well objects,
	 * depending on how many wells on the bench contain that liquid.
	 * A tube id will be mapped to a singleton Well list for the rack it's in and its index in the rack.
	 * A well id will be mapped to a singleton Well list (Well extends Well).
	 * A plate id will not be mapped to a Well list, and will return an error.
	 * All other ids will also return a error. 
	 */
	def mapIdToWell2List(id: String): Result[List[Well]]
	
	/**
	 * Map @param ids onto a list of [[roboliq.core.Well]] lists.
	 * A liquid id will be mapped onto a list of one or more Well objects,
	 * depending on how many wells on the bench contain that liquid.
	 * A tube id will be mapped to a singleton Well list for the rack it's in and its index in the rack.
	 * A well id will be mapped to a singleton Well list (Well extends Well).
	 * A plate id will not be mapped to a Well list, and will return an error.
	 * All other ids will also return a error. 
	 */
	def mapIdsToWell2Lists(ids: String): Result[List[List[Well]]]
	
	/**
	 * Find fully defined [[roboliq.core.Well]] information for the wells in string `ids`.
	 * @see [[roboliq.core.WellSpecParser]] for the format of `ids`.
	 */
	def findDestWells(ids: String): Result[List[Well]]
	
	/** Find the liquid with ID `id` or the liquid currently in the well with ID `id`. */
	def findSourceLiquid(id: String): Result[Liquid] =
		findLiquid(id).orElse(findWellState(id).map(_.liquid))
		
	/** Pair all `wells` with their fully defined [[roboliq.core.Well]]. */
	def getWellPosList(wells: Iterable[Vessel]): Result[List[Tuple2[Vessel, Well]]] = {
		Result.mapOver(wells.toList)(well => findWellPosition(well.id).map(well -> _))
	}
}
