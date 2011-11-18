package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.commands.system._
import roboliq.manufacturers.biorad._


class Robolib(shared: ParserSharedData) {
	import WellPointerImplicits._
	import CmdLogImplicits._
	
	implicit def reagentSpecToPointer(o: Reagent): WellPointer = WellPointerReagent(o.reagent)
	implicit def commandToTuple(o: Command): Tuple2[Seq[Command], Seq[String]] = Seq(o) -> Seq()
	
	val kb = shared.kb
	
	def comment(s: String): Result[CmdLog] = {
		Success(CmdLog(L4C_Comment(s)))
	}
	
	def makeBioradPlateFile(
		sNamePlate: String,
		lsNameSample: List[String],
		rowcol0: Tuple2[Int, Int]
	): Result[CmdLog] = {
		BioradFunctions.makeBioradPlateFile(shared.dirLog, sNamePlate, lsNameSample.toArray, rowcol0)
		Success(CmdLog(Seq()))
	}
	
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
						new L4A_PipetteItem(WellPointer(src), WellPointer(dest), Seq(nVolume), None, None)
					})
				case Some(reagent) =>
					wvs.map(pair => {
						val (dest, nVolume) = pair
						new L4A_PipetteItem(WellPointer(reagent.reagent), WellPointer(dest), Seq(nVolume), None, None)
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
			yield new L4A_PipetteItem(reagent, dest, Seq(nVolume * nFactor / nDests), None, None)
		
		for (cmd <- PipetteCommandsL4.pipette(items)) yield {
			CmdLog(cmd)
		}
	}
	
	private type RV = Tuple2[Reagent, Double]
	private type T1 = Tuple2[Well, List[RV]]

	def prepareReactionList(l: List[T1], sLiquidClass: String, opts_? : Option[String]): Result[CmdLog] = {
		for {
			policy <- getPolicy(sLiquidClass, None)
			mapOpts <- getMapOpts(opts_?)
			mixSpec_? <- getOptMixSpec(mapOpts)
			tipOverrides_? <- getOptTipOverrides(mapOpts)
			tipModel_? <- getOptTipModel(mapOpts)
		} yield {
			val mDestToRv = HashMap(l : _*)
			val lPipetteItem = new ArrayBuffer[L4A_PipetteItem]
			while (!mDestToRv.isEmpty) {
				var bMix = false
				val l2 = l.flatMap(item => {
					val dest = item._1
					mDestToRv.get(dest) match {
						case None => Seq()
						case Some(Nil) => Seq()
						// Last reagent-volume item in dest's list
						case Some(rv :: Nil) =>
							mDestToRv.remove(dest)
							Seq(new L4A_PipetteItem(WellPointer(rv._1.reagent), dest, Seq(rv._2), None, mixSpec_?))
						case Some(rv :: rvs) =>
							mDestToRv(dest) = rvs
							Seq(new L4A_PipetteItem(WellPointer(rv._1.reagent), dest, Seq(rv._2), None, None))
					}
				})
				lPipetteItem ++= lPipetteItem
			}
			val args = new L4A_PipetteArgs(
				lPipetteItem.toSeq,
				mixSpec_? = mixSpec_?,
				tipOverrides_? = tipOverrides_?,
				pipettePolicy_? = Some(policy),
				tipModel_? = tipModel_?
				)
			CmdLog(L4C_Pipette(args))
		}
	}

	def prompt(s: String): Result[CmdLog] = {
		Success(CmdLog(L4C_Prompt(s)))
	}
	
	def serialDilution(
		diluter: Reagent,
		srcs: Seq[Well],
		dests: Seq[Well],
		nVolumeDiluter: Double,
		nVolumeSrc: Double,
		lc_? : Option[String],
		opts_? : Option[String]
	): Result[CmdLog] = {
		val srcs1 = srcs.toIndexedSeq
		val dests1 = dests.toIndexedSeq
		val nSrcs = srcs1.size
		val nDests = dests1.size
		for {
			_ <- Result.assert(nSrcs > 0, "the source list must not be empty")
			_ <- Result.assert(nDests > 0, "the destination list must not be empty")
			_ <- Result.assert((nDests % nSrcs) == 0, "the number of destination wells must be an integer multiple of the number of source wells")
			policy <- getPolicy(lc_?.getOrElse("LCWBOT"), Some(diluter))
			mapOpts <- getMapOpts(opts_?)
			mixSpec_? <- getOptMixSpec(mapOpts)
			tipOverrides_? <- getOptTipOverrides(mapOpts)
			tipModel_? <- getOptTipModel(mapOpts)
			cmdDiluter <- PipetteCommandsL4.pipette(WellPointer(diluter.reagent), WellPointer(dests), Seq(nVolumeDiluter), tipOverrides_?)
			items <- serialDilution2(srcs1, dests1, nVolumeSrc)
		} yield {
			val mixSpec = mixSpec_?.getOrElse(MixSpec((nVolumeDiluter + nVolumeSrc) / 3, 7))
			val args = new L4A_PipetteArgs(
				items,
				mixSpec_? = Some(mixSpec),
				tipOverrides_? = tipOverrides_?,
				pipettePolicy_? = Some(policy),
				tipModel_? = tipModel_?
			)
			val cmds = List(cmdDiluter, L4C_Pipette(args))
			CmdLog(cmds)
		}
	}
	
	/**
	 * For each src well, create pipette items to first transfer 
	 * liquid to the first corresponding destination well and
	 * then from that well to the next destination well.
	 * These lists are then concatenated together.
	 */
	private def serialDilution2(srcs: IndexedSeq[Well], dests: IndexedSeq[Well], nVolume: Double): Result[Seq[L4A_PipetteItem]] = {
		val nSrcs = srcs.size
		val nDilutions = dests.size / nSrcs
		val llItem = List.tabulate(nSrcs, nDilutions)((iSrc, iDilution) => {
			val src = if (iDilution == 0) srcs(iSrc) else dests(iSrc + (iDilution - 1) * nSrcs)
			val dest = if (iDilution == 0) dests(iSrc) else dests(iSrc + iDilution * nSrcs)
			new L4A_PipetteItem(src, dest, Seq(nVolume), None, None)
		})
		Success(llItem.flatten)
	}
	
	//------------------------------------------------------

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