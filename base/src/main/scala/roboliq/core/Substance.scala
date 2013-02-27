package roboliq.core

import scala.reflect.BeanProperty
import scalaz._
import Scalaz._


/**
 * Enumeration of the physical property classes of liquids as relevant to pipetting
 * (currently just Water or Glycerol).
 */
object LiquidPhysicalProperties extends Enumeration {
	val Water, Glycerol = Value
}

/**
 * Base trait YAML JavaBean for a substance
 * @see [[roboliq.core.Substance]]
 */
sealed trait SubstanceBean extends Bean

/** YAML JavaBean representation of [[roboliq.core.SubstanceDna]]. */
class SubstanceDnaBean extends SubstanceBean {
	@BeanProperty var sequence: String = null
	@BeanProperty var costPerUnit: java.math.BigDecimal = null
}

/** YAML JavaBean representation of [[roboliq.core.SubstanceOther]]. */
class SubstanceOtherBean extends SubstanceBean {
	@BeanProperty var costPerUnit: java.math.BigDecimal = null
}

/** YAML JavaBean representation of [[roboliq.core.SubstanceLiquid]]. */
class SubstanceLiquidBean extends SubstanceBean {
	@BeanProperty var physical: String = null
	@BeanProperty var cleanPolicy: String = null
	@BeanProperty var costPerUnit: java.math.BigDecimal = null
}

/** Represents a substance. */
sealed trait Substance {
	/** ID in database. */
	val id: String
	/** List of contaminants in this substance */
	val contaminants: Set[String]
	/** Cost per unit (either liter or mol) of the substance */
	val costPerUnit_? : Option[BigDecimal]
	/** Value per unit (either liter or mol) of the substance (this can be on a different scale than costPerUnit) */
	val valuePerUnit_? : Option[BigDecimal]

	val isEmpty: Boolean
	val isLiquid: Boolean
	val isSolid: Boolean
	
	val simpleAmounts: List[SubstanceAmount]
	
	/**
	 * Whether multipipetting is allowed.
	 * Multipipetting is when a tip aspirates once and distributes to that volume to
	 * multiple destinations.  In our case with our Tecan robot, this wastes more
	 * of the source liquid than single-pipetting, so for expensive liquids we
	 * want to prevent multipipetting.
	 */
	def expensive: Boolean = costPerUnit_?.filter(_ > 0).isDefined
}

object Substance {
	/** Convert [[roboliq.core.SubstanceBean]] to [[roboliq.core.Substance]]. */
	def fromBean(bean: SubstanceBean): Result[Substance] = {
		bean match {
			case b: SubstanceDnaBean => SubstanceDna.fromBean(b)
			case b: SubstanceLiquidBean => SubstanceLiquid.fromBean(b)
			case b: SubstanceOtherBean => SubstanceOther.fromBean(b)
		}
	}
	
	val Empty = new Substance {
		val id = "<EMPTY>"
		val contaminants = Set()
		val costPerUnit_? = None
		val valuePerUnit_? = None
		
		val isEmpty = true
		val isLiquid = false
		val isSolid = false
		val simpleMap: Map[SubstanceSimple, Amount] = Map()
	}
}

/**
 * A substance which can be defined directly, not as a mixture of other substances
 */
sealed trait SubstanceSimple extends Substance {
	val isEmpty = false
	val simpleMap: Map[SubstanceSimple, Amount] = Map(this -> Amount(1, RqUnit.None))
}

/**
 * A solid substance, in contrast to [[roboliq.core.SubstanceLiquid]].
 */
sealed trait SubstanceSolid extends Substance {
	val isLiquid = false
	val isSolid = true
}

/**
 * A solid which is defined directly, rather than as a mixture of other substances.
 */
sealed trait SubstanceSolidSimple extends SubstanceSolid {
	
}

/**
 * A liquid substance, in contrast to [[roboliq.core.SubstanceSolid]].
 */
sealed trait SubstanceLiquid extends Substance {
	val isLiquid = false
	val isSolid = true
}

/**
 * Represents a DNA-based substance.
 * @param id ID in database.
 * @param sequence_? optional DNA sequence string.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SolidDna(
	val id: String,
	val sequence_? : Option[String],
	val costPerUnit_? : Option[BigDecimal],
	val valuePerUnit_? : Option[BigDecimal]
) extends SubstanceSolidSimple {
	val contaminants = Set[String]("DNA")
}

object SolidDna {
	/** Convert [[roboliq.core.SubstanceDnaBean]] to [[roboliq.core.SubstanceDna]]. */
	def fromBean(bean: SubstanceDnaBean): Result[SubstanceDna] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val sequence = if (bean.sequence != null) Some(bean.sequence) else None
			val costPerUnit_? : Option[BigDecimal] = if (bean.costPerUnit == null) None else Some(bean.costPerUnit)
			new SubstanceDna(id, sequence, costPerUnit_?, None)
		}
	}
}

