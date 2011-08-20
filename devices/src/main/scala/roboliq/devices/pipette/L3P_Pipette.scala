package roboliq.devices.pipette

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L3P_Pipette extends CommandCompilerL3 {
	type CmdType = L3C_Pipette
	val cmdType = classOf[CmdType]
	
	def addKnowledge(kb: KnowledgeBase, _cmd: Command) {
		val cmd = _cmd.asInstanceOf[L3C_Pipette]
		// Add sources to KB
		cmd.items.foreach(_.src match {
			case WPL_Well(o) => kb.addWell(o, true)
			case WPL_Plate(o) => kb.addPlate(o, true)
			case WPL_Liquid(o) => kb.addLiquid(o)
		})
		// Add destinations to KB
		cmd.items.foreach(_.dest match {
			case WP_Well(o) => kb.addWell(o, false)
			case WP_Plate(o) => kb.addPlate(o, false)
		})
	}
	
	def compileL3(ctx: CompilerContextL3, _cmd: Command): CompileResult = {
		val cmd = _cmd.asInstanceOf[CmdType]
		val errors = checkParams(ctx.kb, cmd)
		if (!errors.isEmpty)
			return CompileError(cmd, errors)
		
		translate(ctx.map31, cmd) match {
			case Right(translation) => CompileTranslation(cmd, Seq(translation))
			case Left(errors) => CompileError(cmd, Seq(errors))
		}
	}
	
	private def checkParams(kb: KnowledgeBase, cmd: CmdType): Seq[String] = {
		val items = cmd.items
		val srcs = Set() ++ items.map(_.src)
		if (srcs.size == 0)
			return ("must have one or more sources") :: Nil

		val dests = Set() ++ items.map(_.dest)
		if (dests.size == 0)
			return ("must have one or more destinations") :: Nil
		
		// Check validity of source/dest pairs
		val destsAlready = new HashSet[Obj]
		for (item <- items) {
			val destObjs = item.dest match {
				case WP_Well(well) => Seq(well) 
				case WP_Plate(plate) => Seq(plate) ++ getWells(kb, plate)
			}
			if (destObjs.exists(destsAlready.contains))
				return ("each destination may only be used once") :: Nil
			destsAlready ++= destObjs
			// If the source is a plate, the destination must be too
			item.src match {
				case WPL_Plate(plate1) =>
					item.dest match {
						case WP_Plate(plate2) =>
							(kb.getPlateSetup(plate1).dim_?, kb.getPlateSetup(plate2).dim_?) match {
								case (Some(dim1), Some(dim2)) =>
									if (dim1.wells.size != dim2.wells.size)
										return ("source and destination plates must have the same number of wells") :: Nil
								case _ =>
							}
						case _ =>
							return ("when source is a plate, destination must also be a plate") :: Nil
					}
				case _ =>
			}
			
			if (item.nVolume <= 0)
				return ("volume must be > 0") :: Nil
		}
		
		Nil
	}
	
	def translate(map31: ObjMapper, cmd: CmdType): Either[String, Command] = {
		val items2 = new ArrayBuffer[L2A_PipetteItem]
		val bAllOk = cmd.items.forall(item => {
			val srcWells1 = getWells1(map31, item.src)
			val destWells1 = getWells1(map31, item.dest)
			if (srcWells1.isEmpty || destWells1.isEmpty) {
				false
			}
			else {
				items2 ++= destWells1.map(dest1 => new L2A_PipetteItem(srcWells1, dest1, item.nVolume))
				true
			}
		})
		
		println(items2)
		
		if (bAllOk) {
			val args = new L2A_PipetteArgs(
					items2,
					cmd.mixSpec_?,
					sAspirateClass_? = None,
					sDispenseClass_? = None,
					sMixClass_? = None,
					sTipKind_? = None,
					fnClean_? = None)
			Right(L2C_Pipette(args))
		}
		else {
			Left("missing well")
		}
	}
	
	/*
	private def getWells(well: Well): Set[Well] = Set(well)
	private def getWells(plate: Plate): Set[Well] = kb.getPlateData(plate).wells.get_? match {
		case None => Set()
		case Some(wells) => wells.toSet
	}
	private def getWells(liquid: Liquid): Set[Well] = kb.getLiqWells(liquid)
	private def getWells(wpl: WellOrPlateOrLiquid): Set[Well] = wpl match {
		case WPL_Well(o) => getWells(o)
		case WPL_Plate(o) => getWells(o)
		case WPL_Liquid(o) => getWells(o)
	}			
	private def getWells(wp: WellOrPlate): Set[Well] = wp match {
		case WP_Well(o) => getWells(o)
		case WP_Plate(o) => getWells(o)
	}
	
	private def getWells1(map31: ObjMapper, wells3: Set[Well]): Set[WellConfigL1] = {
		if (wells3.forall(kb.map31.contains)) {
			wells3.map(well3 => kb.map31(well3).asInstanceOf[roboliq.parts.Well])
		}
		else {
			Set()
		}
	}
	*/
	private def getWells(kb: KnowledgeBase, plate: Plate): Set[Well] = kb.getPlateSetup(plate).dim_? match {
		case None => Set()
		case Some(dim) => dim.wells.toSet
	}
	
	private def getWells1(map31: ObjMapper, wpl: WellOrPlateOrLiquid): Set[WellConfigL1] = wpl match {
		case WPL_Well(o) => getWells1(map31, o)
		case WPL_Plate(o) => getWells1(map31, o)
		case WPL_Liquid(o) => getWells1(map31, o)
	}			

	private def getWells1(map31: ObjMapper, wpl: WellOrPlate): Set[WellConfigL1] = wpl match {
		case WP_Well(o) => getWells1(map31, o)
		case WP_Plate(o) => getWells1(map31, o)
	}			

	private def getWells1(map31: ObjMapper, well: Well): Set[WellConfigL1] = well.getConfigL1(map31) match {
		case Some(c) => Set(c)
		case None => Set()
	}

	private def getWells1(map31: ObjMapper, plate: Plate): Set[WellConfigL1] = plate.getConfigL1(map31) match {
		case Some(c) =>
			val wells_? = c.wells.map(_.getConfigL1(map31))
			if (wells_?.forall(_.isDefined))
				wells_?.map(_.get).toSet
			else
				Set()
		case None =>
			Set()
	}
	
	private def getWells1(map31: ObjMapper, liquid: Liquid): Set[WellConfigL1] = {
		// Only keep wells with the given initial liquid
		val l1 = map31.map.filter(pair => {
			val sts = pair._2
			sts.state0 match {
				case state0: WellStateL1 => (state0.liquid eq liquid)
				case _ => false
			}
		})
		l1.map(_._2.config.asInstanceOf[WellConfigL1]).toSet
	}
}
