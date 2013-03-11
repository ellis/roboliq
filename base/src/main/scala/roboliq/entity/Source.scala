package roboliq.entity

/**
 * Represents a liquid source.
 * The ID may reference a tube, a plate, a plate well, or a substance.
 * If ID references a tube or a well, then the `vessels` list will contain a single vessel.
 * Otherwise, it may contain multiple vessels.
 * The id has one of the following prefixes:
 * - "L:" for liquid
 * - "W:" for plate well or tube
 * - "P:" for an entire plate
 * @param vessels a list of situated vessels containing the liquid.
 */
case class Source(
	id: String,
	vessels: List[VesselSituatedState]
)
