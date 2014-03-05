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
	pipettePolicy_? : Option[String],
	effects: List[WorldStateEvent]
) extends Command

case class SetReagents(
	wells: PipetteDestinations,
	reagents: List[String]
) extends Command with Action {
	def effects: List[WorldStateEvent] = {
		val event = new WorldStateEvent {
			def update(builder: WorldStateBuilder): RqResult[Unit] = {
				val l = for ((wellInfo, reagentName) <- (wells.l zip reagents)) yield {
					val aliquot0 = builder.well_aliquot_m.getOrElse(wellInfo.well, Aliquot.empty)
					val substance = Substance(
						key = reagentName,
						label = Some(reagentName),
						description = None,
						kind = SubstanceKind.Liquid,
						tipCleanPolicy = TipCleanPolicy.TT,
						contaminants = Set(),
						costPerUnit_? = None,
						valuePerUnit_? = None,
						molarity_? = None,
						gramPerMole_? = None,
						celciusAndConcToViscosity = Nil,
						sequence_? = None
					)
					val mixture = Mixture(Left(substance))
					val aliquot = Aliquot(mixture, aliquot0.distribution)
					builder.well_aliquot_m(wellInfo.well) = aliquot
				}
				builder.well_aliquot_m.foreach(println)
				RqSuccess(())
			}
		}
		List(event)
	}
}

case class ShakePlate(
	program: ShakerSpec,
	plate: Plate
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
	allOf: List[TitrateStep],
	destination: PipetteDestinations,
	volume_? : Option[LiquidVolume],
	replicates_? : Option[Int]
) extends Command {
	def getItems: RqResult[List[TitrateItem]] = {
		RqResult.toResultOfList(allOf.map(_.getItem)).map(_.flatten)
	}
}

case class TitrateStep(
	allOf: List[TitrateStep],
	oneOf: List[TitrateStep],
	source_? : Option[PipetteSources],
	amount_? : Option[List[PipetteAmount]],
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
		val n = 0 + (if (allOf.isEmpty) 0 else 1) + (if (oneOf.isEmpty) 0 else 1) + (if (source_?.isEmpty) 0 else 1)
		if (n != 1) {
			RqError("Each titration step must specify exactly one of: `and`, `or`, or `source`")
		}
		else {
			if (!allOf.isEmpty) RqResult.toResultOfList(allOf.map(_.getItem)).map(l => List(TitrateItem_And(l.flatten)))
			else if (!oneOf.isEmpty) RqResult.toResultOfList(oneOf.map(_.getItem)).map(l => List(TitrateItem_Or(l.flatten)))
			else {
				val l: List[TitrateItem_SourceVolume] = source_?.get.sources.flatMap(src => {
					amount_? match {
						case None => List(TitrateItem_SourceVolume(this, src, None))
						case Some(amount_l) => amount_l.map(amount => TitrateItem_SourceVolume(this, src, Some(amount)))
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
	amount_? : Option[PipetteAmount]
) extends TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String) {
		val amount = amount_? match {
			case None => "*"
			case Some(PipetteAmount_Volume(x)) => x.toString
			case Some(PipetteAmount_Dilution(num, den)) => s"$num:$den"
		}
		println(indent + source.l.head + " " + amount)
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
