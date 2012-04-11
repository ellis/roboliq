package roboliq.core

import scalaz._
import Scalaz._

//import scala.collection

class VesselContent(
	val idVessel: String,
	val mapSolventToVolume: Map[SubstanceLiquid, LiquidVolume],
	val mapSoluteToMol: Map[Substance, BigDecimal]
) {
	val volume = mapSolventToVolume.values.foldLeft(LiquidVolume.empty){(acc,v) => acc + v}
	val liquid = createLiquid()
	
	private def createLiquid(): Liquid = {
		// Volume of solvents
		if (volume.isEmpty)
			Liquid.empty
		else {
			val nSolvents = mapSolventToVolume.size
			val nSolutes = mapSoluteToMol.size
			
			// Construct liquid name
			val sName: String = {
				if (nSolutes == 1)
					mapSoluteToMol.head._1.id
				else if (nSolutes == 0 && nSolvents == 1)
					mapSolventToVolume.head._1.id
				else
					(mapSoluteToMol.keys.map(_.id).toList.sorted ++
						mapSolventToVolume.keys.map(_.id).toList.sorted
					).mkString("+")
			}
			
			// Determine physical properties (either water-like or glycerol-like)
			val physicalProperties: LiquidPhysicalProperties.Value = {
				val volumeGlycerol = mapSolventToVolume.foldLeft(LiquidVolume.empty) {(acc, pair) =>
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
				val (nBio, nOtherº1) = mapSoluteToMol.keys.foldLeft((0, 0))((acc, substance) => {
					substance match {
						case dna: SubstanceDna => (acc._1 + 1, acc._2)
						//case cell: SubstanceCell => (acc._1 + 1, acc._2)
						case _ => (acc._1, acc._2 + 1)
					}
				})
				val nOtherº2 = mapSolventToVolume.keys.foldLeft(nOtherº1)((acc, substance) => {
					if (substance.id == "water") acc
					else acc + 1
				})
				if (nBio > 0)
					GroupCleanPolicy.DDD
				else if (nOtherº2 > 0)
					GroupCleanPolicy.TNT
				else
					GroupCleanPolicy.TNL
			}
			
			// Allow multipipetting if there are substances which don't prohibit it.
			// NOTE: this is very, very arbitrary -- ellis, 2012-04-10
			// TODO: try to figure out a better method!
			val bCanMultipipette = {
				val nAllowMultipipette = mapSolventToVolume.keys.filter(_.allowMultipipette).size
				nAllowMultipipette < nSolvents
			}
			
			new Liquid(
				sName = sName,
				sFamily = physicalProperties.toString,
				contaminants = Set(),
				group = new LiquidGroup(cleanPolicy),
				multipipetteThreshold = if (bCanMultipipette) 1 else 0
			)
		}
	}
	
	private implicit val v1: Semigroup[LiquidVolume] = new Semigroup[LiquidVolume] {
		def append(s1: LiquidVolume, s2: => LiquidVolume) = s1 + s2
	}
	private implicit val v2: Semigroup[BigDecimal] = new Semigroup[BigDecimal] {
		def append(s1: BigDecimal, s2: => BigDecimal) = s1 + s2
	}
	
	def scaleToVolume(volumeNew: LiquidVolume): VesselContent = {
		if (volume.isEmpty)
			return this
		val factor = volumeNew.l / volume.l
		new VesselContent(
			idVessel,
			mapSolventToVolume.mapValues(_ * factor),
			mapSoluteToMol.mapValues(_ * factor)
		)
	}
	
	def +(that: VesselContent): VesselContent = {
		new VesselContent(
			idVessel,
			mapSolventToVolume |+| that.mapSolventToVolume,
			mapSoluteToMol |+| that.mapSoluteToMol
		)
	}
	
	//def append(a: VesselContent, b: VesselContent) = a + b
	
	def addContentByVolume(that: VesselContent, volume: LiquidVolume): VesselContent = {
		this + that.scaleToVolume(volume)
	}
	
	def addPowder(substance: Substance, mol: BigDecimal): VesselContent = {
		new VesselContent(
			idVessel,
			mapSolventToVolume,
			mapSoluteToMol |+| Map(substance -> mol)
		)
	}
	
	def addLiquid(substance: SubstanceLiquid, volume: LiquidVolume): VesselContent = {
		new VesselContent(
			idVessel,
			mapSolventToVolume |+| Map(substance -> volume),
			mapSoluteToMol
		)
	}

	def removeVolume(volume: LiquidVolume): VesselContent = {
		scaleToVolume(this.volume - volume)
	}
	
	def concOfSubstance(substance: Substance): Result[BigDecimal] = {
		if (volume.isEmpty)
			return Success(0)
		mapSoluteToMol.get(substance) match {
			case None => Error("vessel `"+idVessel+"` does not contain substance `"+substance.id+"`")
			case Some(mol) => Success(mol / volume.l)
		}
	}
	/*
	def concOfSubstance(id: String): Result[BigDecimal] = {
		mapSoluteToMol.find(pair => pair._1.id == id) match {
			case None => Error("vessel does not contain `"+id+"`")
			case Some(pair) => Success(pair._2 / volume.l)
		}
	}
	*/
}

object VesselContent {
	def createEmpty(idVessel: String) = {
		new VesselContent(idVessel, Map(), Map())
	}
}
