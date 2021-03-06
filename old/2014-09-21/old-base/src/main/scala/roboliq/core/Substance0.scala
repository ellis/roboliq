package roboliq.core

import scala.reflect.BeanProperty
import scalaz._
import Scalaz._
import roboliq.utils.MathUtils


//case class CelciusToLiterPerMole(celcius: BigDecimal, literPerMole: BigDecimal)
case class CelciusAndConcToViscosity(celcius: BigDecimal, conc: BigDecimal, viscosity: BigDecimal)

/** Represents a substance. */
case class Substance0(
	/** ID in database. */
	val id: String,
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

	val sequence_? : Option[String],
	
	val isEmpty: Boolean,
	val isLiquid: Boolean
) {
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

object Substance0 {
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
	
	val Empty = Substance0(
		id = "<EMPTY>",
		tipCleanPolicy = TipCleanPolicy.NN,
		contaminants = Set(),
		costPerUnit_? = None,
		valuePerUnit_? = None,
		gramPerMole_? = None,
		literPerMole_? = None,
		celciusAndConcToViscosity = Nil,
		sequence_? = None,
		isEmpty = true,
		isLiquid = false
	)
}

/*
/**
 * Represents a DNA-based substance.
 * @param id ID in database.
 * @param sequence_? optional DNA sequence string.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SubstanceDna(
	val id: String,
	val sequence_? : Option[String],
	val costPerUnit_? : Option[BigDecimal],
	val valuePerUnit_? : Option[BigDecimal]
) extends Substance {
	val tipCleanPolicy = TipCleanPolicy.DD
	val contaminants = Set[String]("DNA")
	val isEmpty = false
	val isLiquid = false
}

object SubstanceDna {
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
case class SubstanceOther(
	val id: String,
	val contaminants: Set[String],
	val costPerUnit_? : Option[BigDecimal],
	val valuePerUnit_? : Option[BigDecimal]
) extends Substance {
	val isEmpty = false
	val isLiquid = false
}

object SubstanceOther {
	/** Convert [[roboliq.core.SubstanceOtherBean]] to [[roboliq.core.SubstanceOther]]. */
	def fromBean(bean: SubstanceOtherBean): Result[SubstanceOther] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val costPerUnit_? : Option[BigDecimal] = if (bean.costPerUnit == null) None else Some(bean.costPerUnit)
			new SubstanceOther(id, Set(), costPerUnit_?, None)
		}
	}
}

/**
 * Represents a DNA-based substance.
 * @param id ID in database.
 * @param physicalProperties physical properties of the liquid (e.g. water or glycerol).
 * @param tipCleanPolicy tip cleaning policy when handling this liquid.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SubstanceLiquid(
	val id: String,
	val tipCleanPolicy: TipCleanPolicy,
	val viscosity: BigDecimal,
	val contaminants: Set[String],
	val costPerUnit_? : Option[BigDecimal],
	val valuePerUnit_? : Option[BigDecimal]
) extends Substance {
	val isEmpty = false
	val isLiquid = true
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
*/

sealed class Liquid0 private(
	val id: String,
	val contents: Map[Substance0, BigDecimal]
) {
	val substance_l = contents.keys.toList
	
	/** Tip cleaning policy when handling this substance with pipetter. */
	val tipCleanPolicy: TipCleanPolicy = substance_l.map(_.tipCleanPolicy).concatenate
	/** List of contaminants in this substance */
	val contaminants: Set[String] = substance_l.map(_.contaminants).concatenate
	/** Value per unit (either liter or mol) of the substance (this can be on a different scale than costPerUnit) */
	val valuePerUnit_? : Option[BigDecimal] = substance_l.map(_.valuePerUnit_?).concatenate
	
//	val gramPerMole_? : Option[BigDecimal] = substance_l.map(_.gramPerMole_?).concatenate
//	val celciusToLiterPerMole: List[CelciusToLiterPerMole]
//	val celciusAndConcToLiterPerMole: List[CelciusAndConcToLiterPerMole]
//	val celciusToViscosity: List[CelciusToViscosity]
	
	val isEmpty: Boolean = contents.isEmpty
	val isLiquid: Boolean = substance_l.exists(_.isLiquid)
	// "water"@5.44e-10,"oil"@1.23e-23,
	
	override def equals(that: Any): Boolean = that match {
		case that_# : Liquid0 => id == that_#.id
		case _ => assert(false); false
	}
}

