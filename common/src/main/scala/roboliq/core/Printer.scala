package roboliq.core

import scala.collection.immutable.SortedSet

object Printer {
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

	def getWellsDebugString(wells: Iterable[Well]): String = {
		/*
		val sorted = wells.toSeq//.sortBy(identity)
		val mapPlateToWell = sorted.groupBy(_.holder)
		val lsPlates = for ((plate, wells) <- mapPlateToWell) yield {
			val lsWells = Command.getWellStrings(wells.toList)
			plate.sLabel + ":" + lsWells.mkString(",")
		}
		*/
		val rrWell = wells.toList.foldLeft(List[List[Well]]())((acc, well) => acc match {
			case Nil => List(List(well))
			case (curr @ (prev :: rest)) :: others =>
				if (well.idPlate eq prev.idPlate) (well :: curr) :: others
				else List(well) :: (curr :: others)
			case _ => Nil // Error
		})
		val lsPlates = rrWell.reverse.map(rWell => {
			val lWell = rWell.reverse
			val idPlate = lWell.head.idPlate
			val lsWells = getWellStrings(lWell)
			idPlate + ":" + lsWells.mkString(",")
		})
		lsPlates.mkString(";")
	}
	
	private def getWellStrings(wells: List[Well]): List[String] = {
		wells match {
			case Nil => Nil
			case well0 :: wellsNext0 =>
				val (well1, wellsNext) = getLastContiguousWell(well0, wellsNext0)
				val bShowCol = (well0.holder.nCols > 1)
				val sWells = getWellString(well0, bShowCol) + (if (well0 eq well1) "" else "+"+(well1.index - well0.index + 1).toString)
				sWells :: getWellStrings(wellsNext)
		}
	}
	
	private def getLastContiguousWell(wellPrev: Well, wells: List[Well]): Tuple2[Well, List[Well]] = {
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
	
	private def getWellString(well: Well, bShowCol: Boolean): String = {
		if (bShowCol)
			(well.iRow + 'A').asInstanceOf[Char].toString + (well.iCol + 1)
		else
			(well.index + 1).toString
	}
}
