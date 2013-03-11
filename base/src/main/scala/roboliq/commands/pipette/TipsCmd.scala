package roboliq.commands.pipette

import scala.reflect.runtime.{universe => ru}
import scala.collection.JavaConversions._
import scalaz._
import Scalaz._
import roboliq.core._, roboliq.entity._, roboliq.processor._, roboliq.events._


case class TipsCmd(
	description_? : Option[String],
	tips: List[TipState],
	tipModel_? : Option[TipModel],
	cleanIntensity_? : Option[CleanIntensity.Value],
	items: List[TipsItem]
) extends Cmd {
	def cmd = "pipette.tips"
	def typ = ru.typeOf[this.type]
}

case class TipsItem(
	tip: TipState,
	tipModel_? : Option[TipModel],
	cleanIntensity_? : Option[CleanIntensity.Value]
)

class TipsHandler_Fixed extends CommandHandler[TipsCmd]("pipette.tips") {
	def handleCmd(cmd: TipsCmd): RqReturn = {
		fnRequire(lookupAll[WashProgram]) { program_l =>
			// Get list of items by combining cmd.tips and cmd.items
			// And make sure that each tip is only specified once
			val tipToItems0_m = cmd.items.groupBy(_.tip)
			val tipToItems1_m = cmd.tips.map(tip => tip -> List(TipsItem(tip, None, None))).toMap
			val tipToItems2_m = tipToItems0_m |+| tipToItems1_m
			val tipToItems3_m = tipToItems2_m.map(pair => {
				pair._1 -> (pair._2 match {
					case List(item) => RqSuccess(item)
					case _ => RqError(s"tip `${pair._1}` was given multiple specifications: ${pair._2}")
				})
			})
			
			for {
				item_l <- RqResult.toResultOfList(tipToItems3_m.values.toList)
				// Create list of pairs of wash programs and tips
				l1: List[RqResult[(WashProgram, TipState)]] =
					item_l.flatMap(item => {
						val cleanIntensity_? = item.cleanIntensity_?.orElse(cmd.cleanIntensity_?)
						val res: Option[RqResult[(WashProgram, TipState)]] =
							cleanIntensity_?.map(cleanIntensity => {
								program_l.find(program => program.intensity >= cleanIntensity) match {
									case None => RqError(s"Couldn't find wash program for item `$item`")
									case Some(program) => RqSuccess(program -> item.tip)
								}
							})
						res
					})
				l2 <- RqResult.toResultOfList(l1)
				// Group the list by wash program to list of tips
				m: Map[WashProgram, List[TipState]] = l2.groupBy(_._1).mapValues(_.map(_._2))
				ret <- output(
					m.toList.map(pair => low.WashTipsCmd(None, pair._1, pair._2))
					//AspirateToken(cmd.items)
				)
			} yield ret
		}
	}
}
