package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._


class Robolib(shared: ParserSharedData) {
	import WellPointerImplicits._
	import CmdLogImplicits._
	
	implicit def reagentSpecToPointer(o: Reagent): WellPointer = WellPointerReagent(o.reagent)
	implicit def commandToTuple(o: Command): Tuple2[Seq[Command], Seq[String]] = Seq(o) -> Seq()
	
	val kb = shared.kb
	
	def mix(
		wells: Seq[Well],
		nCount: Int,
		nVolume: Double,
		sLiquidClass: String,
		opts_? : Option[String]
	): Result[CmdLog] = {
		for {
			_ <- Result.assert(!wells.isEmpty, "list of destination wells must be non-empty")
			_ <- Result.assert(nCount >= 0, "mix count must be >= 0")
			_ <- Result.assert(nVolume >= 0, "mix volume must be >= 0")
			mixPolicy <- getPolicy(sLiquidClass, None)
			mapOpts <- getMapOpts(opts_?)
			tipOverrides_? <- getOptTipOverrides(mapOpts)
			tipModel_? <- getOptTipModel(mapOpts)
		} yield {
			val mixSpec = new MixSpec(nVolume, nCount, Some(mixPolicy))
			val args = new L4A_MixArgs(
				wells.map(well => WellPointer(well)),
				mixSpec,
				tipOverrides_?,
				tipModel_?
				)
			CmdLog(L4C_Mix(args))
		}
	}
	
	def pipette(
		reagent_? : Option[Reagent],
		srcs: Seq[Well],
		dests: Seq[Well],
		volumes: Seq[Double],
		sLiquidClass: String,
		opts_? : Option[String]
	): Result[CmdLog] = {
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
			CmdLog(cmd)
		}
	}

	def prepareMix(mixdef: MixDef, nFactor: Double): Result[CmdLog] = {
		val nDests = mixdef.reagent.wells.size
		val dests = mixdef.reagent.wells.toSeq
		
		val items =
			for ((reagent, nVolume) <- mixdef.items; dest <- dests)
			yield new L4A_PipetteItem(reagent, dest, Seq(nVolume * nFactor / nDests))
		
		for (cmd <- PipetteCommandsL4.pipette(items)) yield {
			// TODO: add log?
			CmdLog(cmd)
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
			case None => Success(Map())
			case Some(opts) =>
				val lsOpts = opts.split(",")
				val map = lsOpts.map(sOpt => {
					val lsParts = sOpt.split(":").toList
					val sOptName = lsParts.head
					val args = lsParts.tail.toSeq
					if (!lsOptNames.contains(sOptName))
						return Error("unknown option \""+sOptName+"\"")
					sOptName -> args.toSeq
				}).toMap
				Success(map)
		}
	}
	
	private def getOptMixSpec(mapOpts: Map[String, Seq[String]]): Result[Option[MixSpec]] = {
		mapOpts.get("MIX") match {
			case None => Success(None)
			case Some(args) =>
				args match {
					case Seq(lc, sCountAndVol) =>
						sCountAndVol.split("x") match {
							case Array(sCount, sVol) =>
								for (policy <- getPolicy(lc, None)) yield {
									Some(MixSpec(sVol.toDouble, sCount.toInt, Some(policy)))
								}
							case _ =>
								Error("unrecognized MIX parameter \""+sCountAndVol+"\"")
						}
					case Seq(sCountAndVol) =>
						sCountAndVol.split("x") match {
							case Array(sCount, sVol) =>
								Success(Some(MixSpec(sVol.toDouble, sCount.toInt, None)))
							case _ =>
								Error("unrecognized MIX parameter \""+sCountAndVol+"\"")
						}
					case _ => 
						Error("unknown MIX parameters \""+args.mkString(":")+"\"")
				}
		}
	}
	
	private def getOptTipOverrides(mapOpts: Map[String, Seq[String]]): Result[Option[TipHandlingOverrides]] = {
		mapOpts.get("TIPMODE") match {
			case None => Success(None)
			case Some(Seq(arg)) =>
				arg match {
					case "KEEPTIP" =>
						Success(Some(new TipHandlingOverrides(Some(TipReplacementPolicy.KeepBetween), None, None, None)))
					// NOTE: "KEEPTIP" == "KEEPTIPS"
					case "KEEPTIPS" =>
						Success(Some(new TipHandlingOverrides(Some(TipReplacementPolicy.KeepBetween), None, None, None)))
					// This is apparently like the default behavior in roboliq
					case "MULTIPIP" =>
						Success(None)
					case _ =>
						Error("unknown TIPMODE \""+arg+"\"")
				}
			case Some(args) =>
				Error("unknown TIPMODE \""+args.mkString(":")+"\"")
		}
	}
	
	private def getOptTipModel(mapOpts: Map[String, Seq[String]]): Result[Option[TipModel]] = {
		mapOpts.get("TIPTYPE") match {
			case None => Success(None)
			case Some(Seq(sType)) =>
				shared.mapTipModel.get(sType) match {
					case None => Error("unregister TIPTYPE: \""+sType+"\"")
					case Some(model) => Success(Some(model))
				}
			case Some(args) =>
				Error("unrecognized TIPTYPE arguments: "+args.mkString(":"))
		}
	}
	
	private def toLabel(well: Well): String = {
		shared.kb.getWellSetup(well).sLabel_?.get
	}
}