package roboliq.core
import scala.reflect.BeanProperty


object LiquidPhysicalProperties extends Enumeration {
	val Water, Glycerol = Value
}

sealed trait SubstanceBean extends Bean

/** Represents a DNA-based substance in YAML */
class SubstanceDnaBean extends SubstanceBean {
	@BeanProperty var sequence: String = null
	@BeanProperty var allowMultipipette: java.lang.Boolean = null
}

/** Represents some other substance in YAML */
class SubstanceOtherBean extends SubstanceBean {
	@BeanProperty var allowMultipipette: java.lang.Boolean = null
}

/** Represents a liquid substance in YAML */
class SubstanceLiquidBean extends SubstanceBean {
	@BeanProperty var physical: String = null
	@BeanProperty var cleanPolicy: String = null
	@BeanProperty var allowMultipipette: java.lang.Boolean = null
}

sealed trait Substance {
	val id: String
	val allowMultipipette: Boolean
}

object Substance {
	def fromBean(bean: SubstanceBean): Result[Substance] = {
		bean match {
			case b: SubstanceDnaBean => SubstanceDna.fromBean(b)
			case b: SubstanceLiquidBean => SubstanceLiquid.fromBean(b)
			case b: SubstanceOtherBean => SubstanceOther.fromBean(b)
		}
	}
}

case class SubstanceDna(
	val id: String,
	val sequence_? : Option[String],
	val allowMultipipette: Boolean
) extends Substance

object SubstanceDna {
	def fromBean(bean: SubstanceDnaBean): Result[SubstanceDna] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val sequence = if (bean.sequence != null) Some(bean.sequence) else None
			val allowMultipipette: Boolean = if (bean.allowMultipipette == null) true else bean.allowMultipipette
			new SubstanceDna(id, sequence, allowMultipipette)
		}
	}
}

case class SubstanceOther(
	val id: String,
	val allowMultipipette: Boolean
) extends Substance

object SubstanceOther {
	def fromBean(bean: SubstanceOtherBean): Result[SubstanceOther] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val allowMultipipette: Boolean = if (bean.allowMultipipette == null) true else bean.allowMultipipette
			new SubstanceOther(id, allowMultipipette)
		}
	}
}

case class SubstanceLiquid(
	val id: String,
	val physicalProperties: LiquidPhysicalProperties.Value, 
	val cleanPolicy: GroupCleanPolicy,
	val allowMultipipette: Boolean
) extends Substance

object SubstanceLiquid {
	def fromBean(bean: SubstanceLiquidBean): Result[SubstanceLiquid] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			id <- Result.mustBeSet(bean._id, "_id")
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val physicalProperties = {
				if (bean.physical == null) LiquidPhysicalProperties.Water
				else LiquidPhysicalProperties.withName(bean.physical)
			}
			val cleanPolicy = {
				if (bean.cleanPolicy == null) GroupCleanPolicy.Thorough
				else {
					bean.cleanPolicy match {
						case "T" => GroupCleanPolicy.Thorough
						case "D" => GroupCleanPolicy.Decontaminate
						case "TNL" => GroupCleanPolicy.TNL
						case _ => return Error("unrecognized `cleanPolicy` value \""+bean.cleanPolicy+"\"")
					}
				}
			}
			val allowMultipipette: Boolean = {
				if (bean.allowMultipipette == null) true
				else bean.allowMultipipette
			}

			new SubstanceLiquid(id, physicalProperties, cleanPolicy, allowMultipipette)
		}
	}
}
