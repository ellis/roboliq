package roboliq.common

import scala.collection.immutable.SortedSet

trait Command {
	def toDebugString = toString
	
	def getWellsDebugString(wells: Iterable[WellConfigL2]): String = {
		val sorted = wells.toSeq.sortBy(identity)
		val mapPlateToWell = sorted.groupBy(_.holder)
		val lsPlates = for ((plate, wells) <- mapPlateToWell) yield {
			val lsWells = Command.getWellStrings(wells.toList)
			plate.sLabel + ":" + lsWells.mkString(",")
		}
		lsPlates.mkString(";")
	}
}

object Command {
	private def getWellStrings(wells: List[WellConfigL2]): List[String] = {
		wells match {
			case Nil => Nil
			case well0 :: wellsNext0 =>
				val (well1, wellsNext) = getLastContiguousWell(well0, wellsNext0)
				val bShowCol = (well0.holder.nCols > 1)
				val sWells = getWellString(well0, bShowCol) + (if (well0 eq well1) "" else "+"+(well1.index - well0.index + 1).toString)
				sWells :: getWellStrings(wellsNext)
		}
	}
	
	private def getLastContiguousWell(wellPrev: WellConfigL2, wells: List[WellConfigL2]): Tuple2[WellConfigL2, List[WellConfigL2]] = {
		wells match {
			case Nil =>
				(wellPrev, wells)
			case well :: rest =>
				if (well.index != wellPrev.index + 1)
					(wellPrev, wells)
				else
					getLastContiguousWell(well, rest)
		}
	}
	
	private def getWellString(well: WellConfigL2, bShowCol: Boolean): String = {
		if (bShowCol)
			(well.iCol + 'A').asInstanceOf[Char].toString + (well.iRow + 1)
		else
			(well.index + 1).toString
	}
}

trait CommandL4 extends Command {
	type L3Type <: CommandL3
	def addKnowledge(kb: KnowledgeBase)
	def toL3(states: RobotState): Either[Seq[String], L3Type]
}
trait CommandL3 extends Command
trait CommandL2 extends Command {
	type L1Type <: CommandL1
	def updateState(builder: StateBuilder)
	def toL1(states: RobotState): Either[Seq[String], L1Type]
}
trait CommandL1 extends Command

/*abstract class CommandL2 extends Command
abstract class CommandL3 extends Command
abstract class CommandL4 extends Command
*/