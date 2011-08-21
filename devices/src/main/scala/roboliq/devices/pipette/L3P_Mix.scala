package roboliq.devices.pipette

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L3P_Mix extends CommandCompilerL3 {
	type CmdType = L3C_Mix
	val cmdType = classOf[CmdType]
	
	def addKnowledge(kb: KnowledgeBase, _cmd: Command) {
		val cmd = _cmd.asInstanceOf[CmdType]
		// Add destinations to KB
		cmd.dests.foreach(_ match {
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
		val dests = Set(cmd.dests : _*)
		if (dests.size == 0)
			return ("must have one or more destinations") :: Nil
		
		// Check validity of source/dest pairs
		val destsAlready = new HashSet[Obj]
		for (dest <- cmd.dests) {
			val destObjs = dest match {
				case WP_Well(well) => Seq(well) 
				case WP_Plate(plate) => Seq(plate) ++ getWells(kb, plate)
			}
			if (destObjs.exists(destsAlready.contains))
				return ("each destination may only be used once") :: Nil
			destsAlready ++= destObjs
		}
		
		if (cmd.mixSpec.nVolume <= 0)
			return ("volume must be > 0") :: Nil
		if (cmd.mixSpec.nCount <= 0)
			return ("count must be > 0") :: Nil
		
		Nil
	}
	
	def translate(map31: ObjMapper, cmd: CmdType): Either[String, Command] = {
		val destConfs = new ArrayBuffer[WellConfigL1]
		val bAllOk = cmd.dests.forall(dest => {
			val confs = getWells1(map31, dest)
			if (confs.isEmpty) {
				false
			}
			else {
				destConfs ++= confs
				true
			}
		})
		
		//println(items2)
		
		if (bAllOk) {
			Right(L2C_Mix(new L2A_MixArgs(destConfs.toSet, cmd.mixSpec)))
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
