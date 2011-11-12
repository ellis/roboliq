package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.common.{Error=>RError,Success=>RSuccess}
import roboliq.commands.pipette._


class ParserLineScript(shared: ParserSharedData) extends ParserBase(shared) {
	import WellPointerImplicits._
	
	private val robolib = new Robolib(shared)
	
	val cmds2 = Map[String, Parser[Result[Unit]]](
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
			("TRANSFER_LOCATIONS", plateWells2~plateWells2~valVolumes~ident~opt(word) ^^
				{ case srcs~dests~vol~lc~opts_? => run_TRANSFER_WELLS(getWells(srcs), getWells(dests), vol, lc, opts_?) }),
			("TRANSFER_SAMPLES", integer~idPlate~idPlate~valVolumes~ident~opt(word) ^^
				{ case nWells~splate~dplate~vol~lc~opts_? => run_TRANSFER_SAMPLES(splate, dplate, nWells, vol, lc, opts_?) }),
			("TRANSFER_WELLS", idPlate~idWells~idPlate~idWells~valVolumes~ident~opt(word) ^^
				{ case _~srcs~_~dests~vol~lc~opts_? => run_TRANSFER_WELLS(srcs, dests, vol, lc, opts_?) }),
			("%", restOfLine ^^
				{ case s => run_ChecklistComment(s) })
			)

	val cmds = new ArrayBuffer[RoboeaseCommand]

	def addRunCommand(cmd: Command) {
		cmds += RoboeaseCommand(shared.iLineCurrent, shared.sLineCurrent, cmd)
		println("LOG: addRunCommand: "+cmd.getClass().getCanonicalName())
	}
	
