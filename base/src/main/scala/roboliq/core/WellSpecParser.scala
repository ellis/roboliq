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
}