package roboliq.common

import scala.collection.immutable.SortedSet


/**
 * Wells are guaranteed to be on the same plate
 */
class WellGroupPlate private[common] (
	set: SortedSet[WellConfigL2],
	val plate: Plate,
	iCol_? : Option[Int] = None,
	bAdjacent: Boolean = false
) extends WellGroup(set, Some(plate), iCol_?, bAdjacent) {
	override def toString: String = {
		getClass().getSimpleName() + List(Command.getWellsDebugString(set), plate, iCol_?, bAdjacent).mkString("(", ", ", ")")
	}
}

/**
 * Wells are guaranteed to be in the same column of the same plate
 */
class WellGroupCol private[common] (
	set: SortedSet[WellConfigL2],
	plate: Plate,
	val iCol: Int,
	bAdjacent: Boolean = false
) extends WellGroupPlate(set, plate, Some(iCol), bAdjacent) {
	override def toString: String = {
		getClass().getSimpleName() + List(Command.getWellsDebugString(set), plate, iCol, bAdjacent).mkString("(", ", ", ")")
	}
}

/**
 * Wells are guaranteed to be adjacent in the same column of the same plate
 */
class WellGroupAdjacent private[common] (
	set: SortedSet[WellConfigL2],
	plate: Plate,
	iCol: Int
) extends WellGroupCol(set, plate, iCol, true) {
	assert(bAdjacent == true)
	override def toString: String = {
		getClass().getSimpleName() + List(Command.getWellsDebugString(set), plate, iCol).mkString("(", ", ", ")")
	}
}

/**
 * Base class for grouping wells in such a way as to guarantee that some criterion is fulfilled
 */
sealed class WellGroup private[common] (
	val set: SortedSet[WellConfigL2],
	val plate_? : Option[Plate],
	val iCol_? : Option[Int],
	val bAdjacent: Boolean
) {
	def add(well: WellConfigL2): WellGroup = {
		if (set.isEmpty)
			new WellGroupAdjacent(SortedSet(well), well.holder, well.iCol)
		else {
			val set2 = set + well
			plate_? match {
				case Some(well.holder) =>
					iCol_? match {
						case Some(iCol) if iCol == well.iCol =>
							val indexTop = set.head.index
							val indexBot = set.last.index
							if (well.index == indexTop - 1 || well.index == indexBot + 1)
								new WellGroupAdjacent(set2, well.holder, iCol)
							else
								new WellGroupCol(set2, well.holder, iCol)
						case _ =>
							new WellGroupPlate(set2, well.holder)
					}
				case _ =>
					new WellGroup(set2, None, None, false)
			}
		}
	}
	
	def +(well: WellConfigL2): WellGroup = add(well)

	def splitByPlate(): Seq[WellGroup] = {
		plate_? match {
			case None => set.toSeq.groupBy(_.holder).toSeq.sortBy(_._1).map(pair => WellGroup(pair._2))
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
	
	def apply(well: WellConfigL2): WellGroup = empty.add(well)
	
	//def apply(wells: Seq[WellConfigL2]): WellGroup = wells.foldLeft(empty) { (acc, well) => acc.add(well) }

	def apply(wells: Iterable[WellConfigL2]): WellGroup = wells.foldLeft(empty) { (acc, well) => acc.add(well) }
}
