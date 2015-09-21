package roboliq.core

/**
 * Trait for YAML beans which require a database ID
 */
trait Bean {
	/** Database ID */
	var _id: String = null
}