object Liquid0 {
	def apply(contents: Map[Substance0, BigDecimal]): Liquid0 = {
		val l = contents.toList.sortBy(_._2).reverse
		val id = l.map(pair => "\"" + pair._1 + "\"@" + {pair._2}).mkString("(", ",", ")")
		// Make sure fractions are normalized to 1
		val factor = 1 / contents.values.sum
		val contents_# = contents.mapValues(_ * factor)
		new Liquid0(id, contents_#)
	}
	
	val Empty = new Liquid0("<EMPTY>", Map())
}
/*
sealed trait IsLiquid {
	val isLiquid = true
	def solventToFraction: Map[Substance with IsLiquid, BigDecimal]
	def soluteToMolPerLiter: Map[Substance with IsSolid, BigDecimal]
	
	def substances = solventToFraction.keySet ++ soluteToMolPerLiter.keySet
}

sealed trait IsSolid {
	val isLiquid = false
	def solidToMol: Map[Substance with IsSolid, BigDecimal]
	
	def substances = solidToMol.keySet
}

sealed trait IsMixture extends Substance {
	val substance_l = substances
	
	val sName_? : Option[String] = None
	val sFamily: String 
	val tipCleanPolicy: TipCleanPolicy = substance_l.map(_.tipCleanPolicy).concatenate
	val contaminants: Set[String] = substance_l.map(_.contaminants).concatenate
	val costPerUnit_? : Option[BigDecimal] = substance_l.map(_.costPerUnit_?).concatenate
	val valuePerUnit_? : Option[BigDecimal] = substance_l.map(_.valuePerUnit_?).concatenate

	val viscosity = list.foldLeft(BigDecimal(0)){(acc, x) =>
		
	}
	val physicalProperties: LiquidPhysicalProperties.Value = if (liquid_l.exists(_.physicalProperties == LiquidPhysicalProperties.Glycerol)) LiquidPhysicalProperties.Glycerol else LiquidPhysicalProperties.Water
}

/**
 * For temporary backward compatibility with previous roboliq version
 */
trait Liquid0 extends Substance with IsLiquid {
	val substance_l = substances
	
	val sName_? : Option[String] = None
	val sFamily: String 
	val tipCleanPolicy: TipCleanPolicy = substance_l.map(_.tipCleanPolicy).concatenate
	val contaminants: Set[String] = substance_l.map(_.contaminants).concatenate

