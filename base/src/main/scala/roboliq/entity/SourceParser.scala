package roboliq.entity

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._

trait SourceSpec

case class SourceSpec_Plate(id: String) extends SourceSpec
case class SourceSpec_PlateWell(id: String) extends SourceSpec
case class SourceSpec_PlateWells(id: String, wellSpec_l: List[WellSpec]) extends SourceSpec
case class SourceSpec_Tube(id: String) extends SourceSpec
case class SourceSpec_Substance(id: String) extends SourceSpec

private object SourceParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	val row: Parser[Int] = """[A-Z]""".r ^^ { case s => s.charAt(0) - 'A' }
	val col: Parser[Int] = """[0-9]+""".r ^^ { case s => s.toInt - 1 }
	
	val well: Parser[WellSpecOne] = """[A-Z][0-9][0-9]""".r ^^ {
		case s => WellSpecOne(RowCol(s.charAt(0) - 'A', s.drop(1).toInt - 1))
	}
	
	val well_v_well: Parser[WellSpecVertical] = well ~ "d" ~ well ^^ {
		case w0 ~ _ ~ w1 => WellSpecVertical(w0.rc, w1.rc)
	}
	
	val well_v_row: Parser[WellSpecVertical]  = well ~ "d" ~ row ^^ {
		case w0 ~ _ ~ row1 => WellSpecVertical(w0.rc, RowCol(row1, w0.rc.col))
	}
	
	val well_h_well = well ~ "r" ~ well ^^ {
		case w0 ~ _ ~ w1 => WellSpecHorizontal(w0.rc, w1.rc)
	}

	val well_h_col  = well ~ "r" ~ col ^^ {
		case w0 ~ _ ~ col1 => WellSpecHorizontal(w0.rc, RowCol(w0.rc.row, col1))
	}
	
	val well_x_well = well ~ "x" ~ well ^^ {
		case w0 ~ _ ~ w1 => WellSpecMatrix(w0.rc, w1.rc)
	}
	
	val wellArg: Parser[WellSpec] = well_v_well | well_v_row | well_h_well | well_h_col | well_x_well | well
	
	val wells: Parser[List[WellSpec]] = rep1sep(wellArg, ",")
	
	val plateWells: Parser[SourceSpec_PlateWells] = ident ~ "(" ~ wells ~ ")" ^^ {
		case plate ~ _ ~ l ~ _ => SourceSpec_PlateWells(plate, l)
	}
	
	val plateTubeOrSubstance: Parser[SourceSpec] = ident ^^ {
		case id if id.startsWith("P") => SourceSpec_Plate(id)
		case id if id.startsWith("T") => SourceSpec_Tube(id)
		//case id if id.startsWith("S") => SourceSpec_Substance(id)
		case id => SourceSpec_Substance(id)
	}
	
	val sourceArg = plateWells | plateTubeOrSubstance
	
	val sources: Parser[List[SourceSpec]] = rep1sep(sourceArg, ",")
	
	def parse(input: String): RqResult[List[SourceSpec]] = {
		parseAll(sources, input) match {
			case Success(l, _) =>
				val l2 = l.flatMap(_ match {
					case SourceSpec_PlateWells(plateId, wellSpec_l) =>
						wellSpec_l.flatMap(wellSpec => wellSpec match {
							case WellSpecOne(rc) =>
								List(SourceSpec_PlateWell(plateId + "(" + rc + ")"))
							case WellSpecVertical(rc0, rc1) if rc0.col == rc1.col =>
								(rc0.row to rc1.row).toList.map(row_i => SourceSpec_PlateWell(plateId + "(" + RowCol(row_i, rc0.col) + ")"))
							case WellSpecHorizontal(rc0, rc1) if rc0.row == rc1.row =>
								(rc0.col to rc1.col).toList.map(col_i => SourceSpec_PlateWell(plateId + "(" + RowCol(rc0.row, col_i) + ")"))
							case WellSpecMatrix(rc0, rc1) =>
								(for (col <- rc0.col to rc1.col; row <- rc0.row to rc1.row) yield {
									SourceSpec_PlateWell(plateId + "(" + RowCol(row, col) + ")")
								}).toList
							case _ =>
								List(SourceSpec_PlateWells(plateId, List(wellSpec)))
						})
					case x => List(x)
				})
				RqSuccess(l2)
			case NoSuccess(msg, _) =>
				RqError(msg)
		}
	}

	/*
	def parseToIds(input: String): RqResult[List[String]] = {
		for {
			l <- parse(input)
			id_ll <- RqResult.toResultOfList(l.map(entry => entryToIds(None, entry._1, entry._2)))
		} yield {
			id_ll.flatten
		}
	}
	*/
	
	/*
	private def entryToIds(idPlate: String, lWellSpec: List[WellSpec], ob: ObjBase): Result[List[String]] = {
		if (lWellSpec.isEmpty)
			return roboliq.core.Success(List(idPlate))
		
		for {
			plate <- ob.findPlate(idPlate)
		} yield {
			lWellSpec.flatMap(_ match {
				case WellSpecOne(rc) =>
					List(idPlate + "(" + rc + ")")
				case WellSpecVertical(rc0, rc1) =>
					val i0 = rc0.row + rc0.col * plate.model.rows
					val i1 = rc1.row + rc1.col * plate.model.rows
					(for (i <- i0 to i1) yield {
						val row = i % plate.nRows
						val col = i / plate.nRows
						idPlate + "(" + RowCol(row, col) + ")"
					}).toList
				case WellSpecHorizontal(rc0, rc1) =>
					val i0 = rc0.row * plate.model.cols + rc0.col
					val i1 = rc1.row * plate.model.cols + rc1.col
					(for (i <- i0 to i1) yield {
						val row = i / plate.nCols
						val col = i % plate.nCols
						idPlate + "(" + RowCol(row, col) + ")"
					}).toList
				case WellSpecMatrix(rc0, rc1) =>
					(for (col <- rc0.col to rc1.col; row <- rc0.row to rc1.row) yield {
						idPlate + "(" + RowCol(row, col) + ")"
					}).toList
			})
		}
	}
	*/
}

