package roboliq.core

import scala.util.parsing.combinator._


case class RowCol(row: Int, col: Int) {
	override def toString: String = {
		(row + 'A').asInstanceOf[Char].toString + ("%02d".format(col + 1))
	}
}
sealed abstract class WellSpec
case class WellSpecOne(rc: RowCol) extends WellSpec
case class WellSpecVertical(rc0: RowCol, rc1: RowCol) extends WellSpec
case class WellSpecHorizontal(rc0: RowCol, rc1: RowCol) extends WellSpec
case class WellSpecMatrix(rc0: RowCol, rc1: RowCol) extends WellSpec

object WellSpecParser extends JavaTokenParsers {
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
	
	def toString(lWell: List[Well2], ob: ObjBase, sep: String): String = {
		lWell match {
			case Nil => ""
			case List(well) => well.id
			case well0 :: rest =>
				mergeVertical(lWell) match {
					case Some(s) => s
					case None =>
						mergeHorizontal(lWell, ob) match {
							case Some(s) => s
							case None =>
								lWell.map(_.id).mkString(sep)
						}
				}
		}
	}
	
	private def mergeVertical(lWell: List[Well2]): Option[String] = {
		def expect(idPlate: String, index: Int, l: List[Well2]): Boolean = {
			l match {
				case Nil => true
				case well :: rest =>
					if (well.idPlate != idPlate || well.index != index) false
					else expect(idPlate, index + 1, rest)
			}
		}
		
		val well0 = lWell.head
		if (expect(well0.idPlate, well0.index + 1, lWell.tail)) {
			Some(well0.idPlate+"("+well0.indexName+" d "+lWell.last.indexName+")")
		}
		else
			None
	}
	
	private def mergeHorizontal(lWell: List[Well2], ob: ObjBase): Option[String] = {
		def expect(l: List[Well2]): Boolean = {
			l match {
				case Nil => false
				case x :: Nil => true
				case well0 :: well1 :: rest =>
					if (well0.idPlate != well1.idPlate)
						false
					else if (well0.iRow == well1.iRow && well0.iCol + 1 == well1.iCol)
						expect(l.tail)
					else if (well0.iRow + 1 == well1.iRow && well1.iCol == 0) {
						ob.findPlate(well0.idPlate) match {
							case roboliq.core.Error(_) => false
							case roboliq.core.Success(plate) =>
								if (well0.iCol == plate.model.nCols - 1)
									expect(l.tail)
								else
									false
						}
					}
					else
						false
			}
		}
		
		if (expect(lWell)) {
			val well0 = lWell.head
			Some(well0.idPlate+"("+well0.indexName+" r "+lWell.last.indexName+")")
		}
		else
			None
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
}