package roboliq.common

import scala.collection.mutable.ArrayBuffer

import roboliq.common._


class Volume(n: Double) {
	def ul = n
}

trait RoboliqCommands {
	val cmds: ArrayBuffer[Command]
}

trait L4_Roboliq {
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
	
	implicit def intToVolume(n: Int): Volume = new Volume(n)
	
	/*implicit def liquidToProxy(o: Liquid): LiquidProxy = new LiquidProxy(kb, o)
	implicit def partToProxy(o: Part): PartProxy = new PartProxy(kb, o)
	implicit def wellToProxy(o: Well): WellProxy = new WellProxy(kb, o)*/
	implicit def plateToProxy(o: Plate): PlateProxy = new PlateProxy(kb, o)
}
