package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._


class ParserLineScript(shared: ParserSharedData) extends ParserBase(shared) {
	val cmds2 = Map[String, Parser[Unit]](
			("DIST_REAGENT2", idLiquid~plateWells2~valVolumes~ident~opt(word) ^^
				{ case liquid ~ wells ~ vol ~ lc ~ opts_? => run_DIST_REAGENT2(liquid, wells, vol, lc, opts_?) }),
			("MIX_WELLS", idPlate~idWells~valInt~valVolumes~ident~opt(word) ^^
				{ case plate ~ wells ~ nCount ~ lnVolume ~ lc ~ opts_? => run_MIX_WELLS(plate, wells, nCount, lnVolume, lc, opts_?) }),
			("PROMPT", restOfLine ^^
				{ case s => run_PROMPT(s) })
			)

	val cmds = new ArrayBuffer[RoboeaseCommand]

	def addRunCommand(cmd: Command) {
		cmds += RoboeaseCommand(shared.iLineCurrent, shared.sLineCurrent, cmd)
		println("LOG: addRunCommand: "+cmd.getClass().getCanonicalName())
	}
	
	def run_DIST_REAGENT2(liq: Liquid, wells: Seq[Tuple2[Plate, Int]], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]) {
		if (wells.isEmpty) {
			shared.addError("list of destination wells must be non-empty")
			return
		}
		if (volumes.isEmpty) {
			shared.addError("list of volumes must be non-empty")
			return
		}
		if (volumes.size > 1 && wells.size != volumes.size) {
			shared.addError("lists of wells and volumes must have the same dimensions")
			return
		}
		
		val wells2 = wells.map(pair => {
			val (plate, iWell) = pair
			val dim = kb.getPlateSetup(plate).dim_?.get
			dim.wells(iWell)
		})
		val wvs = {
			if (volumes.size > 1)
				wells2 zip volumes
			else
				wells2.map(_ -> volumes.head)
		}
		
		val sLiquidClass_? = {
			if (sLiquidClass == "DEFAULT")
				shared.mapDefaultLiquidClass.get(liq)
			else
				Some(sLiquidClass)
		}
		
		val items = wvs.map(pair => {
			val (well, nVolume) = pair
			new L4A_PipetteItem(WPL_Liquid(liq), WP_Well(well), nVolume)
		})
		val args = new L4A_PipetteArgs(
			items,
			sAspirateClass_? = sLiquidClass_?,
			sDispenseClass_? = sLiquidClass_?
			)
		val cmd = L4C_Pipette(args)
		addRunCommand(cmd)
	}
	
	def run_MIX_WELLS(plate: Plate, wells: List[Well], nCount: Int, lnVolume: List[Double], sLiquidClass: String, opts_? : Option[String]) {
		if (wells.isEmpty) {
			shared.addError("list of destination wells must be non-empty")
			return
		}
		if (lnVolume.isEmpty) {
			shared.addError("list of volumes must be non-empty")
			return
		}
		if (lnVolume.size > 1 && wells.size != lnVolume.size) {
			shared.addError("lists of wells and volumes must have the same dimensions")
			return
		}
		
		val wvs = {
			if (lnVolume.size > 1)
				wells zip lnVolume
			else
				wells.map(_ -> lnVolume.head)
		}
		
		val sLiquidClass_? = if (sLiquidClass != "DEFAULT") Some(sLiquidClass) else None 
		
		val items = wvs.map(pair => {
			val (well, nVolume) = pair
			new L4A_MixItem(WPL_Well(well), nVolume)
		})
		val args = new L4A_MixArgs(
			items,
			nCount = nCount,
			sMixClass_? = sLiquidClass_?
			)
		val cmd = L4C_Mix(args)
		//addRunCommand(cmd)
	}
	
	def run_PROMPT(s: String) {
		println("WARNING: PROMPT command not yet implemented")
	}
	
	private def toLabel(well: Well): String = {
		kb.getWellSetup(well).sLabel_?.get
	}
}
