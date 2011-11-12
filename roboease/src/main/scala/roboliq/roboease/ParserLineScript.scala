package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.common.{Error=>RError,Success=>RSuccess}
import roboliq.commands.pipette._


class ParserLineScript(shared: ParserSharedData) extends ParserBase(shared) {
	import WellPointerImplicits._
	
	private val robolib = new Robolib(shared)
	
	val cmds2 = Map[String, Parser[Result[CmdLog]]](
			("DIST_REAGENT", idReagent~idPlate~idWells~valVolumes~ident~opt(word) ^^
				{ case liquid~_~wells~vol~lc~opts_? => run_DIST_REAGENT(liquid, wells, vol, lc, opts_?) }),
			("DIST_REAGENT2", idReagent~plateWells2~valVolumes~ident~opt(word) ^^
				{ case liquid~wells~vol~lc~opts_? => run_DIST_REAGENT(liquid, getWells(wells), vol, lc, opts_?) }),
			("DIST_WELL", idPlate~idWell~idPlate~idWells~valVolumes~ident~opt(word) ^^
				{ case _~src~_~dests~vol~lc~opts_? => run_TRANSFER_WELLS(Seq(src), dests, vol, lc, opts_?) }),
			("MIX_WELLS", idPlate~idWells~valInt~valVolume~ident~opt(word) ^^
				{ case _~wells~nCount~nVolume~lc~opts_? => run_MIX_WELLS(wells, nCount, nVolume, lc, opts_?) }),
			("PREPARE_MIX", ident~integer~opt(numDouble) ^^
				{ case id~nShots~nMargin_? => run_PREPARE_MIX(id, nShots, nMargin_?) }),
			("PROMPT", restOfLine ^^
				{ case s => run_PROMPT(s) }),
			("SERIAL_DILUTION", idReagent~idPlate~idWells~idPlate~idWells~valVolume~valVolume~opt(ident)~opt(word) ^^
				{ case diluter~_~srcs~_~dests~nVolumeDiluter~nVolumeSrc~lc_? ~opts_? => robolib.serialDilution(diluter, srcs, dests, nVolumeDiluter, nVolumeSrc, lc_?, opts_?) }),
			("TRANSFER_LOCATIONS", plateWells2~plateWells2~valVolumes~ident~opt(word) ^^
				{ case srcs~dests~vol~lc~opts_? => run_TRANSFER_WELLS(getWells(srcs), getWells(dests), vol, lc, opts_?) }),
			("TRANSFER_SAMPLES", integer~idPlate~idPlate~valVolumes~ident~opt(word) ^^
				{ case nWells~splate~dplate~vol~lc~opts_? => run_TRANSFER_SAMPLES(splate, dplate, nWells, vol, lc, opts_?) }),
			("TRANSFER_WELLS", idPlate~idWells~idPlate~idWells~valVolumes~ident~opt(word) ^^
				{ case _~srcs~_~dests~vol~lc~opts_? => run_TRANSFER_WELLS(srcs, dests, vol, lc, opts_?) }),
			("%", restOfLine ^^
				{ case s => run_ChecklistComment(s) })
			)

	//val cmds = new ArrayBuffer[RoboeaseCommand]

