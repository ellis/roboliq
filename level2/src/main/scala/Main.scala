import roboliq.builder.parts._
import roboliq.devices._


object PipetteCommand extends App {
	val kb = new KnowledgeBase
	val water = new Liquid
	val plate = new Plate
	val items = List(
			new PipetteItem(water, plate, 30)
			)
	val cmd = new PipetteCommand
	val res = cmd.pipette(kb, items)
	println(res)
	kb.printUnknown()
}
