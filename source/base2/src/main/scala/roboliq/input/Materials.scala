package roboliq.input

sealed trait Material {
	val name: String
	val `type`: String
	val label_? : Option[String]
	val description_? : Option[String]
}

case class PlateMaterial(
	name: String,
	`type`: String,
	label_? : Option[String],
	description_? : Option[String]
) extends Material

case class LiquidMaterial(
	name: String,
	`type`: String,
	label_? : Option[String],
	description_? : Option[String]
) extends Material