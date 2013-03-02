package roboliq.core

import scala.collection.immutable.SortedSet


/**
 * Wells are guaranteed to be on the same plate
 */
class WellGroupPlate private[core] (
	query: StateQuery,
	set: SortedSet[Well],
	val idPlate: String,
	iCol_? : Option[Int] = None,
	bAdjacent: Boolean = false
) extends WellGroup(query, set, Some(idPlate), iCol_?, bAdjacent) {
	override def toString: String = {
		getClass().getSimpleName() + List(Printer.getWellsDebugString(set), idPlate, iCol_?, bAdjacent).mkString("(", ", ", ")")
	}
}

/**
 * Wells are guaranteed to be in the same column of the same plate
 */
class WellGroupCol private[core] (
	query: StateQuery,
	set: SortedSet[Well],
	idPlate: String,
	val iCol: Int,
	bAdjacent: Boolean = false
) extends WellGroupPlate(query, set, idPlate, Some(iCol), bAdjacent) {
	override def toString: String = {
		getClass().getSimpleName() + List(Printer.getWellsDebugString(set), idPlate, iCol, bAdjacent).mkString("(", ", ", ")")
	}
}

/**
 * Wells are guaranteed to be adjacent in the same column of the same plate
 */
class WellGroupAdjacent private[core] (
	query: StateQuery,
	set: SortedSet[Well],
	idPlate: String,
	iCol: Int
) extends WellGroupCol(query, set, idPlate, iCol, true) {
	assert(bAdjacent == true)
	override def toString: String = {
		getClass().getSimpleName() + List(Printer.getWellsDebugString(set), idPlate, iCol).mkString("(", ", ", ")")
	}
}

/**
 * Base class for grouping wells in such a way as to guarantee that some criterion is fulfilled
 */
sealed class WellGroup private[core] (
	query: StateQuery,
	val set: SortedSet[Well],
	val idPlate_? : Option[String],
	val iCol_? : Option[Int],
	val bAdjacent: Boolean
) {
	def add(well: Well): WellGroup = {
		query.findWellPosition(well.id) match {
			case Error(_) =>
				println("WARNING: WellGroup.add: no position found for `"+well.id+"`")
				new WellGroup(query, set + well, None, None, false)
			case Success(pos) =>
				if (set.isEmpty)
					new WellGroupAdjacent(query, SortedSet(well), pos.idPlate, pos.iCol)
				else {
					val set2 = set + well
					idPlate_? match {
						case Some(pos.idPlate) =>
							iCol_? match {
								case Some(iCol) if iCol == pos.iCol =>
									(query.findWellPosition(set.head.id), query.findWellPosition(set.last.id)) match {
										case (Success(pos0), Success(pos1)) =>
											val indexTop = pos0.index
											val indexBot = pos1.index
											if (pos.index == indexTop - 1 || pos.index == indexBot + 1)
												new WellGroupAdjacent(query, set2, pos.idPlate, iCol)
											else
												new WellGroupCol(query, set2, pos.idPlate, iCol)
										case _ =>
											println(s"WARNING: WellGroup.add: no position found for `${set.head.id}` and/or `${set.last.id}`")
											this
									}
								case _ =>
									new WellGroupPlate(query, set2, pos.idPlate)
							}
						case _ =>
							new WellGroup(query, set2, None, None, false)
					}
				}
		}
	}
	
	def +(well: Well): WellGroup = add(well)

	def splitByPlate(): Seq[WellGroup] = {
		idPlate_? match {
			case None =>
				set.toSeq.groupBy(well => {
					query.findWellPosition(well.id) match {
						case Error(_) => ""
						case Success(pos) => pos.idPlate
					}
				}).toSeq.sortBy(_._1).map(pair => WellGroup(query, pair._2))
			case _ => Seq(this)
		}
	}
	
	def splitByCol(): Seq[WellGroup] = {
		if (iCol_?.isDefined)
			return Seq(this)
			
		set.toList.foldLeft(Nil : List[WellGroup]) { (acc, well) => acc match {
			case Nil => WellGroup(query, well) :: Nil
			case group :: rest =>
				val group2 = group + well
				if (group2.iCol_?.isDefined)
					group2 :: rest
				else
					WellGroup(query, well) :: acc
		}}.reverse.toSeq
	}
	
	def splitByAdjacent(): Iterable[WellGroup] = {
		if (bAdjacent)
			return Seq(this)
			
		set.toList.foldLeft(Nil : List[WellGroup]) { (acc, well) => acc match {
			case Nil => WellGroup(query, well) :: Nil
			case group :: rest =>
				val group2 = group + well
				if (group2.bAdjacent)
					group2 :: rest
				else
					WellGroup(query, well) :: acc
		}}.reverse
	}
}

object WellGroup {
	def createEmpty(query: StateQuery) = new WellGroup(query, SortedSet(), None, None, false)
	
	def apply(query: StateQuery, well: Well): WellGroup = createEmpty(query).add(well)
	
	def apply(query: StateQuery, wells: Iterable[Well]): WellGroup = wells.foldLeft(createEmpty(query)) { (acc, well) => acc.add(well) }
}
