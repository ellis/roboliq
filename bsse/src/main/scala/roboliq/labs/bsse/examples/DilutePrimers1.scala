/*package roboliq.labs.bsse.examples

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.commands.pipette.L4A_PipetteArgs
import roboliq.commands.pipette.L4A_PipetteItem
import roboliq.commands.pipette.L4C_Pipette
import roboliq.common._
//import roboliq.protocol._
//import roboliq.protocol.commands._

class DilutePrimers1 {
	// The nano mol amount for SEQUENCE_01 to SEQUENCE_49
	val lAmount_nmol = List[BigDecimal](
		54.62,
		72.20,
		45.39,
		49.18,
		54.95,
		58.00,
		54.26,
		47.16,
		40.75,
		37.08,
		47.84,
		44.33,
		48.59,
		46.24,
		48.24,
		44.01,
		49.73,
		46.95,
		51.11,
		47.76,
		115.04,
		59.09,
		47.12,
		59.10,
		41.83,
		46.81,
		54.74,
		48.50,
		54.30,
		44.18,
		59.68,
		50.06,
		45.35,
		53.01,
		48.50,
		50.42,
		52.40,
		43.38,
		48.51,
		41.33,
		38.14,
		37.41,
		46.62,
		45.13,
		47.36,
		53.71,
		46.53,
		48.48,
		56.27
	)
	
	val lAmount_nmol = List[BigDecimal]( 54.62, 72.20, 45.39, 49.18, 54.95, 58.00, 54.26, 47.16, 40.75, 37.08, 47.84, 44.33, 48.59, 46.24, 48.24, 44.01, 49.73, 46.95, 51.11, 47.76, 115.04, 59.09, 47.12, 59.10, 41.83, 46.81, 54.74, 48.50, 54.30, 44.18, 59.68, 50.06, 45.35, 53.01, 48.50, 50.42, 52.40, 43.38, 48.51, 41.33, 38.14, 37.41, 46.62, 45.13, 47.36, 53.71, 46.53, 48.48, 56.27)
	
	// Dilute to 400 mM
	val lVol_ul = lAmount_nmol.map(_ / 0.4)

	/*
	import LiquidAmountImplicits._

	val l = List(
		new Pipette {
			src := refDb("E2215")
			dest := refDb(")
		},
		new Pcr {
			products := List(
				// NOT1
				new Product { template := refDb("FRP446"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO700") },
				new Product { template := refDb("FRP332"); forwardPrimer := refDb("FRO699"); backwardPrimer := refDb("FRO114") },
				// NOT2
				new Product { template := refDb("FRP337"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO704") },
				new Product { template := refDb("FRP222"); forwardPrimer := refDb("FRO703"); backwardPrimer := refDb("FRO114") },
				// NOR3_yellow
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO1260") },
				new Product { template := refDb("FRP128"); forwardPrimer := refDb("FRO1259"); backwardPrimer := refDb("FRO1262") },
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO1261"); backwardPrimer := refDb("FRO114") }
			)
			volumes := (30 ul)
			mixSpec := new PcrMixSpec {
				waterLiquid := refDb("water")

				buffer.liquid := refDb("buffer10x")
				buffer.amt0 := (10 x)
				buffer.amt1 := (1 x)
				
				dntp.liquid := refDb("dntp")
				dntp.amt0 := (2 mM)
				dntp.amt1 := (0.2 mM)
				
				template.amt0 := (1 x) // FIXME: dummy value
				template.amt1 := (3 ul)
				
				forwardPrimer.amt0 := (10 uM)
				forwardPrimer.amt1 := (0.5 uM)
				
				backwardPrimer.amt0 := (10 uM)
				backwardPrimer.amt1 := (0.5 uM)
				
				polymerase.liquid := refDb("taq-diluted")
				polymerase.amt0 := (10 x)
				polymerase.amt1 := (1 x)
			}
		}
	)
	*/
	
	import roboliq.common.WellPointerImplicits._
	
	val water = new Liquid(
		sName = "water",
		sFamily = "Water",
		contaminants = Nil,
		group = new LiquidGroup(GroupCleanPolicy.TNL),
		multipipetteThreshold = 0)
	
	val plate = new Plate
	
	val lPipetteItem = lVol_ul.zipWithIndex.map(pair => {
		val (nVol, i) = pair
		val iRow = i / 12
		val iCol = i % 12
		val coord = WellCoord(iRow, iCol)
		new L4A_PipetteItem(water, plate(coord), List(nVol.doubleValue()), None, None)
	})
	val lPipetteCmd = L4C_Pipette(new L4A_PipetteArgs(lPipetteItem)) :: Nil
	lPipetteCmd
}
*/