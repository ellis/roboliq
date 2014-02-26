package roboliq.input.commands

import roboliq.core._
import roboliq.entities._

trait Command

case class Distribute(
	source: PipetteSources,
	destination: PipetteDestinations,
	volume: LiquidVolume,
	contact_? : Option[PipettePosition.Value],
	sterilize_? : Option[CleanIntensity.Value],
	sterilizeBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	sterilizeAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command

sealed trait TitrateAmount
case class TitrateAmount_Volume(volume: LiquidVolume) extends TitrateAmount
case class TitrateAmount_Range(min: LiquidVolume, max: LiquidVolume) extends TitrateAmount

// REFACTOR: delete this
case class TitrateSource(
	source: String,
	amount_? : Option[TitrateAmount]
)

case class Titrate(
	steps: List[TitrateStep],
	destination: PipetteDestinations,
	volume_? : Option[LiquidVolume],
	replicates_? : Option[Int]
) extends Command {
	def getItems: RqResult[List[TitrateItem]] = {
		RqResult.toResultOfList(steps.map(_.getItem)).map(_.flatten)
	}
}

case class TitrateStep(
	and: List[TitrateStep],
	or: List[TitrateStep],
	source_? : Option[PipetteSources],
	volume_? : Option[List[LiquidVolume]],
	//min_? : Option[LiquidVolume],
	//max_? : Option[LiquidVolume],
	contact_? : Option[PipettePosition.Value],
	sterilize_? : Option[CleanIntensity.Value],
	sterilizeBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	sterilizeAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command {
	def getItem: RqResult[List[TitrateItem]] = {
		val n = 0 + (if (and.isEmpty) 0 else 1) + (if (or.isEmpty) 0 else 1) + (if (source_?.isEmpty) 0 else 1)
		if (n != 1) {
			RqError("Each titration step must specify exactly one of: `and`, `or`, or `source`")
		}
		else {
			if (!and.isEmpty) RqResult.toResultOfList(and.map(_.getItem)).map(l => List(TitrateItem_And(l.flatten)))
			else if (!or.isEmpty) RqResult.toResultOfList(or.map(_.getItem)).map(l => List(TitrateItem_Or(l.flatten)))
			else {
				val l: List[TitrateItem_SourceVolume] = source_?.get.sources.flatMap(src => {
					volume_? match {
						case None => List(TitrateItem_SourceVolume(this, src, None))
						case Some(volume_l) => volume_l.map(volume => TitrateItem_SourceVolume(this, src, Some(volume)))
					}
				})
				val l2 = if (l.size == 1) l else List(TitrateItem_Or(l))
				RqSuccess(l2)
			}
		}
	}
}

sealed trait TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String)
}
case class TitrateItem_And(l: List[TitrateItem]) extends TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String) {
		println(indent + "and:")
		l.foreach(_.printShortHierarchy(eb, indent+"  "))
	}
}
case class TitrateItem_Or(l: List[TitrateItem]) extends TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String) {
		println(indent + "or:")
		l.foreach(_.printShortHierarchy(eb, indent+"  "))
	}
}
case class TitrateItem_SourceVolume(
	step: TitrateStep,
	source: LiquidSource,
	volume_? : Option[LiquidVolume]
) extends TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String) {
		println(indent + source.l.head + " " + volume_?)
	}
}

case class Transfer(
	source: PipetteDestinations,
	destination: PipetteDestinations,
	volume: List[LiquidVolume],
	contact_? : Option[PipettePosition.Value],
	sterilize_? : Option[CleanIntensity.Value],
	sterilizeBefore_? : Option[CleanIntensity.Value],
	sterilizeBetween_? : Option[CleanIntensity.Value],
	sterilizeAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[String],
	pipettePolicy_? : Option[String]
) extends Command
