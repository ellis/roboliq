package roboliq.protocol.commands

import roboliq.commands.pipette.PipetteCommandsL4
import roboliq.commands.seal.SealCommandsL4
import roboliq.common.KnowledgeBase
import roboliq.common
import roboliq.protocol._

class Pcr extends PCommand {
	type Product = PcrProduct
	
	val products = new PropertyItem[Product]
	val volumes = new Property[LiquidVolume]
	val mixSpec = new PropertyItem[PcrMixSpec]
	
	def properties: List[Property[_]] = List(products, volumes, mixSpec)
	
	// FIXME: This mutability is not OK! Figure out another way to associate products with pools.
	//  perhaps create a map from Any -> Pool and pass that back in getNewPools()
	private var m_pools: List[Pool] = Nil

	override def getNewPools(): List[Pool] = {
		m_pools = List.fill(products.values.length)(new Pool("PCR"))
		m_pools
	}
	
	/** Change a list of options to an option of a list.  Only succeed if all items are defined. */
	def invert[A](l: List[Option[A]]): Option[List[A]] = {
		if (l.forall(_.isDefined)) Some(l.flatten)
		else None
	}
	
	def getWellPointer(property: PropertyItem[Liquid], vom: ValueToObjectMap): Option[common.WellPointer] = {
		invert(property.values.map(vom.mapValueToWellPointer.get)) match {
			case None => None
			case Some(Nil) => None
			case Some(x :: Nil) => Some(x)
			case _ => None // FIXME: merge WellPointers 
		}
	}

	def getWellPointer(pool: Pool, vom: ValueToObjectMap): Option[common.WellPointer] = {
		vom.mapPoolToWellPointer.get(pool)
	}

	private class PcrProductItemL5(
		val src: common.WellPointer,
		val amt0: LiquidAmount,
		val amt1: LiquidAmount
	)
	private class PcrProductL5(
		val template: PcrProductItemL5,
		val forwardPrimer: PcrProductItemL5,
		val backwardPrimer: PcrProductItemL5
	)
	private def getPcrProduct5(product6: PcrProduct, mixSpec6: PcrMixSpec, vom: ValueToObjectMap): Option[PcrProductL5] = {
		for {
			templateSrc <- getWellPointer(product6.template, vom)
			template <- getPcrProductItem5(templateSrc, mixSpec6.template, vom.valueDb)
			forwardPrimerSrc <- getWellPointer(product6.forwardPrimer, vom)
			forwardPrimer <- getPcrProductItem5(forwardPrimerSrc, mixSpec6.forwardPrimer, vom.valueDb)
			backwardPrimerSrc <- getWellPointer(product6.backwardPrimer, vom)
			backwardPrimer <- getPcrProductItem5(backwardPrimerSrc, mixSpec6.backwardPrimer, vom.valueDb)
		} yield {
			new PcrProductL5(template, forwardPrimer, backwardPrimer)
		}
	}
	private def getPcrProductItem5(src: common.WellPointer, mixItem6: PcrMixSpecItem, vd: ValueDatabase): Option[PcrProductItemL5] = {
		for {
			amt0 <- mixItem6.amt0.getValue(vd)
			amt1 <- mixItem6.amt1.getValue(vd)
		} yield {
			new PcrProductItemL5(src, amt0, amt1)
		}
	}
	private class MixItemL5(
		val src: common.WellPointer,
		val dest: common.WellPointer,
		val vol: LiquidVolume
	)
	// Get the mix items for the given product
	private def getMixItems5(product5: PcrProductL5, dest: common.WellPointer, mixSpec6: PcrMixSpec, vol: LiquidVolume, vom: ValueToObjectMap): Option[List[MixItemL5]] = {
		def getVol(amt0: LiquidAmount, amt1: LiquidAmount): Option[LiquidVolume] = {
			amt1 match {
				case LiquidAmountByVolume(vol1) => Some(vol1)
				case LiquidAmountByConc(conc1) =>
					amt0 match {
						case LiquidAmountByConc(conc0) => Some(LiquidVolume.nl(vol.nl / (conc0.nM.toDouble / conc1.nM).toInt))
						case _ => None
					}
				case LiquidAmountByFactor(factor1) =>
					amt0 match {
						case LiquidAmountByFactor(factor0) => Some(LiquidVolume.nl(vol.nl / (factor0.toDouble / factor1).toInt))
						case _ => None
					}
			}
		}
		def getProdVol(item5: PcrProductItemL5): Option[LiquidVolume] = getVol(item5.amt0, item5.amt1)
		def getMixSpecVol(mixItem6: PcrMixSpecItem): Option[LiquidVolume] = {
			for {
				amt0 <- mixItem6.amt0.getValue(vom.valueDb)
				amt1 <- mixItem6.amt1.getValue(vom.valueDb)
				vol <- getVol(amt0, amt1)
			} yield {
				vol
			}
		}
		def getMixItem5(mixItem6: PcrMixSpecItem): Option[MixItemL5] = {
			for {
				src <- getWellPointer(mixItem6.liquid, vom)
				vol <- getMixSpecVol(mixItem6)
			} yield {
				new MixItemL5(src, dest, vol)
			}			
		}
		def getProductMixItem5(item5: PcrProductItemL5): Option[MixItemL5] = {
			for {
				vol <- getProdVol(item5)
			} yield {
				new MixItemL5(item5.src, dest, vol)
			}			
		}
		
		for {
			srcWater <- getWellPointer(mixSpec6.waterLiquid, vom)
			// Before adding water
			lMixItem0 <- invert(List(
				getMixItem5(mixSpec6.buffer),
				getMixItem5(mixSpec6.dntp),
				getProductMixItem5(product5.template),
				getProductMixItem5(product5.forwardPrimer),
				getProductMixItem5(product5.backwardPrimer),
				getMixItem5(mixSpec6.polymerase)
			))
		} yield {
			// Before adding water
			val vol0 = LiquidVolume.nl(lMixItem0.foldLeft(0) {(acc, mixItem5) => acc + mixItem5.vol.nl})
			val volWater = LiquidVolume.nl(vol.nl - vol0.nl)
			val mixItemWater5 = new MixItemL5(srcWater, dest, volWater)
			mixItemWater5 :: lMixItem0
		}
	}

