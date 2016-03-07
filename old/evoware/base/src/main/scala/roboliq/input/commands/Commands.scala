/*package roboliq.input.commands

import roboliq.core._
import roboliq.entities._
import roboliq.input.Instruction

case class ReadPlate(
	program: ReaderProgram,
	plate: Plate
) extends Instruction

case class SetReagents(
	wells: PipetteDestinations,
	reagents: List[String]
) extends Instruction {
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
*/