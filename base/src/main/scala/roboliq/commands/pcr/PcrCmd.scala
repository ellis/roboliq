package roboliq.commands.pcr

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

import roboliq.core._
import roboliq.commands.pipette.PipetteCmdBean


class PcrCmdBean extends CmdBean {
	@BeanProperty var dest: String = null
	@BeanProperty var products: java.util.List[PcrProductBean] = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var mixSpec: PcrMixSpecBean = null
}

class PcrCmdHandler extends CmdHandlerA[PcrCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonNull("dest")
		messages.paramMustBeNonNull("products")
		messages.paramMustBeNonNull("volume")
		messages.paramMustBeNonNull("mixSpec")
		if (messages.hasErrors)
			return Expand1Errors()
		
		for {
			ℓℓResourceProduct <- Result.mapOver(cmd.products.toList)(_.expand1A())
			ℓResourceMixSpec <- cmd.mixSpec.expand1A()
		} {
			return Expand1Resources(
				ℓℓResourceProduct.flatten ++
				ℓResourceMixSpec ++
				List(NeedSrc("water"), NeedPool("a", "pcr", cmd.products.size)) ++
				List(NeedDest(cmd.dest))
			)
		}
		return Expand1Errors()
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val query = ctx.states
		val volumeSample = LiquidVolume.l(cmd.volume)
		val x: Result[Expand2Result] = for {
			ℓdest <- query.findDestWells(cmd.dest)
			_ <- Result.assert(ℓdest.length == cmd.products.size(), "product list and destination list must have same size")
			water <- query.findSubstance("water")
			ℓℓitem <- Result.mapOver(ℓdest zip cmd.products)(pair => Item.from(pair._1, pair._2, cmd.mixSpec, volumeSample, water, query))
			/*buffer <- SubSrcConc.fromBean(cmd.mixSpec.buffer, query)
			dntp <- SubSrcConc.fromBean(cmd.mixSpec.dntp, query)
			polymerase <- SubSrcConc.fromBean(cmd.mixSpec.polymerase, query)
			ℓproduct <- Result.mapOver(cmd.products.toList)(PcrProduct.fromBean(query))
			ℓmixSpec <- PcrMixSpec.fromBean(query)(cmd.mixSpec)
			ℓitem <- Result.mapOver(ℓmixSpec)(createItem)
			ℓdestℓitem <- (ℓdest zip ℓitem)*/
		} yield {
			// Get source strings from ℓℓitem
			def getSrcs(ℓℓitem: List[List[Item]], acc: List[String]): String = {
				if (ℓℓitem.head.isEmpty)
					acc.mkString(",")
				else {
					val accº = acc ++ ℓℓitem.map(_.head.substance.id)
					val ℓℓitemº = ℓℓitem.map(_.tail)
					getSrcs(ℓℓitemº, accº)
				}
			}
			val srcs = getSrcs(ℓℓitem, Nil)
			
			// Get dest strings from ℓℓitem
			def getDests(ℓℓitem: List[List[Item]], acc: List[String]): String = {
				if (ℓℓitem.head.isEmpty)
					acc.mkString(",")
				else {
					val accº = acc ++ ℓℓitem.map(_.head.dest.id)
					val ℓℓitemº = ℓℓitem.map(_.tail)
					getDests(ℓℓitemº, accº)
				}
			}
			val dests = getDests(ℓℓitem, Nil)

			// Get volume strings from ℓℓitem
			def getVolumes(ℓℓitem: List[List[Item]], acc: List[java.math.BigDecimal]): List[java.math.BigDecimal] = {
				if (ℓℓitem.head.isEmpty)
					acc
				else {
					val accº = acc ++ ℓℓitem.map(_.head.volume.l.bigDecimal)
					val ℓℓitemº = ℓℓitem.map(_.tail)
					getVolumes(ℓℓitemº, accº)
				}
			}
			val volumes = getVolumes(ℓℓitem, Nil)
			
			val pipette = new PipetteCmdBean
			pipette.src = srcs
			pipette.dest = dests
			pipette.volume = seqAsJavaList(volumes)
			
			/*
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
			*/
			
			// FIXME: add real doc
			val doc = null
			
			Expand2Cmds(List(pipette), Nil, doc)
		}
		
		x match {
			case Error(ls) => ls.foreach(messages.addError); Expand2Errors()
			case Success(expand) => expand
		}
	}

	//-------------------------------------------------
	/*
	type Product = PcrProductBean
	
	
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
		val src: List[Well2],
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
	private def getPcrProductItem5(src: List[Well2], mixItem6: SubSrcConc, vd: ValueDatabase): Option[PcrProductItemL5] = {
		for {
			amt0 <- mixItem6.amt0.getValue(vd)
			amt1 <- mixItem6.amt1.getValue(vd)
		} yield {
			new PcrProductItemL5(src, amt0, amt1)
		}
	}
	private class MixItemL5(
		val src: List[Well2],
		val dest: Well2,
		val vol: LiquidVolume
	)
	// Get the mix items for the given product
	private def getMixItems5(product5: PcrProductL5, dest: Well2, mixSpec6: PcrMixSpec, vol: LiquidVolume, vom: ValueToObjectMap): Option[List[MixItemL5]] = {
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
		def getMixSpecVol(mixItem6: SubSrcConc): Option[LiquidVolume] = {
			for {
				amt0 <- mixItem6.amt0.getValue(vom.valueDb)
				amt1 <- mixItem6.amt1.getValue(vom.valueDb)
				vol <- getVol(amt0, amt1)
			} yield {
				vol
			}
		}
		def getMixItem5(mixItem6: SubSrcConc): Option[MixItemL5] = {
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

	def createCommands(): Result[List[CmdBean]] = {
		for {
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
	*/
}

