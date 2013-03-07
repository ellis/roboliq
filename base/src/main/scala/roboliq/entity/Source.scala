package roboliq.entity

/**
 * Represents a liquid source.
 * The ID may reference a tube, a plate well, or a substance.
 * If ID references a tube or a well, then the `vessels` list will contain a single vessel.
 * Otherwise, it may contain multiple vessels.
 * @param vessels a list of situated vessels containing the liquid.
 */
case class Source(
	id: String,
	vessels: List[VesselSituatedState]
)