	def createCommands(vom: ValueToObjectMap): List[common.Command] = {
		import roboliq.commands.pipette.L4A_PipetteItem
		import roboliq.commands.pipette.MixSpec

		val valueDb = vom.valueDb
		val x = for {
			nVolume <- volumes.getValue(valueDb)
			mixSpec6 <- this.mixSpec.getValue(valueDb)
			products6 <- this.products.getValues(valueDb) match { case Nil => None; case l => Some(l) }
			lProduct5 <- invert(products6.map(product6 => getPcrProduct5(product6, mixSpec6, vom)))
			water4 <- getWellPointer(mixSpec6.waterLiquid, vom)
			lDest4 <- invert(m_pools.map(pool => getWellPointer(pool, vom)))
			val lProductDest = lProduct5 zip lDest4
			llMixItem <- invert(lProductDest.map(pair => {
				val (product5, dest4) = pair
				getMixItems5(product5, dest4, mixSpec6, nVolume, vom)
			}))
		} yield {
			def mixNext(llMixItem: List[List[MixItemL5]], acc: List[L4A_PipetteItem]): List[L4A_PipetteItem] = {
				if (llMixItem.isEmpty) return acc
				val acc2 = acc ++ llMixItem.map(lMixItem => {
					val mixItem5 = lMixItem.head 
					new L4A_PipetteItem(mixItem5.src, mixItem5.dest, List(mixItem5.vol.nl / 1000.0), None, None)
				})
				val llMixItem2 = llMixItem.map(_.tail).filter(!_.isEmpty)
				mixNext(llMixItem2, acc2)
			}
			val lPipetteItem = mixNext(llMixItem, Nil)
			val lPipetteCmd = roboliq.commands.pipette.L4C_Pipette(new roboliq.commands.pipette.L4A_PipetteArgs(lPipetteItem, tipOverrides_? = None)) :: Nil
			
			val dests = lDest4.reduce(_ + _)
			
			val lMixCmd = PipetteCommandsL4.mix(dests, nVolume.nl * 0.75 / 1000.00, 4) match {
				case common.Success(cmd) => List(cmd)
				case _ => Nil
			}
			
			val lSealCmd = SealCommandsL4.seal(dests) match {
				case common.Success(lCmd) => lCmd
				case _ => Nil
			}
			
			val lCmd: List[common.Command] = lPipetteCmd ++ lMixCmd ++ lSealCmd
			lCmd
		}
		x match {
			case None => Nil
			case Some(lCmd) => lCmd
		}
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
	
	override def toString: String = {
		"PcrProduct(template="+template.toContentString+", forwardPrimer="+forwardPrimer.toContentString+", backwardPrimer="+backwardPrimer.toContentString+")"  
	}
}

class PcrMixSpecItem {
	val liquid = new PropertyItem[Liquid]
	val amt0 = new Property[LiquidAmount]
	val amt1 = new Property[LiquidAmount]
	def properties = List[Property[_]](liquid, amt0, amt1)
}

class PcrMixSpec extends Item {
	val waterLiquid = new PropertyItem[Liquid]
	val buffer, dntp, template, forwardPrimer, backwardPrimer, polymerase = new PcrMixSpecItem
	def properties: List[Property[_]] = waterLiquid :: List(buffer, dntp, template, forwardPrimer, backwardPrimer, polymerase).flatMap(_.properties)
}
