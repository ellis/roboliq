package roboliq.commands.pipette.low

import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._
import scala.collection.JavaConversions._
import spray.json._
import roboliq.commands.pipette._


case class AspirateCmd(
	description_? : Option[String],
	items: List[TipWellVolumePolicy]
)

case class AspirateToken(
	val items: List[TipWellVolumePolicy]
) extends CmdToken

class AspirateHandler extends CommandHandler[AspirateCmd]("pipette.low.aspirate") {
	def handleCmd(cmd: AspirateCmd): RqReturn = {
		//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
		val event_l = cmd.items.flatMap(item => {
			TipAspirateEvent(item.tip, item.well.vesselState, item.volume) ::
			VesselRemoveEvent(item.well, item.volume) :: Nil
		})
		output(
			AspirateToken(cmd.items),
			event_l
		)
	}
}

/*
object SpirateTokenItem {
	
	def toAspriateDocString(item_l: Seq[TipWellVolumePolicy], ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All wells
		val well_l = item_l.map(_.well).toList
		
		val well_? : Option[Well] = well_l.distinct match {
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
	
	def toDispenseDocString(item_l: Seq[TipWellVolumePolicy], ob: ObjBase, states: RobotState): Tuple2[String, String] = {
		def getWellsString(l: Iterable[Well]): String =
			WellSpecParser.toString(l.toList, ob, ", ")
		
		// All wells
		val well_l = item_l.map(_.well).toList
		val tip_l = item_l.map(_.tip).toList
		
		val well_? : Option[Well] = well_l.distinct match {
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
*/