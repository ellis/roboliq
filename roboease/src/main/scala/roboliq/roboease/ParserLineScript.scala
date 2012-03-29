package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.common.{Error=>RError,Success=>RSuccess}
import roboliq.commands.pipette._


class ParserLineScript(shared: ParserSharedData) extends ParserBase(shared) {
	import WellPointerImplicits._
	
	private val robolib = new Robolib(shared)
	
	val cmds2 = Map[String, Parser[Result[CmdLog]]](
			("BIOPLATE", ident~idList~idRowCol ^^
				{ case id~lsNameSample~rowcol => robolib.makeBioradPlateFile(id, lsNameSample, rowcol) }),
			("DIST_REAGENT", idReagent~idPlate~idWells~valVolumes~ident~opt(word) ^^
				{ case liquid~_~wells~vol~lc~opts_? => run_DIST_REAGENT(liquid, wells, vol, lc, opts_?) }),
			("DIST_REAGENT2", idReagent~plateWells2~valVolumes~ident~opt(word) ^^
				{ case liquid~wells~vol~lc~opts_? => run_DIST_REAGENT(liquid, getWells(wells), vol, lc, opts_?) }),
			("DIST_WELL", idPlate~idWell~idPlate~idWells~valVolumes~ident~opt(word) ^^
				{ case _~src~_~dests~vol~lc~opts_? => run_TRANSFER_WELLS(Seq(src), dests, vol, lc, opts_?) }),
			("EXECUTE", ident ^^
				{ case cmd => robolib.execute(cmd) }),
			("MIX_WELLS", idPlate~idWells~valInt~valVolume~ident~opt(word) ^^
				{ case _~wells~nCount~nVolume~lc~opts_? => run_MIX_WELLS(wells, nCount, nVolume, lc, opts_?) }),
			("PREPARE_LIST", idList~idPlate~idWells~ident~opt(word) ^^
				{ case l~_~dests~lc~opts_? => run_PREPARE_LIST(l, dests, lc, opts_?) }),
			("PREPARE_MIX", idMixDef~integer~opt(numDouble) ^^
				{ case mixdef~nShots~nMargin_? => run_PREPARE_MIX(mixdef, nShots, nMargin_?) }),
			("PROMPT", restOfLine ^^
				{ case s => robolib.prompt(s) }),
			("REMOTE", ident ^^
				{ case cmd => robolib.remote(cmd) }),
			("SERIAL_DILUTION", idReagent~idPlate~idWells~idPlate~idWells~valVolume~valVolume~opt(ident)~opt(word) ^^
				{ case diluter~_~srcs~_~dests~nVolumeDiluter~nVolumeSrc~lc_? ~opts_? => robolib.serialDilution(diluter, srcs, dests, nVolumeDiluter, nVolumeSrc, lc_?, opts_?) }),
			("TRANSFER_LOCATIONS", plateWells2~plateWells2~valVolumes~ident~opt(word) ^^
				{ case srcs~dests~vol~lc~opts_? => run_TRANSFER_WELLS(getWells(srcs), getWells(dests), vol, lc, opts_?) }),
			("TRANSFER_SAMPLES", integer~idPlate~idPlate~valVolumes~ident~opt(word) ^^
				{ case nWells~splate~dplate~vol~lc~opts_? => run_TRANSFER_SAMPLES(splate, dplate, nWells, vol, lc, opts_?) }),
			("TRANSFER_WELLS", idPlate~idWells~idPlate~idWells~valVolumes~ident~opt(word) ^^
				{ case _~srcs~_~dests~vol~lc~opts_? => run_TRANSFER_WELLS(srcs, dests, vol, lc, opts_?) }),
			("%", restOfLine ^^
				{ case s => robolib.comment(s) }),
			("%%", restOfLine ^^
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

	// Reagent-Volume
	private type RV = Tuple2[List[Well], Double]
	private type T1 = Tuple2[Well, List[RV]]
	
	def run_PREPARE_LIST(l: List[String], dests: Seq[Well], sLiquidClass: String, opts_? : Option[String]): Result[CmdLog] = {
		for {
			// Construct List[T1] from dests and l
			lT1 <- Result.mapOver(dests.toList zip l)(prepareList_toT1)
			cmdlog <- robolib.prepareReactionList(lT1, sLiquidClass, opts_?)
		} yield cmdlog
	}
	
	private def prepareList_toT1(pair: Tuple2[Well, String]): Result[T1] = {
		val (dest, sItem) = pair
		val ls1 = sItem.split("""\s+""")
		if ((ls1.length % 2) != 0)
			return RError("list entry does must consist of valid reagent/volume pairs: "+sItem)

		def convert(array: Array[String]): Result[RV] = {
			for {
				lWell <- parseWells(array(0))
				nVolume <- toDouble(array(1))
			}
			yield lWell -> nVolume
		}
		val ls2 = ls1.grouped(2).toList
		Result.mapOver(ls2)(convert).map(dest -> _.toList)
	}
	
	/*private def prepareList_toT2(lT1: List[T1]): List[T2] = {
		def x(lT1: List[T1], lT2: List[T2]): List[T2] = {
			lT1
		}
		var lT2 = Nil
		
		val lT2 = lT1.foldLeft(T3Acc(Nil, Nil))(prepareList_foldT3).l2
	}
	
	private def prepareList_foldT3(acc: T3Acc, item: T1): T3Acc = {
		item match {
			case (_, Nil) => acc
			case (dest, rv :: Nil) => T3Acc(acc.l1, (dest, rv._1, rv._2) :: acc.l2)
			case (dest, rv :: rest) => T3Acc((dest, rest) :: acc.l1, (dest, rv._1, rv._2) :: acc.l2)
		}
	}*/
	
	def run_PREPARE_MIX(mixdef: MixDef, nShots: Int, nMargin_? : Option[Double]): Result[CmdLog] = {
		// Default to margin of 8%
		val nMargin = nMargin_?.getOrElse(0.08)
		
		for {
			_ <- Result.assert(nShots > 0, "number of shots must be positive")
			_ <- Result.assert(nMargin >= 0 && nMargin <= 0.3, "margin value must be between 0 and 0.3; you specified "+nMargin)
			val nFactor = nShots * (1.0 + nMargin)
			res <- robolib.prepareMix(mixdef, nFactor)
		} yield res
	}
	
	def run_TRANSFER_SAMPLES(splate: PlateObj, dplate: PlateObj, nWells: Int, volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[CmdLog] = {
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
		println("WARNING: %% command not yet implemented")
		RSuccess(CmdLog(Seq()))
	}
	
	private def getReagent(id: String) = Result.get(shared.mapReagents.get(id), "unknown reagent \""+id+"\"")
	private def getMixDef(id: String) = Result.get(shared.mapMixDefs.get(id), "unknown mix definition \""+id+"\"")
	
	private def getDim(plate: PlateObj): Result[PlateSetupDimensionL4] = {
		val setup = kb.getPlateSetup(plate)
		for {
			dim <- Result.get(setup.dim_?, "PlateObj \""+plate+"\" requires dimensions")
		} yield dim
	}
	
	private def wellsToPlateIndexes(wells: Seq[Well]): Seq[Tuple2[PlateObj, Int]] = {
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
	
	private def createSrcLiquids(srcs: Seq[Tuple2[PlateObj, Int]]): Result[Unit] = {
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
