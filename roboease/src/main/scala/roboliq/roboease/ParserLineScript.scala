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
				{ case srcs ~ dests ~ vol ~ lc ~ opts_? => run_TRANSFER_LOCATIONS(srcs, dests, vol, lc, opts_?) })
			)

	val cmds = new ArrayBuffer[RoboeaseCommand]

	def addRunCommand(cmd: Command) {
		cmds += RoboeaseCommand(shared.iLineCurrent, shared.sLineCurrent, cmd)
		println("LOG: addRunCommand: "+cmd.getClass().getCanonicalName())
	}
	
	def run_DIST_REAGENT2(liq: Liquid, wells: Seq[Tuple2[Plate, Int]], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]) {
		sub_pipette(Some(liq), Nil, wells, volumes, sLiquidClass, opts_?)
	}
	
	def run_MIX_WELLS(plate: Plate, wells: List[Well], nCount: Int, nVolume: Double, sLiquidClass: String, opts_? : Option[String]) {
		if (wells.isEmpty) {
			shared.addError("list of destination wells must be non-empty")
			return
		}
		
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
			shared.addError("lists of souces and destinations have the same dimensions")
			return
		}
		// If source well is empty, create a new liquid for the well
		for (pi <- srcs) {
			val well = getWell(pi)
			val plateSetup = kb.getPlateSetup(pi._1)
			val wellSetup = kb.getWellSetup(well)
			wellSetup.liquid_? match {
				case Some(_) =>
				case None =>
					val sLiquid = plateSetup.sLabel_?.get + "#" + wellSetup.index_?.get
					val liquid = new Liquid(sLiquid, false, true, Set())
					wellSetup.liquid_? = Some(liquid)
					wellSetup.nVolume_? = Some(0)
			}
		}
		sub_pipette(None, srcs, dests, volumes, sLiquidClass, opts_?)
	}
	
	private def getWell(pi: Tuple2[Plate, Int]): Well = {
		val (plate, iWell) = pi
		val dim = kb.getPlateSetup(plate).dim_?.get
		dim.wells(iWell)
	}
	
	private def sub_pipette(liq_? : Option[Liquid], srcs: Seq[Tuple2[Plate, Int]], dests: Seq[Tuple2[Plate, Int]], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]) {
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
			if (sLiquidClass == "DEFAULT")
				None
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
		
		val items = liq_? match {
			case None =>
				(srcs zip wvs).map(pair => {
					val (pi, (dest, nVolume)) = pair
					val src = getWell(pi)
					kb.addWell(src, true) // Indicate that this well is a source
					new L4A_PipetteItem(WPL_Well(src), WP_Well(dest), nVolume)
				})
			case Some(liq) =>
				wvs.map(pair => {
					val (dest, nVolume) = pair
					new L4A_PipetteItem(WPL_Liquid(liq), WP_Well(dest), nVolume)
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
