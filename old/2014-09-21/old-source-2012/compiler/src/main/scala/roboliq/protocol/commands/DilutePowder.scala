/*package roboliq.protocol.commands

import roboliq.common.KnowledgeBase
import roboliq.common
import roboliq.protocol._


class DilutePowder extends PCommand {
	val plate = new PropertyItem[Plate]
	val conc = new Property[LiquidConc]
	
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
		import roboliq.commands.pipette.L4A_PipetteItem
		import roboliq.commands.pipette.MixSpec

		val valueDb = vom.valueDb
		val x = for {
			lWell <- getWellPointer(plate, vom)
		} yield {
			for (well <- lWell.getWells(vom.kb)) {
				
			}
			def mixNext(llMixItem: List[List[MixItemL5]], acc: List[L4A_PipetteItem]): List[L4A_PipetteItem] = {
				if (llMixItem.isEmpty) return acc
				val acc2 = acc ++ llMixItem.map(lMixItem => {
					val mixItem5 = lMixItem.head 
					new L4A_PipetteItem(mixItem5.src, mixItem5.dest, List(mixItem5.vol.nl / 1000.0), None, None)
				})
				val llMixItem2 = llMixItem.map(_.tail).filter(!_.isEmpty)
				mixNext(llMixItem2, acc2)
			}
			val lPipetteItem = mixNext(llMixItem, Nil)
			val lPipetteCmd = roboliq.commands.pipette.L4C_Pipette(new roboliq.commands.pipette.L4A_PipetteArgs(lPipetteItem, tipOverrides_? = None)) :: Nil
			
			val dests = lDest4.reduce(_ + _)
			
			val lMixCmd = PipetteCommandsL4.mix(dests, nVolume.nl * 0.75 / 1000.00, 4) match {
				case common.Success(cmd) => List(cmd)
				case _ => Nil
			}
			
			val lSealCmd = SealCommandsL4.seal(dests) match {
				case common.Success(lCmd) => lCmd
				case _ => Nil
			}
			
			val lCmd: List[common.Command] = lPipetteCmd ++ lMixCmd ++ lSealCmd
			lCmd
		}
		x match {
			case None => Nil
			case Some(lCmd) => lCmd
		}
	}
}*/