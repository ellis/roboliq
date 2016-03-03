package roboliq.pipette

import scalaz._
import Scalaz._
import java.text.DecimalFormat
import grizzled.slf4j.Logger
import roboliq.core._
import roboliq.utils.MathUtils


/**
 * Represents the contents of a vessel, i.e. the amounts of the substances it contains.
 * 
 * @param idVessel ID in database.
 * @param solventToVolume map from liquid to volume.
 * @param soluteToMol map from solute to amount in mol.
 */
class VesselContent private(
	val contents: Map[Substance, BigDecimal],
	val liquid: Liquid
) {
	private val logger = Logger[this.type]
	
	val totalMole = contents.values.sum
	
	val substanceToMol: Map[Substance, BigDecimal] = contents
	val substanceToVolume: Map[Substance, LiquidVolume] =
		substanceToMol.toList.flatMap(pair => {
			val (substance, mole) = pair
			substance.molarity_?.map(molarity => substance -> LiquidVolume.l(mole / molarity))
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
	def scaleToVolume(volumeNew: LiquidVolume): RsResult[VesselContent] = {
		// Make sure that this vessel contains something that can be scaled
		if (liquid.isEmpty) {
			RsError(s"called VesselContent.scaleToVolume() on an empty vessel: $this")
		}
		// If there are no contents whose molarity is defined:
		else if (substanceToVolume.isEmpty) {
			RsError(s"called VesselContent.scaleToVolume() on a vessel whose contents do not have molarity specified: $this")
		}
		// If the vessel doesn't have a volume specified, take the liquid and try to scale it appropriately
		else if (volume.isEmpty) {
			val content_# = VesselContent(liquid, 1)
			if (content_#.volume.isEmpty)
				RsError(s"called VesselContent.scaleToVolume() on a vessel whose contents do not have positive molarity specified: $this")
			else
				content_#.scaleToVolume(volumeNew)
		}
		else {
			val factor = volumeNew.l / volume.l
			val contents_# = VesselContent.scaleBy(contents, factor)
			RsSuccess(VesselContent(contents_#))
		}
	}
	
	/**
	 * Return a new VesselContent combining `this` and `that`.
	 */
	def +(that: VesselContent): VesselContent = {
		val contents_# = substanceToMol |+| that.substanceToMol
		VesselContent(contents_#)
	}

	/**
	 * Return a new VesselContent combining `this` and `volume` of `that`.
	 */
	def addContentByVolume(that: VesselContent, volume: LiquidVolume): RsResult[VesselContent] = {
		that.scaleToVolume(volume).map(this + _)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `mol` of a non-liquid `substance`.
	 */
	def addSubstance(substance: Substance, mol: BigDecimal): VesselContent = {
		val content_# = substanceToMol |+| Map(substance -> mol)
		VesselContent(content_#)
	}
	
	/**
	 * Return a new VesselContent combining `this` and `volume` of a liquid `substance`.
	 */
	def addLiquid(substance: Substance, volume: LiquidVolume): RsResult[VesselContent] = {
		for {
			molarity <- substance.molarity_?.asRs("substance must specify literPerMole in order to work with volumes")
		} yield {
			val mole = molarity * volume.l
			addSubstance(substance, mole)
		}
	}

	/**
	 * Return a new VesselContent after removing `volume` of `this`.
	 */
	def removeVolume(volume: LiquidVolume): RsResult[VesselContent] = {
		scaleToVolume(this.volume - volume)
	}

	/**
	 * Get the mole fraction of `substance` (i.e., moles of substance in vessel divided by total moles in vessel). 
	 */
	def moleFractionOf(substance: Substance): BigDecimal = {
		if (totalMole > 0)
			substanceToMol.get(substance).map(_ / totalMole).getOrElse(0)
		else
			0
	}

	/**
	 * Get the molar concentration of `substance` (i.e., moles of substance in vessel divided by total volume in vessel). 
	 */
//	def molarConcOf(substance: Substance): BigDecimal = {
//		if (totalMole > 0)
//			substanceToMol.get(substance).map(_ / totalMole).getOrElse(0)
//		else
//			0
//	}

	override def toString = 
		if (contents.isEmpty) "<EMPTY>"
		else contents.map(pair => "\"" + pair._1.id + "\"@" + MathUtils.toChemistString3(pair._2) + "mol").mkString("(", "+", ")")
		
	override def equals(that: Any): Boolean = that match {
		case that_# : VesselContent => contents == that_#.contents
		case _ => assert(false); false
	}
	override def hashCode() = id.hashCode()
}

object VesselContent {
	def apply(contents: Map[Substance, BigDecimal]): VesselContent = {
		new VesselContent(contents, Liquid(contents))
	}
	
	def apply(liquid: Liquid, totalMole: BigDecimal): VesselContent = {
		new VesselContent(scaleBy(liquid.contents, totalMole), liquid)
	}
	
	/** Empty vessel contents. */
	val Empty = new VesselContent(Map(), Liquid.Empty)
	
	def fromVolume(substance: Substance, volume: LiquidVolume): RsResult[VesselContent] =
		Empty.addLiquid(substance, volume)
		
	def fromVolume(liquid: Liquid, volume: LiquidVolume): RsResult[VesselContent] = {
		val content_# = VesselContent(liquid, 1)
		content_#.scaleToVolume(volume)
	}
		
	def scaleBy(contents: Map[Substance, BigDecimal], factor: BigDecimal): Map[Substance, BigDecimal] =
		contents.mapValues(_ * factor)
	
	implicit object VesselContentMonoid extends Monoid[VesselContent] {
		def zero = Empty
		def append(a: VesselContent, b: => VesselContent): VesselContent = a + b
	}
}