	def run_DIST_REAGENT(reagent: Reagent, wells: Seq[Well], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[CmdLog] = {
		robolib.pipette(Some(reagent), Nil, wells, volumes, sLiquidClass, opts_?)
	}
	
	def run_MIX_WELLS(wells: Seq[Well], nCount: Int, nVolume: Double, lc: String, opts_? : Option[String]): Result[CmdLog] = {
		for {
			_ <- createSrcWellLiquids(wells)
			cmdlog <- robolib.mix(wells, nCount, nVolume, lc, opts_?)
		} yield cmdlog
	}
	
	def run_PREPARE_MIX(id: String, nShots: Int, nMargin_? : Option[Double]): Result[CmdLog] = {
		// Default to margin of 8%
		val nMargin = nMargin_?.getOrElse(0.08)
		
		for {
			_ <- Result.assert(nShots > 0, "number of shots must be positive")
			_ <- Result.assert(nMargin >= 0 && nMargin <= 0.3, "margin value must be between 0 and 0.3; you specified "+nMargin)
			mixdef <- getMixDef(id)
			val nFactor = nShots * (1.0 + nMargin)
			res <- robolib.prepareMix(mixdef, nFactor)
		} yield res
	}
	
	def run_PROMPT(s: String): Result[CmdLog] = {
		println("WARNING: PROMPT command not yet implemented")
		RSuccess(CmdLog(Seq()))
	}
	
	def run_TRANSFER_SAMPLES(splate: Plate, dplate: Plate, nWells: Int, volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[CmdLog] = {
		for {
			srcDim <- getDim(splate)
			destDim <- getDim(dplate)
			_ <- Result.assert(nWells <= srcDim.wells.size && nWells <= destDim.wells.size, "number of samples cannot excede plate size")
			_ <- RError("Need to implement PLATE config variable first for letting the user define the first well index")
			cmdlog <- robolib.pipette(None, srcDim.wells.take(nWells), destDim.wells.take(nWells), volumes, sLiquidClass, opts_?)
		} yield cmdlog
	}
	
	def run_TRANSFER_WELLS(srcs: Seq[Well], dests: Seq[Well], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[CmdLog] = {
		for {
			_ <- validateEqualListLength(srcs, dests)
			_ <- createSrcWellLiquids(srcs)
			cmdlog <- robolib.pipette(None, srcs, dests, volumes, sLiquidClass, opts_?)
		} yield cmdlog
	}
	
	def run_ChecklistComment(s: String): Result[CmdLog] = {
		println("WARNING: % command not yet implemented")
		RSuccess(CmdLog(Seq()))
	}
	
	private def getReagent(id: String) = Result.get(shared.mapReagents.get(id), "unknown reagent \""+id+"\"")
	private def getMixDef(id: String) = Result.get(shared.mapMixDefs.get(id), "unknown mix definition \""+id+"\"")
	
	private def getDim(plate: Plate): Result[PlateSetupDimensionL4] = {
		val setup = kb.getPlateSetup(plate)
		for {
			dim <- Result.get(setup.dim_?, "Plate \""+plate+"\" requires dimensions")
		} yield dim
	}
	
	private def getWell(pi: Tuple2[Plate, Int]): Well = {
		val (plate, iWell) = pi
		val dim = kb.getPlateSetup(plate).dim_?.get
		dim.wells(iWell)
	}
	
	private def getWells(l: Seq[Tuple2[Plate, Int]]): Seq[Well] = l.map(getWell)
	
	private def wellsToPlateIndexes(wells: Seq[Well]): Seq[Tuple2[Plate, Int]] = {
		wells.map(well => {
			val wellSetup = kb.getWellSetup(well)
			(wellSetup.holder_?.get, wellSetup.index_?.get)
		})
	}
	
	private def validateEqualListLength(srcs: Seq[Well], dests: Seq[Well]): Result[Unit] = {
		Result.assert(srcs.size == dests.size, "source and destination lists must have the same length")
	}

	private def createSrcWellLiquids(wells: Seq[Well]): Result[Unit] = {
		createSrcLiquids(wellsToPlateIndexes(wells))
	}
	
	private def createSrcLiquids(srcs: Seq[Tuple2[Plate, Int]]): Result[Unit] = {
		// If source well is empty, create a new liquid for the well
		for (pi <- srcs) {
			val plate = pi._1
			val well = getWell(pi)
			val plateSetup = kb.getPlateSetup(plate)
			val wellSetup = kb.getWellSetup(well)
			wellSetup.reagent_? match {
				case Some(_) =>
				case None =>
					val sLiquid = plateSetup.sLabel_?.get + "#" + wellSetup.index_?.get
					val reagent = new roboliq.common.Reagent
					val reagentSetup = kb.getReagentSetup(reagent)
					reagentSetup.sName_? = Some(sLiquid)
					reagentSetup.sFamily_? = Some("ROBOEASE")
					wellSetup.reagent_? = Some(reagent)
					//wellSetup.nVolume_? = Some(0)
			}
		}
		RSuccess()
	}
}
