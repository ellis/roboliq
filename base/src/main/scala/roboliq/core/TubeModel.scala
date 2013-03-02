package roboliq.core


/**
 * Represents a tube model.
 * 
 * @see [[roboliq.core.Tube]]
 * 
 * @param id ID in database.
 * @param volume maximum volume which the tube can hold.
 */
case class TubeModel(
	val id: String,
	val volume: LiquidVolume
)
