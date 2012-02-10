package roboliq.protocol.commands

import roboliq.protocol._
import roboliq.common.KnowledgeBase
import roboliq.common


class Pcr extends PCommand {
	type Product = PcrProduct
	
	val products = new Property[Product]
	val volumes = new Property[PLiquidVolume]
	val mixSpec = new Property[PcrMixSpec]
	
	def properties: List[Property[_]] = List(products, volumes, mixSpec)

	override def getNewPools(): List[Pool] = {
		List.fill(products.values.length)(new Pool("PCR"))
	}
	
	def createCommands(ild: ItemListData, kb: KnowledgeBase): List[common.Command] = {
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
}

class PcrProduct extends Item {
	val template = new Property[Liquid]
	val forwardPrimer = new Property[Liquid]
	val backwardPrimer = new Property[Liquid]
	def properties: List[Property[_]] = List(template, forwardPrimer, backwardPrimer)
}

class PcrMixSpec extends Item {
	class Item {
		val liquid = new Property[Liquid]
		val amt0 = new Property[PLiquidAmount]
		val amt1 = new Property[PLiquidAmount]
		def properties = List[Property[_]](liquid, amt0, amt1)
	}
	val waterLiquid = new Property[Liquid]
	val buffer = new Item
	val dntp = new Item
	val templateLiquid = new Property[Liquid]
	val templateConc = new Property[PLiquidAmount]
	val forwardPrimerLiquid = new Property[Liquid]
	val forwardPrimerConc = new Property[PLiquidAmount]
	val backwardPrimerLiquid = new Property[Liquid]
	val backwardPrimerConc = new Property[PLiquidAmount]
	val polymerase = new Item
	def properties: List[Property[_]] = waterLiquid :: List(buffer, dntp, polymerase).flatMap(_.properties)
}
