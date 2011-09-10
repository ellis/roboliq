package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._


class ParserLineScript(shared: ParserSharedData) extends ParserBase(shared) {
	val cmds2 = Map[String, Parser[Unit]](
			("DIST_REAGENT2", idLiquid~plateWells2~valVolumes~ident~opt(word) ^^
				{ case liquid ~ wells ~ vol ~ lc ~ opts_? => run_DIST_REAGENT2(liquid, wells, vol, lc, opts_?) }),
			("MIX_WELLS", idPlate~idWells~valInt~valVolume~ident~opt(word) ^^
				{ case plate ~ wells ~ nCount ~ nVolume ~ lc ~ opts_? => run_MIX_WELLS(plate, wells, nCount, nVolume, lc, opts_?) }),
			("PROMPT", restOfLine ^^
				{ case s => run_PROMPT(s) }),
			("TRANSFER_LOCATIONS", plateWells2~plateWells2~valVolumes~ident~opt(word) ^^
				{ case srcs ~ dests ~ vol ~ lc ~ opts_? => run_TRANSFER_LOCATIONS(srcs, dests, vol, lc, opts_?) }),
			("%", restOfLine ^^
				{ case s => run_ChecklistComment(s) })
			)

	val cmds = new ArrayBuffer[RoboeaseCommand]

	def addRunCommand(cmd: Command) {
		cmds += RoboeaseCommand(shared.iLineCurrent, shared.sLineCurrent, cmd)
		println("LOG: addRunCommand: "+cmd.getClass().getCanonicalName())
	}
	
