package roboliq.protocol.commands

import roboliq.protocol._
import roboliq.common.KnowledgeBase
import roboliq.common


class Pcr extends PCommand {
	type Product = PcrProduct
	
	val products = new PropertyItem[Product]
	val volumes = new Property[LiquidVolume]
	val mixSpec = new PropertyItem[PcrMixSpec]
	
	def properties: List[Property[_]] = List(products, volumes, mixSpec)

	override def getNewPools(): List[Pool] = {
		List.fill(products.values.length)(new Pool("PCR"))
	}
	
	/*
	def createCommands(vom: ValueToObjectMap): List[common.Command] = {
		import roboliq.commands.pipette.L4A_PipetteItem
		import roboliq.commands.pipette.MixSpec
		import roboliq.commands.MixItemL4
		import roboliq.commands.MixItemReagentL4
		import roboliq.commands.MixItemTemplateL4
		
		/*object Liquids {
			val water = new Liquid("Water", CleanPolicy.TNN)
			val buffer10x = new Liquid("Water", CleanPolicy.TNT)
			val dNTP = new Liquid("Water", CleanPolicy.TNT)
			val primerF = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
			val primerB = new Liquid("Water", Set(Contaminant.DNA), CleanPolicy.DDD)
			val polymerase = new Liquid("Glycerol", CleanPolicy.TNT)
		}
		
		val well_template = new common.WellPointerVar
		val well_masterMix = new common.WellPointerVar
		val plate_working = new Plate
		val plate_balance = new Plate
		*/
		
		val pipetteItems = List[L4A_PipetteItem](
			new L4A_PipetteItem(Liquids.water, well1, List(15.7), None, None)
		)
		val mixItems = Seq[MixItemL4](
			MixItemReagentL4(Liquids.buffer10x, 10, 1),
			MixItemReagentL4(Liquids.dNTP, 2, .2),
			MixItemReagentL4(Liquids.primerF, 50, .5),
			MixItemReagentL4(Liquids.primerB, 50, .5),
			MixItemReagentL4(Liquids.polymerase, 5, 0.25/25),
			MixItemTemplateL4(well_template, Seq(20), 0.2)
		)
		
		pcrMix(plate_working(C6+4), mixItems, Liquids.water, 50 ul, well_masterMix)
		seal(plate_working)
		val setup_thermocycle = thermocycle(plate_working)
		val setup_centrifuge = centrifuge(plate_working)
		peel(plate_working)
	}
	*/
}

class PcrProduct extends Item {
	val template = new PropertyItem[Liquid]
	val forwardPrimer = new PropertyItem[Liquid]
	val backwardPrimer = new PropertyItem[Liquid]
	def properties: List[Property[_]] = List(template, forwardPrimer, backwardPrimer)
}

class PcrMixSpec extends Item {
	class Item {
		val liquid = new PropertyItem[Liquid]
		val amt0 = new Property[LiquidAmount]
		val amt1 = new Property[LiquidAmount]
		def properties = List[Property[_]](liquid, amt0, amt1)
	}
	val waterLiquid = new PropertyItem[Liquid]
	val buffer = new Item
	val dntp = new Item
	val templateLiquid = new PropertyItem[Liquid]
	val templateConc = new Property[LiquidAmount]
	val forwardPrimerLiquid = new PropertyItem[Liquid]
	val forwardPrimerConc = new Property[LiquidAmount]
	val backwardPrimerLiquid = new PropertyItem[Liquid]
	val backwardPrimerConc = new Property[LiquidAmount]
	val polymerase = new Item
	def properties: List[Property[_]] = waterLiquid :: List(buffer, dntp, polymerase).flatMap(_.properties)
}
