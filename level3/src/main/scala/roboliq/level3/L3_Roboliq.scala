package roboliq.level3

import scala.collection.mutable.ArrayBuffer

import roboliq.common._


class Volume(n: Double) {
	def ul = n
}

trait L3_Roboliq {
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
	
	def mix(dest: WellOrPlate, volume: Double, count: Int) {
		
	}
	
	implicit def wellToWPL(o: Well): WellOrPlateOrLiquid = WPL_Well(o)
	implicit def plateToWPL(o: Plate): WellOrPlateOrLiquid = WPL_Plate(o)
	implicit def liquidToWPL(o: Liquid): WellOrPlateOrLiquid = WPL_Liquid(o)
	
	implicit def wellToWP(o: Well): WellOrPlate = WP_Well(o)
	implicit def plateToWP(o: Plate): WellOrPlate = WP_Plate(o)
	
	implicit def intToVolume(n: Int): Volume = new Volume(n)
	
	/*implicit def liquidToProxy(o: Liquid): LiquidProxy = new LiquidProxy(kb, o)
	implicit def partToProxy(o: Part): PartProxy = new PartProxy(kb, o)
	implicit def wellToProxy(o: Well): WellProxy = new WellProxy(kb, o)
	implicit def plateToProxy(o: Plate): PlateProxy = new PlateProxy(kb, o)*/
}
