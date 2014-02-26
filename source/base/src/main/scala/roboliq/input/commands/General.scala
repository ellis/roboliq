package roboliq.input.commands

import roboliq.entities._
import scala.collection.mutable.SortedSet

case class AgentActivate() extends Command
case class AgentDeactivate() extends Command
case class EvowareSubroutine(path: String) extends Command
case class Log(text: String) extends Command

case class PipetterAspirate(
	val item_l: List[TipWellVolumePolicy]
) extends Command

case class PipetterDispense(
	val item_l: List[TipWellVolumePolicy]
) extends Command

case class PipetterTipsRefresh(
	device: Pipetter,
	// tip, clean intensity, tipModel_?
	item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])]
) extends Command

object PipetterTipsRefresh {
	def combine(l: List[PipetterTipsRefresh]): List[PipetterTipsRefresh] = {
		l.groupBy(_.device).toList.map(pair => {
			val (device, refresh_l) = pair
			val item_l = refresh_l.flatMap(_.item_l)
			val tipToItems_m = item_l.groupBy(_._1)
			val item_l_~ = tipToItems_m.toList.sortBy(_._1).map(pair => {
				val (tip, item_l) = pair
				val intensity = CleanIntensity.max(item_l.map(_._2))
				(tip, intensity, item_l.last._3)
			})
			PipetterTipsRefresh(device, item_l_~)
		})
	}
}

/*case class PipetterTipsRefreshItem(
	tip: Tip,
	intensity: CleanIntensity.Value,
	
	// tip, clean intensity, tipModel_?
	item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])]
) extends Command*/

case class PeelerRun(
	deviceIdent: String,
	specIdent: String,
	labwareIdent: String,
	siteIdent: String
) extends Command

case class Prompt(text: String) extends Command

case class SealerRun(
	deviceIdent: String,
	specIdent: String,
	labwareIdent: String,
	siteIdent: String
) extends Command

/**
 * @param object_l [(labwareIdent, siteIdent)]
 */
case class ShakerRun(
	device: Shaker,
	spec: ShakerSpec,
	labwareToSite_l: List[(Labware, Site)]
) extends Command

case class ThermocyclerClose(
	deviceIdent: String
) extends Command

case class ThermocyclerOpen(
	deviceIdent: String
) extends Command

case class ThermocyclerRun(
	deviceIdent: String,
	specIdent: String/*,
	plateIdent: String*/
) extends Command

case class TransporterRun(
	deviceIdent: String,
	labwareIdent: String,
	modelIdent: String,
	originIdent: String,
	destinationIdent: String,
	vectorIdent: String
) extends Command
