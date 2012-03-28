package roboliq.protocol.commands

import roboliq.common.KnowledgeBase
import roboliq.common
import roboliq.protocol._


class Pipette extends PCommand {
	val src = new PropertyItem[Item]
	val dest = new PropertyItem[Item]
	val volume = new Property[LiquidVolume]
	def properties: List[Property[_]] = List(src, dest, volume)
	
	/** Change a list of options to an option of a list.  Only succeed if all items are defined. */
	private def invert[A](l: List[Option[A]]): Option[List[A]] = {
		if (l.forall(_.isDefined)) Some(l.flatten)
		else None
	}
	
	private def getWellPointer(property: PropertyItem[_], vom: ValueToObjectMap): Option[common.WellPointer] = {
		invert(property.values.map(vom.mapValueToWellPointer.get)) match {
			case None => None
			case Some(Nil) => None
			case Some(x :: Nil) => Some(x)
			case _ => None // FIXME: merge WellPointers 
		}
	}

	def createCommands(vom: ValueToObjectMap): List[common.Command] = {
		import roboliq.commands.pipette.L4A_PipetteArgs
		import roboliq.commands.pipette.L4A_PipetteItem
		import roboliq.commands.pipette.L4C_Pipette
		import roboliq.commands.pipette.MixSpec

		val valueDb = vom.valueDb
		val x = for {
			src <- getWellPointer(src, vom)
			dest <- getWellPointer(dest, vom)
			lVolume <- volume.getValues_?(valueDb)
		} yield {
			val pipetteItem = new L4A_PipetteItem(src, dest, lVolume.map(_.ml), None, None)
			val lPipetteCmd = L4C_Pipette(new L4A_PipetteArgs(List(pipetteItem), tipOverrides_? = None)) :: Nil
			lPipetteCmd
		}
		x match {
			case None => Nil
			case Some(lCmd) => lCmd
		}
	}
}