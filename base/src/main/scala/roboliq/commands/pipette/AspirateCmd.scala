package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class AspirateCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class AspirateCmdHandler extends CmdHandlerA[AspirateCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonEmpty("items")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			cmd.items.map(item => NeedSrc(item.well)).toList
		)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val lItem = Result.sequence(cmd.items.toList.map(_.toTokenItem(ctx.ob, ctx.node))) match {
			case Error(ls) => ls.foreach(messages.addError); return Expand2Errors()
			case Success(l) => l
		}
		if (messages.hasErrors) {
			Expand2Errors()
		}
		else {
			val events = lItem.flatMap(item => {
				TipAspirateEventBean(item.tip, item.well, item.volume) ::
				WellRemoveEventBean(item.well, item.volume) :: Nil
			})
			val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(lItem, ctx.ob, ctx.states)
			Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, doc, docMarkdown)
		}
	}
}

class SpirateCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	@BeanProperty var mixSpec: MixSpecBean = null
	
	def toTokenItem(ob: ObjBase, node: CmdNodeBean): Result[SpirateTokenItem] = {
		node.checkPropertyNonNull_?(this, "tip", "well", "volume", "policy")
		if (node.getErrorCount != 0)
			return Error(Nil)
		for {
			tipObj <- ob.findTip(tip)
			wellObj <- ob.findWell2(well)
		} yield {
			new SpirateTokenItem(tipObj, wellObj, LiquidVolume.l(volume), policy)
		}
	}
}

case class AspirateToken(
	val items: List[SpirateTokenItem]
) extends CmdToken

case class SpirateTokenItem(
	val tip: Tip,
	val well: Well2,
	val volume: LiquidVolume,
	val policy: String
) extends HasTipWell

object SpirateTokenItem {
	
	def toAspriateDocString(item_l: Seq[SpirateTokenItem], ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well2]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All wells
		val well_l = item_l.map(_.well).toList
		
		val well_? : Option[Well2] = well_l.distinct match {
			case well :: Nil => Some(well)
			case _ => None
		}
		
		val well_s = well_? match {
			case Some(well) => well.id
			case None => getWellsString(well_l)
		}
		
		val lLiquid = well_l.flatMap(_.wellState(states).map(_.liquid).toOption)
		val liquid_? : Option[Liquid] = lLiquid.distinct match {
			case liquid :: Nil => Some(liquid)
			case _ => None
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
		
		val volume_l = item_l.map(_.volume)
		val volume_? : Option[LiquidVolume] = volume_l.distinct match {
			case volume :: Nil => Some(volume)
			case _ => None
		}
		val sVolumes_? = volume_? match {
			case Some(volume) => Some(volume.toString())
			case _ => None
		}

		val sVolumesAndLiquids = List(sVolumes_?, sLiquids_?).flatten.mkString(" of ")
		
		//Printer.getWellsDebugString(args.items.map(_.dest))
		//val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
		
		val doc = List("Aspriate", sVolumesAndLiquids, "from", well_s).filterNot(_.isEmpty).mkString(" ") 
		(doc, null)
	}
	
	def toDispenseDocString(item_l: Seq[SpirateTokenItem], ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well2]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All wells
		val well_l = item_l.map(_.well).toList
		val tip_l = item_l.map(_.tip).toList
		
		val well_? : Option[Well2] = well_l.distinct match {
			case well :: Nil => Some(well)
			case _ => None
		}
		
		val well_s = well_? match {
			case Some(well) => well.id
			case None => getWellsString(well_l)
		}
		
		val lLiquid = tip_l.flatMap(tip => states.findTipState(tip.id).map(_.liquid).toOption)
		val liquid_? : Option[Liquid] = lLiquid.distinct match {
			case liquid :: Nil => Some(liquid)
			case _ => None
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
		
		val volume_l = item_l.map(_.volume)
		val volume_? : Option[LiquidVolume] = volume_l.distinct match {
			case volume :: Nil => Some(volume)
			case _ => None
		}
		val sVolumes_? = volume_? match {
			case Some(volume) => Some(volume.toString())
			case _ => None
		}

		val sVolumesAndLiquids = List(sVolumes_?, sLiquids_?).flatten.mkString(" of ")
		
		//Printer.getWellsDebugString(args.items.map(_.dest))
		//val sVolumes = Printer.getSeqDebugString(args.items.map(_.nVolume))
		
		val doc = List("Dispense", sVolumesAndLiquids, "to", well_s).filterNot(_.isEmpty).mkString(" ") 
		(doc, null)
	}
}
