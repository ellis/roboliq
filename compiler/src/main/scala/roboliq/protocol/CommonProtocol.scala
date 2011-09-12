package roboliq.protocol

import scala.collection.mutable.ArrayBuffer

import roboliq.common
import roboliq.common._
import roboliq.commands._
import roboliq.commands.move._
import roboliq.commands.pipette._
import roboliq.commands.shake._


trait CommonProtocol extends
	PipetteCommands with
	ShakeCommands
{ thisObj =>
	type Location = common.Location
	
	val Contaminant = common.Contaminant
	val CleanPolicy = common.GroupCleanPolicy
	
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
	
	class Liquid private (
		family: String,
		contaminants: Set[Contaminant.Value],
		cleanPolicy_? : Option[GroupCleanPolicy]
	) extends common.Reagent {
		def this(family: String) = this(family, Set[Contaminant.Value](), None)
		def this(family: String, contaminants: Set[Contaminant.Value]) = this(family, contaminants, None)
		def this(family: String, contaminants: Set[Contaminant.Value], cleanPolicy: GroupCleanPolicy) = this(family, contaminants, Some(cleanPolicy))
		def this(family: String, cleanPolicy: GroupCleanPolicy) = this(family, Set[Contaminant.Value](), Some(cleanPolicy))
		
		val setup = kb.getReagentSetup(this)
		
		kb.addReagent(this)
		setup.sFamily_? = Some(family)
		setup.contaminants = contaminants
		if (cleanPolicy_?.isDefined)
			setup.group_? = Some(new LiquidGroup(cleanPolicy_?.get))
		
		/*def fill(plate: Plate) {
			for (well <- plate.wells) {
				val wellSetup = kb.getWellSetup(well)
				wellSetup.liquid_? = Some(this)
			}
		}*/
	}
	
	class Plate private (
		family_? : Option[PlateFamily]
	) extends roboliq.protocol.Plate {
		def this() = this(None)
		def this(family: PlateFamily) = this(Some(family))
		
		val protocol = thisObj
		val obj = new common.Plate
		val setup = kb.getPlateSetup(obj)
		val proxy = new PlateProxy(kb, obj)
		
		//if (family_?.isDefined) {
		//	setup.
		//}
		
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
				if (o.setup.sName_? == None)
					o.setup.sName_? = Some(f.getName())
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