	def run_DIST_REAGENT(reagent: Reagent, wells: Seq[Well], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[Unit] = {
		sub_pipette(Some(reagent), Nil, wells, volumes, sLiquidClass, opts_?)
	}
	
	def run_MIX_WELLS(wells: Seq[Well], nCount: Int, nVolume: Double, lc: String, opts_? : Option[String]): Result[Unit] = {
		for {
			_ <- Result.assert(!wells.isEmpty, "list of destination wells must be non-empty")
			mixPolicy <- getPolicy(lc, None)
			mapOpts <- getMapOpts(opts_?)
			tipOverrides_? <- getOptTipOverrides(mapOpts)
			tipModel_? <- getOptTipModel(mapOpts)
		} yield {
			createSrcWellLiquids(wells)
			val mixSpec = new MixSpec(nVolume, nCount, Some(mixPolicy))
			val args = new L4A_MixArgs(
				wells.map(well => WellPointer(well)),
				mixSpec,
				tipOverrides_?,
				tipModel_?
				)
			val cmd = L4C_Mix(args)
			addRunCommand(cmd)
		}
	}
	
	def run_PREPARE_MIX(id: String, nShots: Int, nMargin_? : Option[Double]): Result[Unit] = {
		// Default to margin of 8%
		val nMargin = nMargin_?.getOrElse(0.08)
		
		for {
			_ <- Result.assert(nShots > 0, "number of shots must be positive")
			_ <- Result.assert(nMargin >= 0 && nMargin <= 0.3, "margin value must be between 0 and 0.3; you specified "+nMargin)
			mixdef <- getMixDef(id)
			val nFactor = nShots * (1.0 + nMargin)
			res <- robolib.prepareMix(mixdef, nFactor)
		} yield {
			res._1.foreach(addRunCommand)
		}
	}
	
	def run_PROMPT(s: String): Result[Unit] = {
		println("WARNING: PROMPT command not yet implemented")
		RSuccess()
	}
	
	def run_TRANSFER_SAMPLES(splate: Plate, dplate: Plate, nWells: Int, volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[Unit] = {
		for {
			srcDim <- getDim(splate)
			destDim <- getDim(dplate)
			_ <- Result.assert(nWells <= srcDim.wells.size && nWells <= destDim.wells.size, "number of samples cannot excede plate size")
			_ <- RError("Need to implement PLATE config variable first for letting the user define the first well index")
		} yield {
			sub_pipette(None, srcDim.wells.take(nWells), destDim.wells.take(nWells), volumes, sLiquidClass, opts_?)
		}
	}
	
	def run_TRANSFER_WELLS(srcs: Seq[Well], dests: Seq[Well], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[Unit] = {
		for {
			_ <- validateEqualListLength(srcs, dests)
		} yield {
			createSrcWellLiquids(srcs)
			sub_pipette(None, srcs, dests, volumes, sLiquidClass, opts_?)
		}
	}
	
	def run_ChecklistComment(s: String): Result[Unit] = {
		println("WARNING: % command not yet implemented")
		RSuccess()
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

	private def createSrcWellLiquids(wells: Seq[Well]) {
		createSrcLiquids(wellsToPlateIndexes(wells))
	}
	
	private def createSrcLiquids(srcs: Seq[Tuple2[Plate, Int]]) {
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
	}
	
	private def sub_pipette(reagent_? : Option[Reagent], srcs: Seq[Well], dests: Seq[Well], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]): Result[Unit] = {
		for {
			_ <- Result.assert(!dests.isEmpty, "list of destination wells must be non-empty")
			_ <- Result.assert(!volumes.isEmpty, "list of volumes must be non-empty")
			_ <- Result.assert(volumes.size == 1 || dests.size == volumes.size, "lists of wells and volumes must have the same dimensions")
			policy <- getPolicy(sLiquidClass, reagent_?)
			mapOpts <- getMapOpts(opts_?)
			mixSpec_? <- getOptMixSpec(mapOpts)
			tipOverrides_? <- getOptTipOverrides(mapOpts)
			tipModel_? <- getOptTipModel(mapOpts)
		} yield {
			val wvs = {
				if (volumes.size > 1)
					dests zip volumes
				else
					dests.map(_ -> volumes.head)
			}
			dests.foreach(well => kb.addWell(well, false)) // Indicate that these wells are destinations
			
			val items = reagent_? match {
				case None =>
					(srcs zip wvs).map(pair => {
						val (src, (dest, nVolume)) = pair
						kb.addWell(src, true) // Indicate that this well is a source
						new L4A_PipetteItem(WellPointer(src), WellPointer(dest), Seq(nVolume))
					})
				case Some(reagent) =>
					wvs.map(pair => {
						val (dest, nVolume) = pair
						new L4A_PipetteItem(WellPointer(reagent.reagent), WellPointer(dest), Seq(nVolume))
					})
			}
			val args = new L4A_PipetteArgs(
				items,
				mixSpec_? = mixSpec_?,
				tipOverrides_? = tipOverrides_?,
				pipettePolicy_? = Some(policy),
				tipModel_? = tipModel_?
				)
			val cmd = L4C_Pipette(args)
			addRunCommand(cmd)
		}
	}

	private def getPolicy(lc: String, reagent_? : Option[Reagent]): Result[PipettePolicy] = {
		if (lc == "DEFAULT") {
			for {
				reagent <- Result.get(reagent_?, "explicit liquid class required here instead of \"DEFAULT\"")
			} yield reagent.policy
		}
		else {
			shared.getPipettePolicy(lc)
		}
	}

	private val lsOptNames = Set("MIX", "TIPMODE", "TIPTYPE")
	
	private def getMapOpts(opts_? : Option[String]): Result[Map[String, Seq[String]]] = {
		opts_? match {
			case None => RSuccess(Map())
			case Some(opts) =>
				val lsOpts = opts.split(",")
				val map = lsOpts.map(sOpt => {
					val lsParts = sOpt.split(":").toList
					val sOptName = lsParts.head
					val args = lsParts.tail.toSeq
					if (!lsOptNames.contains(sOptName))
						return RError("unknown option \""+sOptName+"\"")
					sOptName -> args.toSeq
				}).toMap
				RSuccess(map)
		}
	}
	
	private def getOptMixSpec(mapOpts: Map[String, Seq[String]]): Result[Option[MixSpec]] = {
		mapOpts.get("MIX") match {
			case None => RSuccess(None)
			case Some(args) =>
				args match {
					case Seq(lc, sCountAndVol) =>
						sCountAndVol.split("x") match {
							case Array(sCount, sVol) =>
								for (policy <- getPolicy(lc, None)) yield {
									Some(MixSpec(sVol.toDouble, sCount.toInt, Some(policy)))
								}
							case _ =>
								RError("unrecognized MIX parameter \""+sCountAndVol+"\"")
						}
					case Seq(sCountAndVol) =>
						sCountAndVol.split("x") match {
							case Array(sCount, sVol) =>
								RSuccess(Some(MixSpec(sVol.toDouble, sCount.toInt, None)))
							case _ =>
								RError("unrecognized MIX parameter \""+sCountAndVol+"\"")
						}
					case _ => 
						RError("unknown MIX parameters \""+args.mkString(":")+"\"")
				}
		}
	}
	
	private def getOptTipOverrides(mapOpts: Map[String, Seq[String]]): Result[Option[TipHandlingOverrides]] = {
		mapOpts.get("TIPMODE") match {
			case None => RSuccess(None)
			case Some(Seq(arg)) =>
				arg match {
					case "KEEPTIP" =>
						RSuccess(Some(new TipHandlingOverrides(Some(TipReplacementPolicy.KeepBetween), None, None, None)))
					// NOTE: "KEEPTIP" == "KEEPTIPS"
					case "KEEPTIPS" =>
						RSuccess(Some(new TipHandlingOverrides(Some(TipReplacementPolicy.KeepBetween), None, None, None)))
					// This is apparently like the default behavior in roboliq
					case "MULTIPIP" =>
						RSuccess(None)
					case _ =>
						RError("unknown TIPMODE \""+arg+"\"")
				}
			case Some(args) =>
				RError("unknown TIPMODE \""+args.mkString(":")+"\"")
		}
	}
	
	private def getOptTipModel(mapOpts: Map[String, Seq[String]]): Result[Option[TipModel]] = {
		mapOpts.get("TIPTYPE") match {
			case None => RSuccess(None)
			case Some(Seq(sType)) =>
				shared.mapTipModel.get(sType) match {
					case None => RError("unregister TIPTYPE: \""+sType+"\"")
					case Some(model) => RSuccess(Some(model))
				}
			case Some(args) =>
				RError("unrecognized TIPTYPE arguments: "+args.mkString(":"))
		}
	}
	
	private def toLabel(well: Well): String = {
		kb.getWellSetup(well).sLabel_?.get
	}
}
