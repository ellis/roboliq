package roboliq.commands.pipette.scheduler

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet
import roboliq.core._
import roboliq.commands.pipette.PipetteCmdBean


case class L3C_Pipette(args: PipetteCmd) {
	def toDebugString = {
		val srcs = args.items.groupBy(_.srcs).keys
		val sDests = Printer.getWellsDebugString(args.items.map(_.dest))
		val sVolumes = Printer.getSeqDebugString(args.items.map(_.volume))
		if (srcs.size == 1) {
			val sSrcs = Printer.getWellsDebugString(srcs.head)
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else if (args.items.forall(_.srcs.size == 1)) {
			val sSrcs = Printer.getWellsDebugString(args.items.map(_.srcs.head))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else {
			val lsSrcs = args.items.map(item => Printer.getWellsDebugString(item.srcs))
			val sSrcs = Printer.getSeqDebugString(lsSrcs)
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
	}
	
	def toDocString(ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All lists of sources
		val llSrc = args.items.map(_.srcs)
		// First source of each item
		val lSrc0 = llSrc.map(_.head)
		
		val src_? : Option[Well] = {
			val lSrcDistinct = lSrc0.distinct
			if (lSrcDistinct.size == 1)
				Some(lSrcDistinct.head)
			else
				None
		}
		
		//val bShortSrcs
		val sSrcs = src_? match {
			case Some(src) => src.id
			case None =>
				def step(llSrc: Seq[SortedSet[Well]], accR: List[String]): String = {
					if (llSrc.isEmpty)
						accR.reverse.mkString(", ")
					else if (llSrc.head.size > 1)
						step(llSrc.tail, getWellsString(llSrc.head) :: accR)
					else {
						val (x, rest) = llSrc.span(_.size == 1)
						val lSrc = x.map(_.head).toList
						step(rest, getWellsString(lSrc) :: accR)
					}
				}
				step(llSrc, Nil)
		}
		
		val lLiquid = lSrc0.toList.flatMap(_.wellState(states).map(_.liquid).toOption)
		val liquid_? : Option[Liquid] = {
			lLiquid.distinct match {
				case liquid :: Nil => Some(liquid)
				case _ => None
			}
		}
		
		val sLiquids_? = liquid_? match {
			case Some(liquid) => Some(liquid.id)
			case _ =>
				val lsLiquid = lLiquid.map(_.id)
				val lsLiquid2 = lsLiquid.distinct
				if (lsLiquid2.isEmpty)
					None
				else {
					val sLiquids = {
						if (lsLiquid2.size <= 3)
							lsLiquid2.mkString(", ")
						else
							List(lsLiquid2.head, "...", lsLiquid2.last).mkString(", ")
					}
					Some(sLiquids)
				}
		}
		
		val lVolume = args.items.map(_.volume)
		val volume_? : Option[LiquidVolume] = lVolume.distinct match {
			case volume :: Nil => Some(volume)
			case _ => None
		}
		val sVolumes_? = volume_? match {
			case Some(volume) => Some(volume.toString())
			case _ => None
		}

		val sVolumesAndLiquids = List(sVolumes_?, sLiquids_?).flatten.mkString(" of ")
		
		val sDests = getWellsString(args.items.map(_.dest))
				//Printer.getWellsDebugString(args.items.map(_.dest))
		val sVolumes = Printer.getSeqDebugString(args.items.map(_.volume))
		
		val doc = List("Pipette", sVolumesAndLiquids, "from", sSrcs, "to", sDests).filterNot(_.isEmpty).mkString(" ") 
		(doc, null)
	}
}

class PipetteCmd(
	val items: Seq[Item],
	val mixSpec_? : Option[MixSpec] = None,
	val tipOverrides_? : Option[TipHandlingOverrides] = None,
	val pipettePolicy_? : Option[PipettePolicy] = None,
	val tipModel_? : Option[TipModel] = None
)

object PipetteCmd {
	def fromBean(cmd: PipetteCmdBean, query: StateQuery): Result[PipetteCmd] = {

		def opt[A](id: String, fn: String => Result[A]): Result[Option[A]] = {
			if (id == null) {
				Success(None)
			}
			else {
				for {res <- fn(id)} yield Some(res)
			}
		}
		
		def zipit(
			ls: List[List[Well]],
			ld: List[Well],
			lv: List[LiquidVolume],
			acc: List[Tuple3[List[Well], Well, LiquidVolume]]
		): List[Tuple3[List[Well], Well, LiquidVolume]] = {
			//println("zipit:", ls, ld, lv)
			val (s::ss, d::ds, v::vs) = (ls, ld, lv)
			val sdv = (s, d, v)
			val acc2 = sdv :: acc
			if (ss == Nil && ds == Nil && vs == Nil)
				acc2.reverse
			else {
				val ls2 = if (ss.isEmpty) ls else ss
				val ld2 = if (ds.isEmpty) ld else ds
				val lv2 = if (vs.isEmpty) lv else vs
				zipit(ls2, ld2, lv2, acc2)
			}
		}
		
		val mixSpec_? : Option[MixSpec] = if (cmd.postmix == null) None else Some(MixSpec.fromBean(cmd.postmix))
		val pipettePolicy_? : Option[PipettePolicy] = if (cmd.policy == null) None else Some(PipettePolicy.fromName(cmd.policy))
		val volumes_? : Option[List[LiquidVolume]] = if (cmd.volume == null) None else Some(cmd.volume.map(n => LiquidVolume.l(n)).toList)
		val tipOverrides_? = Some(new TipHandlingOverrides(
			replacement_? = if (cmd.tipReplacement == null) None else Some(TipReplacementPolicy.withName(cmd.tipReplacement)),
			washIntensity_? = None,
			allowMultipipette_? = if (cmd.allowMultipipette == null) None else Some(cmd.allowMultipipette),
			contamInside_? = None,
			contamOutside_? = None
		))
		println("tipOverrides_?: "+tipOverrides_?)

		for {
			tipModel_? <- opt(cmd.tipModel, query.findTipModel _)
			srcs_? <- opt(cmd.src, query.mapIdsToWell2Lists _)
			// TODO: disallow liquids in destination
			dests_? <- opt(cmd.dest, query.mapIdsToWell2Lists _)
		} yield {
			// If only one entry is given, use it as the default
			val srcDefault_? = srcs_?.filter(_.tail.isEmpty).map(_.head)
			val destDefault_? = dests_?.filter(_.tail.isEmpty).map(_.head)
			val volumeDefault_? = volumes_?.filter(_.tail.isEmpty).map(_.head)
			
			val lnLen = List(
				srcs_?.map(_.length).getOrElse(0),
				dests_?.map(_.length).getOrElse(0),
				volumes_?.map(_.length).getOrElse(0)
			)
			
			val bLengthsOk = lnLen.filter(_ != 1) match {
				case Nil => true
				case x :: xs => xs.forall(_ == x)
			}
			
			if (!bLengthsOk) {
				return Error("arrays must have equal lengths")
			}
		
			val lsdv = zipit(srcs_?.get, dests_?.get.flatten, volumes_?.get, Nil)
			
			val items = lsdv.map(svd => new Item(
				SortedSet(svd._1 : _*),
				svd._2,
				//volumes_?.get(0),
				svd._3,
				None,
				None
			))

			new PipetteCmd(items, mixSpec_?, tipOverrides_?, pipettePolicy_?, tipModel_?)
		}
	}
}

case class Item(
	val srcs: SortedSet[Well],
	val dest: Well,
	val volume: LiquidVolume,
	val premix_? : Option[MixSpec],
	val postmix_? : Option[MixSpec]
)

object Item {
	def toDebugString(items: Seq[Item]): String = {
		val srcs = items.groupBy(_.srcs).keys
		if (srcs.size == 1) {
			val sSrcs = Printer.getWellsDebugString(srcs.head)
			val sDests = Printer.getWellsDebugString(items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(items.map(_.volume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else if (items.forall(_.srcs.size == 1)) {
			val sSrcs = Printer.getWellsDebugString(items.map(_.srcs.head))
			val sDests = Printer.getWellsDebugString(items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(items.map(_.volume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
		else {
			val lsSrcs = items.map(item => Printer.getWellsDebugString(item.srcs))
			val sSrcs = Printer.getSeqDebugString(lsSrcs)
			val sDests = Printer.getWellsDebugString(items.map(_.dest))
			val sVolumes = Printer.getSeqDebugString(items.map(_.volume))
			getClass().getSimpleName() + List(sSrcs, sDests, sVolumes).mkString("(", ", ", ")")
		}
	}

	def toDebugString(item: Item): String = toDebugString(Seq(item))
}