class PcrProductBean {
	@BeanProperty var template: String = null
	@BeanProperty var forwardPrimer: String = null
	@BeanProperty var backwardPrimer: String = null
	
	def expand1A(): Result[List[NeedResource]] = {
		for {
			_ <- Result.mustBeSet(template, "template")
			_ <- Result.mustBeSet(forwardPrimer, "forwardPrimer")
			_ <- Result.mustBeSet(backwardPrimer, "backwardPrimer")
			
		} yield {
			List(
				NeedSrc(template),
				NeedSrc(forwardPrimer),
				NeedSrc(backwardPrimer)
			)
		}
	}
	
	override def toString: String = {
		"PcrProduct(template="+template+", forwardPrimer="+forwardPrimer+", backwardPrimer="+backwardPrimer+")"
	}
}

class PcrProduct(
	val template: SubSrcConc, //List[Well2],
	val forwardPrimer: List[Well2],
	val backwardPrimer: List[Well2]
)

/*object PcrProduct {
	def fromBean(query: StateQuery)(bean: PcrProductBean): Result[PcrProduct] = {
		for {
			_ <- Result.mustBeSet(bean.template, "template")
			_ <- Result.mustBeSet(bean.forwardPrimer, "forwardPrimer")
			_ <- Result.mustBeSet(bean.backwardPrimer, "backwardPrimer")
			template <- SubSrcConc(bean.template, )
			forwardPrimer <- query.mapIdToWell2List(bean.forwardPrimer)
			backwardPrimer <- query.mapIdToWell2List(bean.backwardPrimer)
		} yield {
			new PcrProduct(template, forwardPrimer, backwardPrimer)
		}
	}
}*/

class PcrMixSpecItemBean {
	@BeanProperty var liquid: String = null
	//@BeanProperty var amt0: java.math.BigDecimal = null
	//@BeanProperty var amt1: java.math.BigDecimal = null
	@BeanProperty var conc: java.math.BigDecimal = null

	/*def expand1A(): Result[List[NeedResource]] = {
		for {
			_ <- Result.mustBeSet(conc, "conc")
		} yield {
			if (liquid == null) Nil
			else List(NeedSrc(liquid))
		}
	}*/
}

class SubSrcConc(
	val substance: Substance,
	val src: List[Well2],
	val concSrc: BigDecimal,
	val concDest: BigDecimal
)

object SubSrcConc {
	def fromBean(query: StateQuery)(bean: PcrMixSpecItemBean, liquid_? : Option[String] = None): Result[SubSrcConc] = {
		val liquid = liquid_?.getOrElse(bean.liquid)
		for {
			_ <- Result.mustBeSet(liquid, "liquid")
			_ <- Result.mustBeSet(bean.conc, "conc")
			substance <- query.findSubstance(liquid)
			src <- query.mapIdToWell2List(liquid)
			srcState <- query.findWellState(src.head.id)
			concSrc <- srcState.content.concOfSubstance(substance)
		} yield {
			new SubSrcConc(substance, src, concSrc, bean.conc)
		}
	}
	
