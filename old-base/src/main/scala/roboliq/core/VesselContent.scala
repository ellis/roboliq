package roboliq.core

import scalaz._
import Scalaz._
import java.text.DecimalFormat


/**
 * Represents the contents of a vessel, i.e. the amounts of the substances it contains.
 * 
 * @param idVessel ID in database.
 * @param solventToVolume map from liquid to volume.
 * @param soluteToMol map from solute to amount in mol.
 */
case class VesselContent(
	// REFACTOR: Change VesselContent to a recursive structure
	//  such as ((water)@100ul+(beer)@100ml)@100ml+(rum)@20ml
	//val idVessel: String,
	val solventToVolume: Map[SubstanceLiquid, LiquidVolume],
	val soluteToMol: Map[SubstanceSolid, BigDecimal]
) {
	/** Total liquid volume in vessel. */
	val volume = solventToVolume.values.foldLeft(LiquidVolume.empty){(acc,v) => acc + v}
	/** A [[roboliq.core.Liquid]] representation of these contents. */
	val liquid = createLiquid()
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
	def scaleToVolume(volumeNew: LiquidVolume): VesselContent = {
		if (volume.isEmpty)
			return this
		val factor = volumeNew.l / volume.l
		new VesselContent(
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

object VesselContent {
	/** Empty contents for this given vessel. */
	def createEmpty = {
		new VesselContent(Map(), Map())
	}
}
