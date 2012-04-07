package roboliq.core

import scala.util.parsing.combinator._


case class RowCol(row: Int, col: Int)
sealed abstract class WellSpec
case class WellSpecOne(rc: RowCol) extends WellSpec
case class WellSpecVertical(rc0: RowCol, rc1: RowCol) extends WellSpec
case class WellSpecHorizontal(rc0: RowCol, rc1: RowCol) extends WellSpec
case class WellSpecMatrix(rc0: RowCol, rc1: RowCol) extends WellSpec

object WellSpecParser extends JavaTokenParsers {
	val row: Parser[Int] = """[A-Z]""".r ^^ { case s => s.charAt(0) - 'A' }
	val col: Parser[Int] = """0*[0-9]+""".r ^^ { case s => s.toInt - 1 }
	
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
	
	def parse(input: String): List[Tuple2[String, List[WellSpec]]] = {
		parseAll(plates, input).getOrElse(Nil)
	}
}