	def apply(idSubstance: String, concDest: BigDecimal, query: StateQuery): Result[SubSrcConc] = {
		for {
			_ <- Result.mustBeSet(idSubstance, "liquid")
			_ <- Result.mustBeSet(concDest, "conc")
			substance <- query.findSubstance(idSubstance)
			src <- query.mapIdToWell2List(idSubstance)
			srcState <- query.findWellState(src.head.id)
			concSrc <- srcState.content.concOfSubstance(substance)
			_ <- Result.assert(concSrc > 0, "vessel `"+src.head.id+"` contains no solvent")
		} yield {
			new SubSrcConc(substance, src, concSrc, concDest)
		}
	}

	def fromBean(mixSpecItemBean: PcrMixSpecItemBean, query: StateQuery): Result[SubSrcConc] = {
		apply(mixSpecItemBean.liquid, mixSpecItemBean.conc, query)
	}
}

class PcrMixSpecBean {
	//@BeanProperty var waterLiquid: String = null
	@BeanProperty var buffer: PcrMixSpecItemBean = null
	@BeanProperty var dntp: PcrMixSpecItemBean = null
	@BeanProperty var template: PcrMixSpecItemBean = null
	@BeanProperty var forwardPrimer: PcrMixSpecItemBean = null
	@BeanProperty var backwardPrimer: PcrMixSpecItemBean = null
	@BeanProperty var polymerase: PcrMixSpecItemBean = null

	def expand1A(): Result[List[NeedResource]] = {
		for {
			_ <- Result.mustBeSet(buffer, "buffer")
			_ <- Result.mustBeSet(buffer.liquid, "buffer.liquid")
			_ <- Result.mustBeSet(dntp, "dntp")
			_ <- Result.mustBeSet(dntp.liquid, "dntp.liquid")
			_ <- Result.mustBeSet(template, "template")
			_ <- Result.mustBeSet(forwardPrimer, "forwardPrimer")
			_ <- Result.mustBeSet(backwardPrimer, "backwardPrimer")
			_ <- Result.mustBeSet(polymerase, "polymerase")
			_ <- Result.mustBeSet(polymerase.liquid, "polymerase.liquid")
		} yield {
			List(
				NeedSrc(buffer.liquid),
				NeedSrc(dntp.liquid),
				NeedSrc(polymerase.liquid)
			)
		}
	}
}

class PcrMixSpec(
	val buffer: SubSrcConc,
	val dntp: SubSrcConc,
	val templateConc: BigDecimal,
	val forwardPrimerConc: BigDecimal,
	val backwardPrimerConc: BigDecimal,
	val polymerase: SubSrcConc
)

object PcrMixSpec {
	def fromBean(query: StateQuery)(bean: PcrMixSpecBean): Result[PcrMixSpec] = {
		val l = List(bean.buffer, bean.dntp, bean.polymerase)
		for {
			_ <- Result.mustBeSet(bean.buffer, "buffer")
			_ <- Result.mustBeSet(bean.dntp, "dntp")
			_ <- Result.mustBeSet(bean.template, "template")
			_ <- Result.mustBeSet(bean.template.conc, "template.conc")
			_ <- Result.mustBeSet(bean.forwardPrimer, "forwardPrimer")
			_ <- Result.mustBeSet(bean.forwardPrimer.conc, "forwardPrimer.conc")
			_ <- Result.mustBeSet(bean.backwardPrimer, "backwardPrimer")
			_ <- Result.mustBeSet(bean.backwardPrimer.conc, "backwardPrimer.conc")
			_ <- Result.mustBeSet(bean.polymerase, "polymerase")
			buffer <- SubSrcConc.fromBean(query)(bean.buffer)
			dntp <- SubSrcConc.fromBean(query)(bean.dntp)
			polymerase <- SubSrcConc.fromBean(query)(bean.polymerase)
		} yield {
			new PcrMixSpec(
				buffer,
				dntp,
				bean.template.conc,
				bean.forwardPrimer.conc,
				bean.backwardPrimer.conc,
				polymerase
			)
		}
	}
}

private class Item(
	val substance: Substance,
	//val src: List[Well2],
	val dest: Well2,
	val volume: LiquidVolume
)

