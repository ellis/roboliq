package roboliq.core

import scala.util.parsing.combinator.JavaTokenParsers


/**
 * Represents a row and column index.
 * 
 * @param row row index (0-based).
 * @param col column index (0-based).
 */
case class RowCol(row: Int, col: Int) {
	override def toString: String = {
		(row + 'A').asInstanceOf[Char].toString + ("%02d".format(col + 1))
	}
}
/**
 * Represents slots in an array of rows and columns (usually a well on a plate).
 */
sealed abstract class WellSpec
/** A single well at a given row and column. */
case class WellSpecOne(rc: RowCol) extends WellSpec
///** A single well repeated multiple times */
//case class WellSpecN(rc: RowCol, n: Int) extends WellSpec
/**
 * The sequence of wells obtained by moving downward from `rc0` to `rc1`
 * wrapping to the top of the next column when necessary.
 */
case class WellSpecVertical(rc0: RowCol, rc1: RowCol) extends WellSpec
/**
 * The sequence of wells obtained by moving rightward from `rc0` to `rc1`
 * wrapping to the left of the next row when necessary.
 */
case class WellSpecHorizontal(rc0: RowCol, rc1: RowCol) extends WellSpec
/**
 * The sequence of wells obtained by selecting all wells in the rectangle
 * with `rc0` in the top left corner and `rc1` in the bottom left corner.
 */
case class WellSpecMatrix(rc0: RowCol, rc1: RowCol) extends WellSpec


private object WellSpecParser0 extends JavaTokenParsers {
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
	
	val plateWells: Parser[Tuple2[String, List[WellSpec]]] = ident ~ "(" ~ wells ~ ")" ^^ {
		case plate ~ _ ~ l ~ _ => plate -> l
	}
	
	val plate: Parser[Tuple2[String, List[WellSpec]]] = ident ^^ {
		case plate => plate -> Nil
	}
	
	val plateArg = plateWells | plate
	
	val plates: Parser[List[Tuple2[String, List[WellSpec]]]] = rep1sep(plateArg, ",")
	
	def parse(input: String): roboliq.core.Result[List[Tuple2[String, List[WellSpec]]]] = {
		roboliq.core.Success(parseAll(plates, input).getOrElse(Nil))
	}
	
	def parseToIds(input: String, ob: ObjBase): roboliq.core.Result[List[String]] = {
		for {
			l <- parse(input)
			lId <- Result.mapOver(l){entry => entryToIds(entry._1, entry._2, ob)}.map(_.flatten)
		} yield {
			lId
		}
	}
	
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
					val i0 = rc0.row + rc0.col * plate.model.nRows
					val i1 = rc1.row + rc1.col * plate.model.nRows
					(for (i <- i0 to i1) yield {
						val row = i % plate.nRows
						val col = i / plate.nRows
						idPlate + "(" + RowCol(row, col) + ")"
					}).toList
				case WellSpecHorizontal(rc0, rc1) =>
					val i0 = rc0.row * plate.model.nCols + rc0.col
					val i1 = rc1.row * plate.model.nCols + rc1.col
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
}

/**
 * Parses a string of plates and wells.
 */
object WellSpecParser {
	/**
	 * Parse `input` as a string of plates and wells,
	 * and return a list of tuples of referenced plate ID and the wells referenced on those plate.
	 */
	def parse(input: String): roboliq.core.Result[List[(String, List[WellSpec])]] =
		WellSpecParser0.parse(input)
	
	/**
	 * Parse `input` as a string of plates and wells,
	 * and return a list of the referenced well IDs.
	 */
	def parseToIds(input: String, ob: ObjBase): roboliq.core.Result[List[String]] =
		WellSpecParser0.parseToIds(input, ob)
	
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
									if (well0.iCol == plate.model.nCols - 1)
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
	
	private def mergeRepeatLen(lWell: List[Well]): Int = {
		val well0 = lWell.head
		lWell.tail.takeWhile(well => well.index == well0.index).length + 1
	}
	
	private def mergeVerticalString(lWell: List[Well]): String = {
		lWell.head.indexName+" d "+lWell.last.indexName
	}
	
	private def mergeHorizontalString(lWell: List[Well]): String = {
		val well0 = lWell.head
		well0.indexName+" r "+lWell.last.indexName
	}
	
	private def mergeRepeatString(lWell: List[Well]): String = {
		val well0 = lWell.head
		well0.indexName+"*"+lWell.length
	}
	
	/*
	private def merge(spec1: WellSpec, rc2: RowCol, plateModel: PlateModel): List[WellSpec] = {
		spec1 match {
			case WellSpecOne(rc1) =>
				if (rc2.col == rc1.col && rc2.row == rc1.row + 1) {
					List(WellSpecVertical(rc1, rc2))
				}
				else if (rc2.row == rc1.row && rc2.col == rc1.col + 1) {
					List(WellSpecHorizontal(rc1, rc2))
				}
				else {
					List(spec1, WellSpecOne(rc2))
				}
			case WellSpecVertical(rc0, rc1) =>
				if (rc1.col == rc2.col && rc1.row + 1 == rc2.row)
					List(WellSpecVertical(rc0, rc2))
				else if (rc1.col + 1 == rc2.col && rc1.row == plateModel.nRows - 1 && rc2.row == 0) 
					List(WellSpecVertical(rc0, rc2))
				else
					List(spec1, WellSpecOne(rc2))
			case WellSpec
		}
	}
	*/
	
	/** Get a row/column representation of the index of the a well. */
	def wellIndexName(nRows: Int, nCols: Int, iRow: Int, iCol: Int): String = {
		if (nCols == 1) {
			if (nRows == 1) "" else (iRow + 1).toString
		}
		else if (nRows == 1) {
			(iCol + 1).toString
		}
		else {
			(iRow + 'A').asInstanceOf[Char].toString + ("%02d".format(iCol + 1))
		}
	}

	/** Get a row/column representation of the index of the a well. */
	def wellIndexName(nRows: Int, nCols: Int, iWell: Int): String = {
		wellIndexName(nRows, nCols, iWell % nRows, iWell / nRows)
	}
	
	/** Get a row/column representation of the index of the a well. */
	def wellId(plate: Plate, iWell: Int): String = {
		s"${plate.id}(${wellIndexName(plate.nRows, plate.nCols, iWell)})"
	}

	/** Get a row/column representation of the index of the a well. */
	def wellId(plate: Plate, iRow: Int, iCol: Int): String = {
		s"${plate.id}(${wellIndexName(plate.nRows, plate.nCols, iRow, iCol)})"
	}

	def wellIndex(plate: Plate, iRow: Int, iCol: Int): Int = {
		iCol + iRow * plate.nCols
	}
}
