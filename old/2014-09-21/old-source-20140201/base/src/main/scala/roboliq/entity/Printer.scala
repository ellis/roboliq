package roboliq.entity

import scala.collection.immutable.SortedSet
import roboliq.core._


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
				if (well.idPlate == prev.idPlate)
					(well :: curr) :: others
				else
					List(well) :: (curr :: others)
			case _ => Nil // Error
		})
		val lsPlates = rrWell.reverse.map(rWell => {
			val lWell = rWell.reverse
			lWell match {
				case Nil => ""
				case List(well) => well.id
				case well0 :: rest =>
					val lsWells = getWellStrings(lWell.map(_.asInstanceOf[Well]))
					well0.idPlate + "(" + lsWells.mkString(",") + ")"
			}
		})
		lsPlates.mkString(",")
	}
	
	private def getWellStrings(wells: List[Well]): List[String] = {
		wells match {
			case Nil => Nil
			case well0 :: wellsNext0 =>
				val (well1, wellsNext) = getLastContiguousWell(well0, wellsNext0)
				val sWells = well0.indexName + (if (well0 eq well1) "" else "+"+(well1.index - well0.index + 1).toString)
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
	
	private val WellColRowPattern = """([^(]+)\(([A-Z])([0-9]+)\)""".r
	private val WellRowPattern = """([^(]+)\(([0-9]+)\)""".r
	private val WellNullPattern = """([^(]+)\(\)""".r
	def parseWellId(id: String): RqResult[Tuple4[String, String, Int, Int]] = {
		id match {
			case WellColRowPattern(idPlate, sRow, sCol) => RqSuccess((idPlate, sRow+sCol, (sRow.charAt(0) - 'A'), sCol.toInt - 1))
			case WellRowPattern(idPlate, sRow) => RqSuccess((idPlate, sRow, sRow.toInt - 1, 0))
			case WellNullPattern(idPlate) => RqSuccess((idPlate, "", 0, 0))
			case _ => RqSuccess((id, "", 0, 0))
		}
	}
}