	val viscosity = list.foldLeft(BigDecimal(0)){(acc, x) =>
		
	}
	val physicalProperties: LiquidPhysicalProperties.Value = if (liquid_l.exists(_.physicalProperties == LiquidPhysicalProperties.Glycerol)) LiquidPhysicalProperties.Glycerol else LiquidPhysicalProperties.Water
	val costPerUnit_? : Option[BigDecimal] = substance_l.map(_.costPerUnit_?).concatenate
	val valuePerUnit_? : Option[BigDecimal] = substance_l.map(_.valuePerUnit_?).concatenate
}

case class LiquidMixture(
	val solventToFraction: Map[Substance with IsLiquid, BigDecimal],
	val soluteToMolPerLiter: Map[Substance with IsSolid, BigDecimal]
) extends Liquid0

case class SolidMixture(
	solidToMol: Map[Substance with IsSolid, BigDecimal]
) extends Substance with IsSolid {
}
*/
/*
sealed trait SubstanceMixture extends Substance {
	val list: List[SubstanceAmount]

	def isEmpty = list.isEmpty
	def isLiquid = list.exists(_.substance.isInstanceOf[SubstanceLiquid])
	def isSolid = !list.isEmpty && !isLiquid
	def id = list.mkString("+")
	override def toString = id
}
*/

/*
object RqUnit extends Enumeration {
	val None, l, mol, g = Value
}

case class Amount(n: BigDecimal, unit: RqUnit.Value) {
	override def toString = {
		val s = if (unit == RqUnit.None) "" else unit.toString
		s"$n$s"
	}
}
*/

/*
(A)@a+(B)@b => ((A)@(a/(a+b))+(B)@(b/(a+b)))@(a+b) 
((A)@a+(B)@b)@c => (A)@(a/(a+b)*c)+(B)@(b/(a+b)*c) 
solids: (A)@a+(B)@b => ((A)@(a/0)+(B)@(b/0))@0
((A)@(a/0)+(B)@(b/0))@0+(C)@c => ((A)@(a/c)+(B)@(b/c)+(C)@(c/c))@c
*/


/*
case class Mixture0(
	val solventToFraction: Map[Substance with IsLiquid, BigDecimal],
	val soluteToMol: Map[Substance with IsSolid, BigDecimal]
) {
	/** Doc strings for this object. */
	val docContent = createDocContent()
	
	private def createLiquid(): Liquid = {
		// Volume of solvents
		if (volume.isEmpty)
			Liquid.empty
		else {
			val nSolvents = solventToVolume.size
			val nSolutes = soluteToMol.size
			
			// Construct liquid id
			val id: String = {
				if (nSolutes == 1)
					soluteToMol.head._1.id
				else if (nSolutes == 0 && nSolvents == 1)
					solventToVolume.head._1.id
				else
					(soluteToMol.keys.map(_.id).toList.sorted ++
						solventToVolume.keys.map(_.id).toList.sorted
					).mkString("+")
			}
			
			// Determine physical properties (either water-like or glycerol-like)
			val physicalProperties: LiquidPhysicalProperties.Value = {
				val volumeGlycerol = solventToVolume.foldLeft(LiquidVolume.empty) {(acc, pair) =>
					val (solution, volume) = pair
					solution.physicalProperties match {
						case LiquidPhysicalProperties.Glycerol => acc + volume
						case _ => acc
					}
				}
				// If glycerol volume is 5% or more, select glycerol
				val fractionGlycerol = volumeGlycerol.l / volume.l
				if (fractionGlycerol >= 0.05)
					LiquidPhysicalProperties.Glycerol
				else
					LiquidPhysicalProperties.Water
			}
			
			// Water only => TNL, DNA and Cells => Decon, other => TNT
			val cleanPolicy = {
				val (nBio, nOtherº1) = soluteToMol.keys.foldLeft((0, 0))((acc, substance) => {
					substance match {
						case dna: SubstanceDna => (acc._1 + 1, acc._2)
						//case cell: SubstanceCell => (acc._1 + 1, acc._2)
						case _ => (acc._1, acc._2 + 1)
					}
				})
				val nOtherº2 = solventToVolume.keys.foldLeft(nOtherº1)((acc, substance) => {
					if (substance.id == "water") acc
					else acc + 1
				})
				if (nBio > 0)
					TipCleanPolicy.DD
				else if (nOtherº2 > 0)
					TipCleanPolicy.TT
				else
					TipCleanPolicy.TL
			}
			
			// Allow multipipetting if there are substances which don't prohibit it.
			// NOTE: this is very, very arbitrary -- ellis, 2012-04-10
			// TODO: try to figure out a better method!
			val bCanMultipipette = {
				val nAllowMultipipette1 = solventToVolume.keys.filter(_.expensive).size
				val nAllowMultipipette2 = soluteToMol.keys.filter(_.expensive).size
				//nAllowMultipipette < nSolvents
				// If there are solutes which shouldn't be multipipetted
				if (nAllowMultipipette2 < nSolutes) {
					// If any DO allow multipipetting, go ahead and do so
					nAllowMultipipette2 > 0
				}
				else if (nAllowMultipipette1 < nSolvents) {
					nAllowMultipipette1 > 0
				}
				else
					true
			}
			
			new Liquid(
				id = id,
				None,
				sFamily = physicalProperties.toString,
				contaminants = Set(),
				cleanPolicy,
				multipipetteThreshold = if (bCanMultipipette) 0 else 1
			)
		}
	}
	
