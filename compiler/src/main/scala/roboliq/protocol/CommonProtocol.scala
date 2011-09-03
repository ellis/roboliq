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
	
	class Liquid(val family: LiquidFamily) extends roboliq.common.Liquid(null, true, false, Set()) {
		kb.addLiquid(this)
		
		def fill(plate: Plate) {
			for (well <- plate.wells) {
				val wellSetup = kb.getWellSetup(well)
				wellSetup.liquid_? = Some(this)
			}
		}
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
	
	def __findPlateLabels() {
		val c = this.getClass()
		for (f <- this.getClass().getDeclaredFields()) {
			val t = f.getType()
			if (t == classOf[Plate]) {
				f.setAccessible(true)
				val o = f.get(this).asInstanceOf[Plate]
				if (o.setup.sLabel_?.isEmpty)
					o.setup.sLabel_? = Some(f.getName())
			}
			else if (t == classOf[Liquid]) {
				f.setAccessible(true)
				val o = f.get(this).asInstanceOf[Liquid]
				if (o.sName == null)
					o.sName = f.getName()
			}
		}
	}
	
	/*private def getLabel(o: Any): Option[String] = {
		val c = this.getClass()
		for (f <- this.getClass().getDeclaredFields()) {
			f.setAccessible(true)
			val o2 = f.get(this)
			if (o2 eq o) {
				return Some(f.getName())
			}
		}
		None
	}*/
	
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

	//implicit def liquidToObj(o: Liquid): common.Liquid = o.obj
}
