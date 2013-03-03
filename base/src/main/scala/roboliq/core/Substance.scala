package roboliq.core

import scalaz._
import Scalaz._


//case class CelciusToLiterPerMole(celcius: BigDecimal, literPerMole: BigDecimal)
case class CelciusAndConcToViscosity(celcius: BigDecimal, conc: BigDecimal, viscosity: BigDecimal)

object SubstanceKind extends Enumeration {
	val None, Liquid, Dna, Other = Value
}

/** Represents a substance. */
case class Substance(
	/** ID in database. */
	val id: String,
	/** Kind of substance */
	val kind: SubstanceKind.Value,
	/** Tip cleaning policy when handling this substance with pipetter. */
	val tipCleanPolicy: TipCleanPolicy,
	/** List of contaminants in this substance */
	val contaminants: Set[String],
	/** Cost per unit (either liter or mol) of the substance */
	val costPerUnit_? : Option[BigDecimal],
	/** Value per unit (either liter or mol) of the substance (this can be on a different scale than costPerUnit) */
	val valuePerUnit_? : Option[BigDecimal],
	
	val gramPerMole_? : Option[BigDecimal],
	val literPerMole_? : Option[BigDecimal],
	val celciusAndConcToViscosity: List[CelciusAndConcToViscosity],

	val sequence_? : Option[String]
) {
	val isEmpty: Boolean = (kind == SubstanceKind.None)
	val isLiquid: Boolean = (kind == SubstanceKind.Liquid) 
	def literPerMole: BigDecimal = literPerMole_?.getOrElse(0)
	
	/**
	 * Whether multipipetting is allowed.
	 * Multipipetting is when a tip aspirates once and distributes to that volume to
	 * multiple destinations.  In our case with our Tecan robot, this wastes more
	 * of the source liquid than single-pipetting, so for expensive liquids we
	 * want to prevent multipipetting.
	 */
	def expensive: Boolean = costPerUnit_?.filter(_ > 0).isDefined
	
//	def literPerMoleAt(celcius: BigDecimal): Option[BigDecimal] = {
//		celciusToLiterPerMole.find(_.celcius == celcius).map(_.literPerMole)
//	}
}

object Substance {
	/*
	/** Convert [[roboliq.core.SubstanceBean]] to [[roboliq.core.Substance]]. */
	def fromBean(bean: SubstanceBean): Result[Substance] = {
		bean match {
			case b: SubstanceDnaBean => SubstanceDna.fromBean(b)
			case b: SubstanceLiquidBean => SubstanceLiquid.fromBean(b)
			case b: SubstanceOtherBean => SubstanceOther.fromBean(b)
		}
	}
	*/
	
	val Empty = Substance(
		id = "<EMPTY>",
		kind = SubstanceKind.None,
		tipCleanPolicy = TipCleanPolicy.NN,
		contaminants = Set(),
		costPerUnit_? = None,
		valuePerUnit_? = None,
		gramPerMole_? = None,
		literPerMole_? = None,
		celciusAndConcToViscosity = Nil,
		sequence_? = None
	)
	
	def liquid(
		id: String,
		literPerMole: BigDecimal,
		tipCleanPolicy: TipCleanPolicy = TipCleanPolicy.TT,
		contaminants: Set[String] = Set(),
		costPerUnit_? : Option[BigDecimal] = None,
		valuePerUnit_? : Option[BigDecimal] = None,
		celciusAndConcToViscosity: List[CelciusAndConcToViscosity] = Nil
	): Substance = {
		Substance(
			id = id,
			kind = SubstanceKind.Liquid,
			tipCleanPolicy = tipCleanPolicy,
			contaminants = contaminants,
			costPerUnit_? = costPerUnit_?,
			valuePerUnit_? = valuePerUnit_?,
			gramPerMole_? = None,
			literPerMole_? = Some(literPerMole),
			celciusAndConcToViscosity = celciusAndConcToViscosity,
			sequence_? = None
		)
	}
	
	def dna(
		id: String,
		sequence_? : Option[String] = None,
		costPerUnit_? : Option[BigDecimal] = None,
		valuePerUnit_? : Option[BigDecimal] = None,
		gramPerMole_? : Option[BigDecimal] = None,
		literPerMole_? : Option[BigDecimal] = None,
		celciusAndConcToViscosity: List[CelciusAndConcToViscosity] = Nil
	): Substance = {
		Substance(
			id = id,
			kind = SubstanceKind.Dna,
			tipCleanPolicy = TipCleanPolicy.DD,
			contaminants = Set("DNA"),
			costPerUnit_? = costPerUnit_?,
			valuePerUnit_? = valuePerUnit_?,
			gramPerMole_? = gramPerMole_?,
			literPerMole_? = literPerMole_?,
			celciusAndConcToViscosity = celciusAndConcToViscosity,
			sequence_? = None
		)
	}
	
	def other(
		id: String,
		tipCleanPolicy: TipCleanPolicy = TipCleanPolicy.TT,
		contaminants: Set[String] = Set(),
		costPerUnit_? : Option[BigDecimal] = None,
		valuePerUnit_? : Option[BigDecimal] = None,
		gramPerMole_? : Option[BigDecimal] = None,
		literPerMole_? : Option[BigDecimal] = None,
		celciusAndConcToViscosity: List[CelciusAndConcToViscosity] = Nil
	): Substance = {
		Substance(
			id = id,
			kind = SubstanceKind.Other,
			tipCleanPolicy = tipCleanPolicy,
			contaminants = contaminants,
			costPerUnit_? = costPerUnit_?,
			valuePerUnit_? = valuePerUnit_?,
			gramPerMole_? = gramPerMole_?,
			literPerMole_? = literPerMole_?,
			celciusAndConcToViscosity = celciusAndConcToViscosity,
			sequence_? = None
		)
	}
}