/**
 * Parses a string of plates and wells.
 */
object SourceParser {

	/**
	 * Parse `input` as a string of plates and wells,
	 * and return a list of tuples of referenced plate ID and the wells referenced on those plate.
	 */
	def parse(input: String): RqResult[List[SourceSpec]] =
		SourceParser0.parse(input)
	
	/*
	/**
	 * Given a list of wells `lWell`, return a string representation.
	 */
	def toString(lWell: List[Well], ob: ObjBase, sep: String): String = {
		// Split list by plate
		def partitionByPlate(lWell: List[Well], accR: List[List[Well]]): List[List[Well]] = {
			lWell match {
				case Nil => accR.reverse
				case well0 :: _ =>
					val (l0, rest) = {
						// If this is a tube:
						if (well0.indexName.isEmpty)
							lWell.span(_.id == well0.id)
						// If on the same plate
						else
							lWell.span(_.idPlate == well0.idPlate)
					}
					partitionByPlate(rest, l0 :: accR)
			}
		}
		val llWell = partitionByPlate(lWell, Nil)
		// For each group of sequential wells on a single plate:
		llWell.map(lWell => {
			lWell match {
				case Nil => ""
				case List(well) => well.id
				case well0 :: _ =>
					def step(lWell: List[Well], accR: List[String]): List[String] = {
						if (lWell.isEmpty)
							accR.reverse
						else {
							val nV = mergeVerticalLen(lWell)
							val nH = mergeHorizontalLen(lWell, ob)
							val nR = mergeRepeatLen(lWell)
							if (nV > 2) {
								val (l0, rest) = lWell.splitAt(nV)
								val s = mergeVerticalString(l0)
								step(rest, s :: accR)
							}
							else if (nH > 2) {
								val (l0, rest) = lWell.splitAt(nH)
								val s = mergeHorizontalString(l0)
								step(rest, s :: accR)
							}
							else if (nR > 1) {
								val (l0, rest) = lWell.splitAt(nR)
								val s = mergeRepeatString(l0)
								step(rest, s :: accR)
							}
							else {
								step(lWell.tail, lWell.head.indexName :: accR)
							}
						}
					}
					val sInner = step(lWell, Nil).mkString(sep)
					if (well0.indexName.isEmpty)
						well0.id+sInner
					else
						well0.idPlate+"("+sInner+")"
			}
		}).mkString(sep)
	}
	
	private def mergeVerticalLen(lWell: List[Well]): Int = {
		def expect(idPlate: String, index: Int, l: List[Well], acc: Int): Int = {
			l match {
				case Nil => acc
				case well :: rest =>
					if (well.idPlate != idPlate || well.index != index) acc
					else expect(idPlate, index + 1, rest, acc + 1)
			}
		}
		
		val well0 = lWell.head
		expect(well0.idPlate, well0.index + 1, lWell.tail, 1)
	}
	
	private def mergeHorizontalLen(lWell: List[Well], ob: ObjBase): Int = {
		def expect(l: List[Well], acc: Int): Int = {
			l match {
				case Nil => acc
				case x :: Nil => acc
				case well0 :: well1 :: rest =>
					val bContinue = {
						if (well0.idPlate != well1.idPlate)
							false
						else if (well0.iRow == well1.iRow && well0.iCol + 1 == well1.iCol)
							true
						else if (well0.iRow + 1 == well1.iRow && well1.iCol == 0) {
							ob.findPlate(well0.idPlate) match {
								case roboliq.core.Error(_) => false
								case roboliq.core.Success(plate) =>
									if (well0.iCol == plate.model.cols - 1)
										true
									else
										false
							}
						}
						else
							false
					}
					if (!bContinue) acc
					else expect(l.tail, acc + 1)
			}
		}
		
		expect(lWell, 1)
	}
	*/
}
