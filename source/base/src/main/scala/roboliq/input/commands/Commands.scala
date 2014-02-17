package roboliq.input.commands

import roboliq.core._
import roboliq.entities._

trait Command

case class Distribute(
	source: String,
	destination: String,
	volume: LiquidVolume,
	contact_? : Option[PipettePosition.Value],
	sterilize_? : Option[CleanIntensity.Value],
	sterilizeBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	sterilizeAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command

sealed trait TitrationAmount
case class TitrationAmount_Volume(volume: LiquidVolume) extends TitrationAmount
case class TitrationAmount_Range(min: LiquidVolume, max: LiquidVolume) extends TitrationAmount

// REFACTOR: delete this
case class TitrationSource(
	source: String,
	amount_? : Option[TitrationAmount]
)

case class TitrationSeries(
	steps: List[TitrationStep],
	destination: PipetteDestinations,
	volume_? : Option[LiquidVolume]
) extends Command {
	def getItems: RqResult[List[TitrationItem]] = {
		RqResult.toResultOfList(steps.map(_.getItem)).map(_.flatten)
	}
}

case class TitrationStep(
	and: List[TitrationStep],
	or: List[TitrationStep],
	source_? : Option[PipetteSources],
	volume_? : Option[List[LiquidVolume]],
	min_? : Option[LiquidVolume],
	max_? : Option[LiquidVolume],
	contact_? : Option[PipettePosition.Value],
	sterilize_? : Option[CleanIntensity.Value],
	sterilizeBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	sterilizeAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command {
	def getItem: RqResult[List[TitrationItem]] = {
		val n = 0 + (if (and.isEmpty) 0 else 1) + (if (or.isEmpty) 0 else 1) + (if (source_?.isEmpty) 0 else 1)
		if (n != 1) {
			RqError("Each titration step must specify exactly one of: `and`, `or`, or `source`")
		}
		else {
			if (!and.isEmpty) RqResult.toResultOfList(and.map(_.getItem)).map(l => List(TitrationItem_And(l.flatten)))
			else if (!or.isEmpty) RqResult.toResultOfList(or.map(_.getItem)).map(l => List(TitrationItem_Or(l.flatten)))
			else {
				val l: List[TitrationItem_SourceVolume] = source_?.get.sources.flatMap(src => {
					volume_? match {
						case None => List(TitrationItem_SourceVolume(this, src, None))
						case Some(volume_l) => volume_l.map(volume => TitrationItem_SourceVolume(this, src, Some(volume)))
					}
				})
				RqSuccess(l)
			}
		}
	}
}

sealed trait TitrationItem
case class TitrationItem_And(l: List[TitrationItem]) extends TitrationItem
case class TitrationItem_Or(l: List[TitrationItem]) extends TitrationItem
case class TitrationItem_SourceVolume(
	step: TitrationStep,
	source: LiquidSource,
	volume_? : Option[LiquidVolume]
) extends TitrationItem
