package roboliq.common

import scala.collection.immutable.SortedSet

trait Command {
	def toDebugString = toString
	
	def getWellsDebugString(wells: Iterable[WellConfigL2]): String = Command.getWellsDebugString(wells)
	
	def getSeqDebugString[T](seq: Seq[T]): String = Command.getSeqDebugString(seq)
}

object Command {
	def getSeqDebugString[T](seq: Seq[T]): String = {
		seq match {
			case Seq(a, rest @ _*) =>
				if (rest.forall(_ == a))
					a.toString
				else
					seq.toString
			case _ =>
				"Nil"
		}
	}

	def getWellsDebugString(wells: Iterable[WellConfigL2]): String = {
		/*
		val sorted = wells.toSeq//.sortBy(identity)
		val mapPlateToWell = sorted.groupBy(_.holder)
		val lsPlates = for ((plate, wells) <- mapPlateToWell) yield {
			val lsWells = Command.getWellStrings(wells.toList)
			plate.sLabel + ":" + lsWells.mkString(",")
		}
		*/
		val rrWell = wells.toList.foldLeft(List[List[WellConfigL2]]())((acc, well) => acc match {
			case Nil => List(List(well))
			case (curr @ (prev :: rest)) :: others =>
				if (well.holder eq prev.holder) (well :: curr) :: others
				else List(well) :: (curr :: others)
			case _ => Nil // Error
		})
		val lsPlates = rrWell.reverse.map(rWell => {
			val lWell = rWell.reverse
			val plate = lWell.head.holder
			val lsWells = Command.getWellStrings(lWell)
			plate.sLabel + ":" + lsWells.mkString(",")
		})
		lsPlates.mkString(";")
	}
	
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
			(well.iRow + 'A').asInstanceOf[Char].toString + (well.iCol + 1)
		else
			(well.index + 1).toString
	}
}

trait CommandL4 extends Command {
	type L3Type <: CommandL3
	def addKnowledge(kb: KnowledgeBase)
	def toL3(states: RobotState): Result[L3Type]
}
trait CommandL3 extends Command
trait CommandL2 extends Command {
	type L1Type <: CommandL1
	def updateState(builder: StateBuilder)
	def toL1(states: RobotState): Result[L1Type]
}
trait CommandL1 extends Command

/*abstract class CommandL2 extends Command
abstract class CommandL3 extends Command
abstract class CommandL4 extends Command
*/