package roboliq.core

import scala.collection.immutable.SortedSet


/**
 * Wells are guaranteed to be on the same plate
 */
class WellGroupPlate private[core] (
	set: SortedSet[Well],
	val idPlate: String,
	iCol_? : Option[Int] = None,
	bAdjacent: Boolean = false
) extends WellGroup(set, Some(idPlate), iCol_?, bAdjacent) {
	override def toString: String = {
		getClass().getSimpleName() + List(Printer.getWellsDebugString(set), idPlate, iCol_?, bAdjacent).mkString("(", ", ", ")")
	}
}

/**
 * Wells are guaranteed to be in the same column of the same plate
 */
class WellGroupCol private[core] (
	set: SortedSet[Well],
	idPlate: String,
	val iCol: Int,
	bAdjacent: Boolean = false
) extends WellGroupPlate(set, idPlate, Some(iCol), bAdjacent) {
	override def toString: String = {
		getClass().getSimpleName() + List(Printer.getWellsDebugString(set), idPlate, iCol, bAdjacent).mkString("(", ", ", ")")
	}
}

/**
 * Wells are guaranteed to be adjacent in the same column of the same plate
 */
class WellGroupAdjacent private[core] (
	set: SortedSet[Well],
	idPlate: String,
	iCol: Int
) extends WellGroupCol(set, idPlate, iCol, true) {
	assert(bAdjacent == true)
	override def toString: String = {
		getClass().getSimpleName() + List(Printer.getWellsDebugString(set), idPlate, iCol).mkString("(", ", ", ")")
	}
}

/**
 * Base class for grouping wells in such a way as to guarantee that some criterion is fulfilled
 */
sealed class WellGroup private[core] (
	val set: SortedSet[Well],
	val idPlate_? : Option[String],
	val iCol_? : Option[Int],
	val bAdjacent: Boolean
) {
	def add(well: Well): WellGroup = {
		if (set.isEmpty)
			new WellGroupAdjacent(SortedSet(well), well.idPlate, well.iCol)
		else {
			val set2 = set + well
			idPlate_? match {
				case Some(well.idPlate) =>
					iCol_? match {
						case Some(iCol) if iCol == well.iCol =>
							val indexTop = set.head.index
							val indexBot = set.last.index
							if (well.index == indexTop - 1 || well.index == indexBot + 1)
								new WellGroupAdjacent(set2, well.idPlate, iCol)
							else
								new WellGroupCol(set2, well.idPlate, iCol)
						case _ =>
							new WellGroupPlate(set2, well.idPlate)
					}
				case _ =>
					new WellGroup(set2, None, None, false)
			}
		}
	}
	
	def +(well: Well): WellGroup = add(well)

	def splitByPlate(): Seq[WellGroup] = {
		idPlate_? match {
			case None => set.toSeq.groupBy(_.idPlate).toSeq.sortBy(_._1).map(pair => WellGroup(pair._2))
			case _ => Seq(this)
		}
	}
	
	def splitByCol(): Seq[WellGroup] = {
		if (iCol_?.isDefined)
			return Seq(this)
			
		set.toList.foldLeft(Nil : List[WellGroup]) { (acc, well) => acc match {
			case Nil => WellGroup(well) :: Nil
			case group :: rest =>
				val group2 = group + well
				if (group2.iCol_?.isDefined)
					group2 :: rest
				else
					WellGroup(well) :: acc
		}}.reverse.toSeq
	}
	
	def splitByAdjacent(): Iterable[WellGroup] = {
		if (bAdjacent)
			return Seq(this)
			
		set.toList.foldLeft(Nil : List[WellGroup]) { (acc, well) => acc match {
			case Nil => WellGroup(well) :: Nil
			case group :: rest =>
				val group2 = group + well
				if (group2.bAdjacent)
					group2 :: rest
				else
					WellGroup(well) :: acc
		}}.reverse
	}
}

object WellGroup {
	val empty = new WellGroup(SortedSet(), None, None, false)
	
	def apply(well: Well): WellGroup = empty.add(well)
	
	//def apply(wells: Seq[Well]): WellGroup = wells.foldLeft(empty) { (acc, well) => acc.add(well) }

	def apply(wells: Iterable[Well]): WellGroup = wells.foldLeft(empty) { (acc, well) => acc.add(well) }
}