	private def createDocContent(): Doc = {
		// List of solvents in order of decreasing volume
		val lSolvent: List[SubstanceLiquid] = solventToVolume.toList.sortBy(-_._2.nl).map(_._1)
		// List of solutes in order of decreasing mol
		val lSolute: List[SubstanceSolid] = soluteToMol.toList.sortBy(-_._2).map(_._1)
		
		// Empty
		if (volume.isEmpty)
			new Doc(None, None, None)
		// Only solutes
		else {
			val sVol = {
				if (solventToVolume.isEmpty) ""
				else volume.toString+" "
			}
			
			val lsSolvent = lSolvent.map(sub => sub.id+"("+solventToVolume(sub)+")")
			val (lsSolutePlain, lsSoluteMd) = (lSolute map { sub =>
				val fmt = new DecimalFormat("0.00E0")
				val nMol = soluteToMol(sub)
				val nM = nMol / volume.l
				val sMPlain = fmt.format(nM.toDouble)//.replace("E", "E^")+"^"
				val sMMd = sMPlain.replace("E", "E^")+"^"
				(sub.id+"("++sMPlain++" M)", sub.id.replace("_", "\\_")+"("++sMMd++" M)") 
			}).unzip
			val lsSubstancePlain = (lsSolvent ++ lsSolutePlain)
			val lsSubstanceMd = (lsSolvent ++ lsSoluteMd)
			
			val idLiquid = lsSubstancePlain mkString "+"
			val sLiquidName_? =
				if (solventToVolume.size + soluteToMol.size == 1) Some(idLiquid)
				else None
			
			val sContentPlainShort_? = sLiquidName_?.map(sVol + _)
			val sContentMdLong_? = {
				Some((sVol :: lsSubstanceMd) mkString "  \n")
			}
			
			new Doc(sContentPlainShort_?, sContentPlainShort_?, sContentMdLong_?)
		}
	}
	
	private implicit val v1: Semigroup[LiquidVolume] = new Semigroup[LiquidVolume] {
		def append(s1: LiquidVolume, s2: => LiquidVolume) = s1 + s2
	}
	private implicit val v2: Semigroup[BigDecimal] = new Semigroup[BigDecimal] {
		def append(s1: BigDecimal, s2: => BigDecimal) = s1 + s2
	}
	
