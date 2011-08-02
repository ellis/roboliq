import scala.collection.mutable.ArrayBuffer

import roboliq.builder.parts._
import roboliq.devices._

class Volume(n: Double) {
	def ul = n
}

trait Roboliq {
	val kb = new KnowledgeBase
	val cmds = new ArrayBuffer[Command]
	var m_protocol: Option[() => Unit] = None
	var m_customize: Option[() => Unit] = None
	
	def protocol(fn: => Unit) {
		m_protocol = Some(fn _)
	}
	
	def customize(fn: => Unit) {
		m_customize = Some(fn _)
	}
	
	def setLiquidClasses(pairs: Tuple2[Liquid, String]*) {
		pairs.foreach(pair => pair._1)
	}
	
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double) {
		val item = new PipetteItem(source, dest, volume)
		val cmd = PipetteCommand(Seq(item))
		cmds += cmd
	}
	
	def mix(dest: WellOrPlate, volume: Double, count: Int) {
		
	}
	
	implicit def wellToWPL(o: Well): WellOrPlateOrLiquid = WPL_Well(o)
	implicit def plateToWPL(o: Plate): WellOrPlateOrLiquid = WPL_Plate(o)
	implicit def liquidToWPL(o: Liquid): WellOrPlateOrLiquid = WPL_Liquid(o)
	
	implicit def wellToWP(o: Well): WellOrPlate = WP_Well(o)
	implicit def plateToWP(o: Plate): WellOrPlate = WP_Plate(o)
	
	implicit def intToVolume(n: Int): Volume = new Volume(n)
	
	implicit def liquidToProxy(o: Liquid): LiquidProxy = new LiquidProxy(kb, o)
	implicit def partToProxy(o: Part): PartProxy = new PartProxy(kb, o)
	implicit def plateToProxy(o: Plate): PlateProxy = new PlateProxy(kb, o)
}

class Tester extends Roboliq {
	val water = new Liquid
	val plate = new Plate
	
	protocol {
		pipette(water, plate, 30 ul)
	}
	
	customize {
		val p2 = Plate(rows = 8, cols = 1, location = "P2")

		water.liquidClass = "water"
		
		plate.location = "P1"
		
		p2.location = "P2"
		
		/*setInitialLiquids(
			water -> p2.well(1)
		)*/
	}
}

class Tester2 extends Roboliq {
	val water = new Liquid
	val liquid_plasmidDna = new Liquid
	val liquid_competentCells = new Liquid
	val liquid_ssDna = new Liquid
	val liquid_liAcMix = new Liquid
	
	val plate_template = new Plate
	val plate_working = new Plate
	
	def decontamination_WashBigTips() {
		
	}
	
	def pcrDispense(volume: Double) {
		decontamination_WashBigTips()
		pipette(plate_template, plate_working, volume)
		decontamination_WashBigTips()
	}
	
	def competentYeastDispense() {
		pipette(liquid_plasmidDna, plate_working, 2)
		pipette(liquid_competentCells, plate_working, 30)
		pipette(liquid_ssDna, plate_working, 5)
		pipette(liquid_liAcMix, plate_working, 90)
		mix(plate_working, 90, 4)
	}
	
	def heatShock() {
		
	}
	
	pcrDispense(3)
	competentYeastDispense()
} 

object Main extends App {
	val tester = new Tester
	val handler = new PipetteCommandHandler(tester.kb, tester.cmds.head.asInstanceOf[PipetteCommand])
	val res = handler.exec()
	println(res)
	tester.kb.printUnknown()
}
