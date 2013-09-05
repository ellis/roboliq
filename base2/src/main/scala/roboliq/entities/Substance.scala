package roboliq.entities

import scalaz._
import Scalaz._


//case class CelciusToLiterPerMole(celcius: BigDecimal, literPerMole: BigDecimal)
case class CelciusAndConcToViscosity(celcius: BigDecimal, conc: BigDecimal, viscosity: BigDecimal)

object SubstanceKind extends Enumeration {
	val None, Liquid, Dna, Other = Value
}

/**
 * Represents a substance.
 * 
 * @param molarity_? moles per liter
 */
case class Substance(
	/** Key in database */
	val key: String,
	/** A more human-friendly name */
	val label: Option[String],
	/** Description for the user */
	val description: Option[String],
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
	
	val molarity_? : Option[BigDecimal],
	val gramPerMole_? : Option[BigDecimal],
	val celciusAndConcToViscosity: List[CelciusAndConcToViscosity],

	val sequence_? : Option[String]
) {
	val isEmpty: Boolean = (kind == SubstanceKind.None)
	val isLiquid: Boolean = (kind == SubstanceKind.Liquid) 
	def molarity: BigDecimal = molarity_?.getOrElse(0)
}

object Substance {
	val Empty = Substance(
		key = "<EMPTY>",
		label = None,
		description = None,
		kind = SubstanceKind.None,
		tipCleanPolicy = TipCleanPolicy.NN,
		contaminants = Set(),
		costPerUnit_? = None,
		valuePerUnit_? = None,
		molarity_? = None,
		gramPerMole_? = None,
		celciusAndConcToViscosity = Nil,
		sequence_? = None
	)
	
	def liquid(
		id: String,
		molarity: BigDecimal,
		tipCleanPolicy: TipCleanPolicy = TipCleanPolicy.TT,
		contaminants: Set[String] = Set(),
		costPerUnit_? : Option[BigDecimal] = None,
		valuePerUnit_? : Option[BigDecimal] = None,
		gramPerMole_? : Option[BigDecimal] = None,
		celciusAndConcToViscosity: List[CelciusAndConcToViscosity] = Nil
	): Substance = {
		Substance(
			key = id,
			label = None,
			description = None,
			kind = SubstanceKind.Liquid,
			tipCleanPolicy = tipCleanPolicy,
			contaminants = contaminants,
			costPerUnit_? = costPerUnit_?,
			valuePerUnit_? = valuePerUnit_?,
			molarity_? = Some(molarity),
			gramPerMole_? = gramPerMole_?,
			celciusAndConcToViscosity = celciusAndConcToViscosity,
			sequence_? = None
		)
	}
	
	def dna(
		id: String,
		sequence_? : Option[String] = None,
		costPerUnit_? : Option[BigDecimal] = None,
		valuePerUnit_? : Option[BigDecimal] = None,
		molarity_? : Option[BigDecimal] = None,
		gramPerMole_? : Option[BigDecimal] = None,
		celciusAndConcToViscosity: List[CelciusAndConcToViscosity] = Nil
	): Substance = {
		Substance(
			key = id,
			label = None,
			description = None,
			kind = SubstanceKind.Dna,
			tipCleanPolicy = TipCleanPolicy.DD,
			contaminants = Set("DNA"),
			costPerUnit_? = costPerUnit_?,
			valuePerUnit_? = valuePerUnit_?,
			molarity_? = molarity_?,
			gramPerMole_? = gramPerMole_?,
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
		molarity_? : Option[BigDecimal] = None,
		gramPerMole_? : Option[BigDecimal] = None,
		celciusAndConcToViscosity: List[CelciusAndConcToViscosity] = Nil
	): Substance = {
		Substance(
			key = id,
			label = None,
			description = None,
			kind = SubstanceKind.Other,
			tipCleanPolicy = tipCleanPolicy,
			contaminants = contaminants,
			costPerUnit_? = costPerUnit_?,
			valuePerUnit_? = valuePerUnit_?,
			molarity_? = molarity_?,
			gramPerMole_? = gramPerMole_?,
			celciusAndConcToViscosity = celciusAndConcToViscosity,
			sequence_? = None
		)
	}
}