	/**
	 * Return a new VesselContent from this one which has been scaled to a total volume of `volumeNew`.
	 */
	def getAmount(volume: LiquidVolume): RqResult[LiquidAmount] = {
		if (solventToFraction.isEmpty)
			RqError("cannot extract volume from a non-liquid solvent")
		else
		if (volume.isEmpty)
			RqSuccess(LiquidVolume())
			return this
		val factor = volumeNew.l / volume.l
		LiquidAmount(
			//idVessel,
			solventToVolume.mapValues(_ * factor),
			soluteToMol.mapValues(_ * factor)
		)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `that`.
	 */
	def +(that: VesselContent): VesselContent = {
		new VesselContent(
			//idVessel,
			solventToVolume |+| that.solventToVolume,
			soluteToMol |+| that.soluteToMol
		)
	}

	/**
	 * Return a new VesselContent combining `this` and `volume` of `that`.
	 */
	def addContentByVolume(that: VesselContent, volume: LiquidVolume): VesselContent = {
		this + that.scaleToVolume(volume)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `mol` of a non-liquid `substance`.
	 */
	def addPowder(substance: SubstanceSolid, mol: BigDecimal): VesselContent = {
		new VesselContent(
//			idVessel,
			solventToVolume,
			soluteToMol |+| Map(substance -> mol)
		)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `volume` of a liquid `substance`.
	 */
	def addLiquid(substance: SubstanceLiquid, volume: LiquidVolume): VesselContent = {
		new VesselContent(
//			idVessel,
			solventToVolume |+| Map(substance -> volume),
			soluteToMol
		)
	}

	/**
	 * Return a new VesselContent after removing `volume` of `this`.
	 */
	def removeVolume(volume: LiquidVolume): VesselContent = {
		scaleToVolume(this.volume - volume)
	}

	/**
	 * Get the molar concentration of a non-liquid `substance`. 
	 */
	def concOfSolid(substance: SubstanceSolid): Result[BigDecimal] = {
		if (volume.isEmpty)
			return Success(0)
		soluteToMol.get(substance) match {
			case None => Error("vessel does not contain substance `"+substance.id+"`")
			case Some(mol) => Success(mol / volume.l)
		}
	}

	/**
	 * Get the proportion of a liquid in this vessel. 
	 */
	def concOfLiquid(substance: SubstanceLiquid): Result[BigDecimal] = {
		if (volume.isEmpty)
			return Success(0)
		solventToVolume.get(substance) match {
			case None => Error("vessel does not contain liquid `"+substance.id+"`")
			case Some(vol) => Success(vol.l / volume.l)
		}
	}

	/**
	 * Get the proportion of a liquid in this vessel. 
	 */
	def concOfSubstance(substance: Substance): Result[BigDecimal] = {
		substance match {
			case liquid: SubstanceLiquid => concOfLiquid(liquid)
			case solid: SubstanceSolid => concOfSolid(solid)
		}
	}
}
*/


/*
sealed trait SubstanceAmount

object SubstanceAmount {
	def add(a: SubstanceAmount, b: SubstanceAmount): SubstanceAmount = {
		(a, b) match {
			case (c: LiquidAmount, d: LiquidAmount) =>
				val volume = c.volume + d.volume
				val fractionC = c.volume / volume
				val fractionD = d.volume / volume
				val solventToFraction_m =
					c.liquid.solventToFraction.mapValues(_ * fractionC) |+|
					d.liquid.solventToFraction.mapValues(_ * fractionD)
				val soluteToFraction_m =
					c.liquid.soluteToMolPerLiter.mapValues(_ * fractionC) |+|
					d.liquid.soluteToMolPerLiter.mapValues(_ * fractionD)
				val liquid = LiquidMixture(
					solventToFraction_m,
					soluteToFraction_m
				)
				LiquidAmount(liquid, volume)
			case (c: SolidAmount, d: SolidAmount) =>
				val solidToMol = c.solid.solidToMol |+| d.solid.solidToMol
				val solid = SolidMixture(solidToMol)
				SolidAmount(solid, c.mol + d.mol)
			case (c: LiquidAmount, d: SolidAmount) =>
				addLiquidAndSolid(c, d)
			case (c: SolidAmount, d: LiquidAmount) =>
				addLiquidAndSolid(d, c)
		}
	}
	
	private def addLiquidAndSolid(a: LiquidAmount, b: SolidAmount): LiquidAmount = {
		val soluteToFraction_m =
			a.liquid.soluteToMolPerLiter |+|
			b.solid.solidToMol.mapValues(_ / a.volume.l)
		val liquid = LiquidMixture(
			a.liquid.solventToFraction,
			soluteToFraction_m
		)
		LiquidAmount(liquid, a.volume)
	}
}

case class LiquidAmount(liquid: Substance with IsLiquid, volume: LiquidVolume) extends SubstanceAmount {
	override def toString = s"(${liquid.id})@${volume}"
}

case class SolidAmount(solid: Substance with IsSolid, mol: BigDecimal) extends SubstanceAmount {
	override def toString = s"(${solid.id})@${mol}mol"
}
*/