/**
 * Represents the catch-all case of a substance which isn't DNA or a liquid.
 * @param id ID in database.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SolidOther(
	val id: String,
	val contaminants: Set[String],
	val costPerUnit_? : Option[BigDecimal],
	val valuePerUnit_? : Option[BigDecimal]
) extends SubstanceSolidSimple

object SolidOther {
	/** Convert [[roboliq.core.SubstanceOtherBean]] to [[roboliq.core.SubstanceOther]]. */
	def fromBean(bean: SubstanceOtherBean): Result[SolidOther] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val costPerUnit_? : Option[BigDecimal] = if (bean.costPerUnit == null) None else Some(bean.costPerUnit)
			new SolidOther(id, Set(), costPerUnit_?, None)
		}
	}
}

/**
 * Represents a DNA-based substance.
 * @param id ID in database.
 * @param physicalProperties physical properties of the liquid (e.g. water or glycerol).
 * @param cleanPolicy tip cleaning policy when handling this liquid.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class LiquidSimple(
	val id: String,
	val cleanPolicy: TipCleanPolicy,
	val viscosity: BigDecimal,
	val contaminants: Set[String],
	val costPerUnit_? : Option[BigDecimal],
	val valuePerUnit_? : Option[BigDecimal]
) extends SubstanceLiquid {
	val isLiquid = true
	val isSolid = false
	val physicalProperties: LiquidPhysicalProperties.Value = 
		if (viscosity > 0.1) LiquidPhysicalProperties.Glycerol else LiquidPhysicalProperties.Water
}

object SubstanceLiquid {
	/** Convert [[roboliq.core.SubstanceLiquidBean]] to [[roboliq.core.SubstanceLiquid]]. */
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
				if (bean.cleanPolicy == null) TipCleanPolicy.Thorough
				else {
					bean.cleanPolicy match {
						case "T" => TipCleanPolicy.Thorough
						case "D" => TipCleanPolicy.Decontaminate
						case "TNL" => TipCleanPolicy.TL
						case _ => return Error("unrecognized `cleanPolicy` value \""+bean.cleanPolicy+"\"")
					}
				}
			}
			val costPerUnit_? : Option[BigDecimal] = if (bean.costPerUnit == null) None else Some(bean.costPerUnit)

			new SubstanceLiquid(id, cleanPolicy, 0, Set(), costPerUnit_?, None)
		}
	}
}

sealed trait SubstanceMixture extends Substance {
	val list: List[SubstanceAmount]

	def isEmpty = list.isEmpty
	def isLiquid = list.exists(_.substance.isInstanceOf[SubstanceLiquid])
	def isSolid = !list.isEmpty && !isLiquid
	def id = list.mkString("+")
	override def toString = id
}

case class Liquid0(
	list: List[SubstanceAmount]
) extends SubstanceMixture {
	private val substance_l = list.map(_.substance)
	private val liquid_l = substance_l.collect({case x: SubstanceLiquid => x})
	private val solid_l = substance_l.collect({case x: SubstanceSolid => x})
	
	val simpleList: List[(SubstanceSimple, Amount)] = list.flatMap(sa => sa.substance match {
		case sub: SubstanceSimple => List(sub -> sa)
		case sub: Liquid0 => sub.map.toList
	}).groupBy(_._1).mapValues(l => l)

	val viscosity = list.foldLeft(BigDecimal(0)){(acc, x) =>
		
	}
	val physicalProperties: LiquidPhysicalProperties.Value = if (liquid_l.exists(_.physicalProperties == LiquidPhysicalProperties.Glycerol)) LiquidPhysicalProperties.Glycerol else LiquidPhysicalProperties.Water
	val cleanPolicy: TipCleanPolicy = liquid_l.map(_.cleanPolicy).concatenate
	val contaminants: Set[String] = substance_l.map(_.contaminants).concatenate
	val costPerUnit_? : Option[BigDecimal] = substance_l.map(_.costPerUnit_?).concatenate
	val valuePerUnit_? : Option[BigDecimal] = substance_l.map(_.valuePerUnit_?).concatenate
}

object RqUnit extends Enumeration {
	val None, l, mol, g = Value
}

case class Amount(n: BigDecimal, unit: RqUnit.Value) {
	override def toString = {
		val s = if (unit == RqUnit.None) "" else unit.toString
		s"$n$s"
	}
}
/*
((A)@a+(B)@b)@c => (A)@(a/(a+b)*c)+(B)@(b/(a+b)*c) 
*/
case class SolidMixture(list: List[SolidAmount]) extends SubstanceSolid {
	val simpleAmounts = {
		val 
		list.flatMap(_.simpleAmounts)
	}
}

sealed trait HasLiquid
sealed trait HasSolid
sealed trait SubstanceAmount
case class LiquidAmount(substance: Substance with HasLiquid, num: LiquidVolume, den: LiquidVolume) extends SubstanceAmount {
	override def toString = s"(${substance.id})@${volume}"

	def +(sa: SubstanceAmount): SubstanceAmount = {
		sa match {
			
		}
	}
}

case class SolidAmount(substance: Substance with HasSolid, num: BigDecimal, den: LiquidVolume) extends SubstanceAmount {
	override def toString = s"(${substance.id})@${mol}mol"
	
	def +(sa: SubstanceAmount): SubstanceAmount = {
		sa match {
			case LiquidAmount(liquid, volume) =>
			case SolidAmount(solid2, mol2) =>
		}
	}

}
