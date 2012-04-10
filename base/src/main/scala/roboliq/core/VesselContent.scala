package roboliq.core

import scalaz._
import Scalaz._

//import scala.collection

class VesselContent(
	val idVessel: String,
	val mapSolventToVolume: Map[SubstanceLiquid, LiquidVolume],
	val mapSoluteToMol: Map[Substance, BigDecimal]
) {
	private def createLiquid(): Liquid = {
		// Volume of solvents
		val volume = mapSolventToVolume.values.reduce(_ + _)
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
	
	def +(that: VesselContent): VesselContent = {
		
		val mapSolventToVolume: Map[SubstanceLiquid, LiquidVolume],
		val mapSoluteToMol: Map[Substance, BigDecimal]
		
	}
}

object VesselContent {
	def add(a: VesselContent, b: VesselContent): VesselContent = {
		for ((solvent, volume) <- b.mapSolventToVolume) {
			a.mapSolventToVolume
		}
	}
}