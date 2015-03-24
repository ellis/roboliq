package roboliq.entities


/**
 * Enumeration for where the tips should be positioned with respect to the liquid being pipetted.
 * `Free` means in the air, `WetContact` means just below the liquid surface,
 * `DryContact` means just above the bottom of an empty vessel.
 */
object PipettePosition extends Enumeration {
	val Free, WetContact, DryContact = Value
	
	def getPositionFromPolicyNameHack(policy: String): PipettePosition.Value = {
		if (policy.contains("Air") || policy.contains("_A_")) PipettePosition.Free
		else if (policy.contains("Dry")) PipettePosition.DryContact
		else PipettePosition.WetContact
	}
}

/** Basically a tuple of a pipette policy name and the position of the tips while pipetting. */
case class PipettePolicy(id: String, pos: PipettePosition.Value) {
	override def equals(that: Any): Boolean = {
		that match {
			case b: PipettePolicy => id == b.id
			case _ => false
		}
	}
}

object PipettePolicy {
	def fromName(name: String): PipettePolicy = {
		val pos = PipettePosition.getPositionFromPolicyNameHack(name)
		PipettePolicy(name, pos)
	}
}
