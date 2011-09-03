package roboliq.protocol

import scala.collection.mutable.ArrayBuffer

import roboliq.common
import roboliq.common._
import roboliq.commands.pipette._


trait CommonProtocol { thisObj =>
	val kb = new KnowledgeBase
	val cmds = new ArrayBuffer[Command]
	var m_protocol: Option[() => Unit] = None
	var m_customize: Option[() => Unit] = None

	abstract class LiquidFamily
	object LiquidFamily {
		case object Water extends LiquidFamily
		case object WaterLike extends LiquidFamily
	}
	
	abstract class PlateFamily
	object PlateFamily {
		case object Standard extends PlateFamily
	}
	
	class Liquid(val family: LiquidFamily) extends roboliq.common.Liquid("unnamed", true, false, Set()) {
		//def set()
	}
	
	class Plate(val family: PlateFamily) extends roboliq.protocol.Plate {
		val protocol = thisObj
		val obj = new common.Plate
		val setup = kb.getPlateSetup(obj)
		val proxy = new PlateProxy(kb, obj)
		
		def set(model: PlateModel, location: String) {
			setup.model_? = Some(model)
			proxy.setDimension(model.nRows, model.nCols)
			proxy.location = location
		}
	}
	
	
	def protocol(fn: => Unit) {
		m_protocol = Some(fn _)
	}
	
	def customize(fn: => Unit) {
		m_customize = Some(fn _)
	}
	
	object A1 extends WellLocA(WellCoord(0, 0))
	object G7 extends WellLocA(WellCoord(6, 6))
	
	implicit def intToVolume(n: Int): Volume = new Volume(n)
	
	/*implicit def liquidToProxy(o: Liquid): LiquidProxy = new LiquidProxy(kb, o)
	implicit def partToProxy(o: Part): PartProxy = new PartProxy(kb, o)
	implicit def wellToProxy(o: Well): WellProxy = new WellProxy(kb, o)*/
	implicit def plateToObj(o: Plate): common.Plate = o.obj
	implicit def plateToProxy(o: Plate): PlateProxy = new PlateProxy(kb, o.obj)
	implicit def plateToWPL(o: Plate): WellOrPlateOrLiquid = WPL_Plate(o.obj)
	implicit def plateToWP(o: Plate): WellOrPlate = WP_Plate(o.obj)
}