	def run_DIST_REAGENT2(reagent: Reagent, wells: Seq[Tuple2[Plate, Int]], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]) {
		sub_pipette(Some(reagent), Nil, wells, volumes, sLiquidClass, opts_?)
	}
	
	def run_MIX_WELLS(plate: Plate, wells: List[Well], nCount: Int, nVolume: Double, sLiquidClass: String, opts_? : Option[String]) {
		if (wells.isEmpty) {
			shared.addError("list of destination wells must be non-empty")
			return
		}
		
		createSrcWellLiquids(wells)
		
		val sMixClass_? = if (sLiquidClass != "DEFAULT") Some(sLiquidClass) else None 
		
		val mapOpts = getMapOpts(opts_?) match {
			case Left(sError) => shared.addError(sError); return
			case Right(map) => map
		}
		var tipOverrides_? = getOptTipOverrides(mapOpts) match {
			case Left(sError) => shared.addError(sError); return
			case Right(opt) => opt
		}
		var sTipKind_? = getOptTipKind(mapOpts)

		val mixSpec = new MixSpec(nVolume, nCount, sMixClass_?)
		val args = new L4A_MixArgs(
			wells.map(well => WPL_Well(well)),
			mixSpec,
			tipOverrides_?,
			sTipKind_?
			)
		val cmd = L4C_Mix(args)
		addRunCommand(cmd)
	}
	
	def run_PROMPT(s: String) {
		println("WARNING: PROMPT command not yet implemented")
	}
	
	def run_TRANSFER_LOCATIONS(srcs: Seq[Tuple2[Plate, Int]], dests: Seq[Tuple2[Plate, Int]], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]) {
		if (srcs.size != dests.size) {
			shared.addError("souce and destination lists must have the same length")
			return
		}
		createSrcLiquids(srcs)
		sub_pipette(None, srcs, dests, volumes, sLiquidClass, opts_?)
	}
	
	def run_ChecklistComment(s: String) {
		println("WARNING: % command not yet implemented")
	}
	
	private def getWell(pi: Tuple2[Plate, Int]): Well = {
		val (plate, iWell) = pi
		val dim = kb.getPlateSetup(plate).dim_?.get
		dim.wells(iWell)
	}
	
	private def wellsToPlateIndexes(wells: Seq[Well]): Seq[Tuple2[Plate, Int]] = {
		wells.map(well => {
			val wellSetup = kb.getWellSetup(well)
			(wellSetup.holder_?.get, wellSetup.index_?.get)
		})
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
					val reagent = new Reagent
					val reagentSetup = kb.getReagentSetup(reagent)
					reagentSetup.sName_? = Some(sLiquid)
					reagentSetup.sFamily_? = Some("ROBOEASE")
					wellSetup.reagent_? = Some(reagent)
					//wellSetup.nVolume_? = Some(0)
			}
		}
	}
	
	private def sub_pipette(reagent_? : Option[Reagent], srcs: Seq[Tuple2[Plate, Int]], dests: Seq[Tuple2[Plate, Int]], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]) {
		if (dests.isEmpty) {
			shared.addError("list of destination wells must be non-empty")
			return
		}
		if (volumes.isEmpty) {
			shared.addError("list of volumes must be non-empty")
			return
		}
		if (volumes.size > 1 && dests.size != volumes.size) {
			shared.addError("lists of wells and volumes must have the same dimensions")
			return
		}
		
		val wells2 = dests.map(getWell)
		val wvs = {
			if (volumes.size > 1)
				wells2 zip volumes
			else
				wells2.map(_ -> volumes.head)
		}
		wells2.foreach(well => kb.addWell(well, false)) // Indicate that these wells are destinations
		
		val sLiquidClass_? = {
			if (sLiquidClass == "DEFAULT") {
				reagent_? match {
					case None => None
					case Some(reagent) => kb.getReagentSetup(reagent).sFamily_?
				}
			}
			else
				Some(sLiquidClass)
		}
		
		val mapOpts = getMapOpts(opts_?) match {
			case Left(sError) => shared.addError(sError); return
			case Right(map) => map
		}
		var mixSpec_? = getOptMixSpec(mapOpts) match {
			case Left(sError) => shared.addError(sError); return
			case Right(opt) => opt
		}
		var tipOverrides_? = getOptTipOverrides(mapOpts) match {
			case Left(sError) => shared.addError(sError); return
			case Right(opt) => opt
		}
		var sTipKind_? = getOptTipKind(mapOpts)
		
		val items = reagent_? match {
			case None =>
				(srcs zip wvs).map(pair => {
					val (pi, (dest, nVolume)) = pair
					val src = getWell(pi)
					kb.addWell(src, true) // Indicate that this well is a source
					new L4A_PipetteItem(WPL_Well(src), WP_Well(dest), nVolume)
				})
			case Some(reagent) =>
				wvs.map(pair => {
					val (dest, nVolume) = pair
					new L4A_PipetteItem(WPL_Liquid(reagent), WP_Well(dest), nVolume)
				})
		}
		val args = new L4A_PipetteArgs(
			items,
			mixSpec_? = mixSpec_?,
			tipOverrides_? = tipOverrides_?,
			sAspirateClass_? = sLiquidClass_?,
			sDispenseClass_? = sLiquidClass_?,
			sTipKind_? = sTipKind_?
			)
		val cmd = L4C_Pipette(args)
		addRunCommand(cmd)
	}

	private val lsOptNames = Set("MIX", "TIPMODE", "TIPTYPE")
	
	private def getMapOpts(opts_? : Option[String]): Either[String, Map[String, Seq[String]]] = {
		opts_? match {
			case None => Right(Map())
			case Some(opts) =>
				val lsOpts = opts.split(",")
				val map = lsOpts.map(sOpt => {
					val lsParts = sOpt.split(":").toList
					val sOptName = lsParts.head
					val args = lsParts.tail.toSeq
					if (!lsOptNames.contains(sOptName))
						return Left("unknown option \""+sOptName+"\"")
					sOptName -> args.toSeq
				}).toMap
				Right(map)
		}
	}
	
	private def getOptMixSpec(mapOpts: Map[String, Seq[String]]): Either[String, Option[MixSpec]] = {
		mapOpts.get("MIX") match {
			case None => Right(None)
			case Some(args) =>
				args match {
					case Seq(lc, sCountAndVol) =>
						sCountAndVol.split("x") match {
							case Array(sCount, sVol) =>
								Right(Some(MixSpec(sVol.toDouble, sCount.toInt, Some(lc))))
							case _ =>
								Left("unrecognized MIX parameter \""+sCountAndVol+"\"")
						}
					case Seq(sCountAndVol) =>
						sCountAndVol.split("x") match {
							case Array(sCount, sVol) =>
								Right(Some(MixSpec(sVol.toDouble, sCount.toInt, None)))
							case _ =>
								Left("unrecognized MIX parameter \""+sCountAndVol+"\"")
						}
					case _ => 
						Left("unknown MIX parameters \""+args.mkString(":")+"\"")
				}
		}
	}
	
	private def getOptTipOverrides(mapOpts: Map[String, Seq[String]]): Either[String, Option[TipHandlingOverrides]] = {
		mapOpts.get("TIPMODE") match {
			case None => Right(None)
			case Some(Seq(arg)) =>
				arg match {
					case "KEEPTIP" =>
						Right(Some(new TipHandlingOverrides(Some(TipReplacementPolicy.KeepBetween), None, None, None)))
					// This is apparently like the default behavior in roboliq
					case "MULTIPIP" =>
						Right(None)
					case _ =>
						Left("unknown TIPMODE \""+arg+"\"")
				}
			case Some(args) =>
				Left("unknown TIPMODE \""+args.mkString(":")+"\"")
		}
	}
	
	private def getOptTipKind(mapOpts: Map[String, Seq[String]]): Option[String] = {
		mapOpts.get("TIPTYPE") match {
			case None => None
			case Some(args) => Some("DiTi "+args(0)+"ul")
		}
	}
	
	private def toLabel(well: Well): String = {
		kb.getWellSetup(well).sLabel_?.get
	}
}
