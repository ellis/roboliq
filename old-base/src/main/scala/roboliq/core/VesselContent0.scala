package roboliq.core

import scalaz._
import Scalaz._
import java.text.DecimalFormat
import grizzled.slf4j.Logger
import RqPimper._


/**
 * Represents the contents of a vessel, i.e. the amounts of the substances it contains.
 * 
 * @param idVessel ID in database.
 * @param solventToVolume map from liquid to volume.
 * @param soluteToMol map from solute to amount in mol.
 */
case class VesselContent0(
	// REFACTOR: consider merging Liquid and VesselContent, where Liquid is a VesselContent normalized to 1 
	val liquid: Liquid0,
	val totalMole: BigDecimal
) {
	private val logger = Logger[this.type]
	
	val substanceToMol: Map[Substance0, BigDecimal] = {
		val partsTotal = liquid.contents.values.sum
		val factor = totalMole / partsTotal
		liquid.contents.mapValues(_ * totalMole)
	}
	val substanceToVolume: Map[Substance0, LiquidVolume] =
		substanceToMol.toList.flatMap(pair => {
			val (substance, mole) = pair
			substance.literPerMole_?.map(literPerMole => substance -> LiquidVolume.l(literPerMole * mole))
		}).toMap
	/** Total liquid volume in vessel at 25C. */
	val volume = substanceToVolume.values.toList.concatenate

	/*
	/** Doc strings for this object. */
	val docContent = createDocContent()
	
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
	*/
	
	private implicit val v2: Semigroup[BigDecimal] = new Semigroup[BigDecimal] {
		def append(s1: BigDecimal, s2: => BigDecimal) = s1 + s2
	}
	
	/**
	 * Return a new VesselContent from this one which has been scaled to a total volume of `volumeNew`.
	 */
	def scaleToVolume(volumeNew: LiquidVolume): VesselContent0 = {
		if (volume.isEmpty) {
			logger.warn(s"called VesselContent0.scaleToVolume() on empty vessel: $this")
			return this
		}
		val totalMole_# = totalMole * volumeNew.l / volume.l
		VesselContent0(liquid, totalMole_#)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `that`.
	 */
	def +(that: VesselContent0): VesselContent0 = {
		val contents_# = substanceToMol |+| that.substanceToMol
		val liquid_# = Liquid0(contents_#)
		new VesselContent0(
			liquid_#,
			totalMole + that.totalMole
		)
	}

	/**
	 * Return a new VesselContent combining `this` and `volume` of `that`.
	 */
	def addContentByVolume(that: VesselContent0, volume: LiquidVolume): VesselContent0 = {
		this + that.scaleToVolume(volume)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `mol` of a non-liquid `substance`.
	 */
	def addSubstance(substance: Substance0, mol: BigDecimal): VesselContent0 = {
		val content_# = substanceToMol |+| Map(substance -> mol)
		val liquid_# = Liquid0(content_#)
		new VesselContent0(
			liquid_#,
			totalMole + mol
		)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `volume` of a liquid `substance`.
	 */
	def addLiquid(substance: Substance0, volume: LiquidVolume): RqResult[VesselContent0] = {
		for {
			literPerMole <- substance.literPerMole_?.asRq("substance must specify literPerMole in order to work with volumes")
		} yield {
			val mole = volume.l / literPerMole
			addSubstance(substance, mole)
		}
	}

	/**
	 * Return a new VesselContent after removing `volume` of `this`.
	 */
	def removeVolume(volume: LiquidVolume): VesselContent0 = {
		scaleToVolume(this.volume - volume)
	}

	/**
	 * Get the molar concentration of a non-liquid `substance`. 
	 */
	def substanceFraction(substance: Substance0): BigDecimal = {
		substanceToMol.get(substance).map(_ / totalMole).getOrElse(0)
	}
}

object VesselContent0 {
	/** Empty contents for this given vessel. */
	def createEmpty = {
		new VesselContent0(Liquid0.Empty, 0)
	}
}