private object Item {
	def from(dest: Well2, product: PcrProductBean, mixSpec: PcrMixSpecBean, volumeSample: LiquidVolume, water: Substance, query: StateQuery): Result[List[Item]] = {
		for {
			buffer <- SubSrcConc.fromBean(mixSpec.buffer, query)
			dntp <- SubSrcConc.fromBean(mixSpec.dntp, query)
			template <- SubSrcConc(product.template, mixSpec.template.conc, query)
			forwardPrimer <- SubSrcConc(product.forwardPrimer, mixSpec.forwardPrimer.conc, query)
			backwardPrimer <- SubSrcConc(product.backwardPrimer, mixSpec.backwardPrimer.conc, query)
			polymerase <- SubSrcConc.fromBean(mixSpec.polymerase, query)
			ℓssc = List(buffer, dntp, template, forwardPrimer, backwardPrimer, polymerase)
			ℓitem1 <- Result.mapOver(ℓssc)(ssc => apply(dest, ssc, volumeSample))
			itemWater <- createWaterItem(dest, water, ℓitem1, volumeSample)
		} yield itemWater :: ℓitem1
	}
	
	def apply(dest: Well2, ssc: SubSrcConc, volumeSample: LiquidVolume): Result[Item] = {
		for {
			_ <- Result.assert(ssc.concSrc >= ssc.concDest, "the concentration of the source reagent must not be higher than desired target concentration")
			volume = volumeSample * (ssc.concDest / ssc.concSrc)
			_ <- Result.assert(volume >= LiquidVolume.ul(0.1), "the concentration of the source reagent `"+ssc.substance.id+"` is too high, such that the pipette volume is under 0.1ul")
		} yield {
			//val srcContent = srcState.content
			new Item(ssc.substance, dest, volume)
		}
	}
	
	private def createWaterItem(dest: Well2, water: Substance, ℓitem1: List[Item], volumeSample: LiquidVolume): Result[Item] = {
		// Volume of the other items so far
		val volume1 = ℓitem1.foldLeft(LiquidVolume.empty)((acc, item) => acc + item.volume)
		val volumeWater = volumeSample - volume1
		if (volumeWater >= LiquidVolume.empty)
			Success(new Item(water, dest, volumeWater))
		else
			Error("volume of reagents would excede sample volume")
	}
	
	/*def apply(
		dest: Well2,
		product: PcrProduct,
		mixSpec: PcrMixSpec,
		volumeSample: LiquidVolume,
		query: StateQuery
	): Result[List[Item]] = {
		val l = List(
			makeOne(dest, mixSpec.buffer, volumeSample, query),
			makeOne(dest, mixSpec.dntp, volumeSample, query),
			makeOne(dest, product.template mixSpec.templateConc, volumeSample, query)
			makeOne(dest, mixSpec.polymerase, volumeSample, query)
		)
		for {
			item1 <- makeOne(dest, mixSpec.buffer, volumeSample, query)
			item1 <- makeOne(dest, mixSpec.buffer, volumeSample, query)
			item1 <- makeOne(dest, mixSpec.buffer, volumeSample, query)
		} yield {
			Nil
		}
	}
	
	private def makeOne(dest: Well2, mixSpecItem: SubSrcConc, volumeSample: LiquidVolume, query: StateQuery): Result[Item] = {
		val src = mixSpecItem.src
		val src0 = src.head
		for {
			srcState <- query.findWellState(src0.id)
			srcConc <- srcState.content.concOfSubstance(mixSpecItem.substance)
			_ <- Result.assert(srcConc >= mixSpecItem.conc, "the concentration of the source reagent must not be higher than desired target concentration")
		} yield {
			//val srcContent = srcState.content
			val volume = volumeSample * (mixSpecItem.conc / srcConc)
			new Item(src, dest, volume)
		}
	}
	
	private def makeOne(dest: Well2, src: List[Well2], substance: Substance, conc: BigDecimal, volumeSample: LiquidVolume, query: StateQuery): Result[Item] = {
		val src0 = src.head
		for {
			srcState <- query.findWellState(src0.id)
			srcConc <- srcState.content.concOfSubstance(substance)
			_ <- Result.assert(srcConc >= conc, "the concentration of the source reagent must not be higher than desired target concentration")
		} yield {
			//val srcContent = srcState.content
			val volume = volumeSample * (conc / srcConc)
			new Item(src, dest, volume)
		}
	}*/
}