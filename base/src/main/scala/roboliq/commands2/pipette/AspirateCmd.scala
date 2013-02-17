package roboliq.commands2.pipette

import scala.reflect.runtime.{universe => ru}
import roboliq.core._
import RqPimper._
import roboliq.processor2._
import scala.collection.JavaConversions._
import scala.Option.option2Iterable
import spray.json._
import roboliq.commands.pipette.TipWellVolumePolicy


case class AspirateCmd(
	description_? : Option[String],
	items: List[TipWellVolumePolicy] //FIXME: This should be TipWellVolumePolicyMixspec
)

case class AspirateToken(
	val items: List[TipWellVolumePolicy]
) extends CmdToken

class AspirateHandler extends CommandHandler("pipetter.aspirate") {
	val fnargs = cmdAs[AspirateCmd] { cmd =>
		val events = cmd.items.flatMap(item => {
			TipAspirateEvent(item.tip, item.well.vessel, item.volume) :: Nil
			//WellRemoveEventBean(item.well.vessel, item.volume) :: Nil
		})
		//val (doc, docMarkdown) = SpirateTokenItem.toAspriateDocString(cmd.items, ctx.ob, ctx.states)
		//Expand2Tokens(List(new AspirateToken(lItem.toList)), events.toList, doc, docMarkdown)
		RqSuccess(List(
			ComputationItem_Token(AspirateToken(cmd.items)),
			ComputationItem_Events(events)
		))
	}
}

/** Represents an aspiration event. */
case class TipAspirateEvent(
	tip: Tip,
	/** Source well ID. */
	src: VesselState,
	/** Volume in liters to aspirate. */
	volume: LiquidVolume
) extends Event {
	def toJson: JsValue = {
		JsObject(Map(
			"kind" -> JsString("tip.aspirate"),
			"tip" -> JsString(tip.id),
			"src" -> JsString(src.vessel.id),
			"volume" -> JsString(volume.toString)
		))
	}
}

class TipAspirateEventHandler {// extends EventHandler {
	import RqFunctionHandler._
	
	def fnargs(event: TipAspirateEvent) = {
		fnRequire (lookup[TipState0](event.tip.id)) { state0 =>
			val liquid = event.src.content.liquid
			val volumeNew = state0.volume + event.volume
			val state_# = TipState0(
				state0.conf,
				state0.model_?,
				Some(event.src),
				state0.liquid + liquid,
				volumeNew,
				state0.contamInside ++ liquid.contaminants,
				LiquidVolume.max(state0.nContamInsideVolume, volumeNew),
				state0.contamOutside ++ liquid.contaminants,
				state0.srcsEntered + liquid,
				state0.destsEntered,
				CleanIntensity.None,
				state0.cleanDegreePrev,
				CleanIntensity.max(state0.cleanDegreePending, liquid.tipCleanPolicy.exit)
			)
			RqSuccess(List(EventItem_State(TKP("tipState", event.tip.id, Nil), Conversions.tipStateToJson(state_#))))
		}
	}
}
/*
case class SpirateCmdItem(
	tip: Tip,
	well: Well,
	volume: LiquidVolume,
	policy: PipettePolicy,
	x: TipWellVolumePolicy
	//@BeanProperty var mixSpec: MixSpecBean = null
	/*
	def toTokenItem(ob: ObjBase, node: CmdNodeBean): Result[TipWellVolumePolicy] = {
		node.checkPropertyNonNull_?(this, "tip", "well", "volume", "policy")
		if (node.getErrorCount != 0)
			return Error(Nil)
		for {
			tipObj <- ob.findTip(tip)
			wellObj <- ob.findWell2(well)
		} yield {
			new TipWellVolumePolicy(tipObj, wellObj, LiquidVolume.l(volume), PipettePolicy.fromName(policy))
		}
	}*/
)
*/
/*
class AspirateHandler extends CommandHandler("aspirate") {
	import roboliq.processor2.{ConversionsDirect => D}

	val fnargs = cmdAs[AspriateCmd] { cmd =>
		fnRequireList
	}
	
	private def toTwvp(jsval: JsValue): RqResult[TipWellVolumePolicy] = {
		for {
			jsobj <- D.toJsObject(jsval)
			id <- D.getString('tip, jsobj)
			plateModelIds <- D.getStringList('plateModels, jsobj)
			cooled <- D.getBoolean('cooled, jsobj)
		} yield {
		
	}
	def toTokenItem(ob: ObjBase, node: CmdNodeBean): Result[TipWellVolumePolicy] = {
		node.checkPropertyNonNull_?(this, "tip", "well", "volume", "policy")
		if (node.getErrorCount != 0)
			return Error(Nil)
		for {
			tipObj <- ob.findTip(tip)
			wellObj <- ob.findWell2(well)
		} yield {
			new TipWellVolumePolicy(tipObj, wellObj, LiquidVolume.l(volume), PipettePolicy.fromName(policy))
		}
	}

	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	/** Volume in liters. */
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	@BeanProperty var mixSpec: MixSpecBean = null

	def getResult = {
		handlerRequire (
			getJsValue
		) {
			(jsval) =>
			for {
				jsobj <- D.toJsObject(jsval)
				id <- D.getString('id, jsobj)
				plateModelIds <- D.getStringList('plateModels, jsobj)
				cooled <- D.getBoolean('cooled, jsobj)
			} yield {
				List(ConversionItem_Conversion(
					input_l = plateModelIds.map(id => KeyClassOpt(KeyClass(TKP("plateModel", id, Nil), ru.typeOf[PlateModel]))),
					fn = (l: List[Object]) => {
						val plateModel_l = l.asInstanceOf[List[PlateModel]]
						val loc = new PlateLocation(id, plateModel_l, cooled)
						RqSuccess(List(ConversionItem_Object(loc)))
					}
				))
			}
			(plate, plateState, dest, deviceId_?) =>
			for {
				locationSrc <- plateState.location_?.asRq(s"plate `${plate.id}` must have an location set.")
			} yield {
				val token = new MovePlateToken(
					deviceId_?,
					plate,
					locationSrc,
					dest)
				List(ComputationItem_Token(token))
			}
		}
		/*
		handlerRequire(as[String]('id), as[List[String]]('list)/*, as[Option[String]]('deviceId)*/) {
			(id: String, list: List[String]) =>
			handlerRequire (lookupPlate(id)) { (plate: Plate) =>
				val l: List[RequireItem[PlateModel]] = list.map(lookupPlateModel(_): RequireItem[PlateModel])
				handlerRequireN (l) { (list2: List[PlateModel]) =>
					handlerReturn(Token_Comment(plate.toString + " on " + list + " to " + list2))
				}
			}
		}*/
	}
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
*/


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