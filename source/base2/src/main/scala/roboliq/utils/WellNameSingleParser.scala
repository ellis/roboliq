package roboliq.utils

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._


/**
 * Represents value returned from WellNameParser.
 * 
 * @param row row index (0-based).
 * @param col column index (0-based).
 */
case class WellNameSingleParsed(
	labware_? : Option[String],
	row: Int,
	col: Int
) {
	override def toString: String = {
		val s = (row + 'A').asInstanceOf[Char].toString + ("%02d".format(col + 1))
		labware_? match {
			case None => s
			case Some(labware) => s"$labware($s)"
		}
	}
}

private object WellNameSingleParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	val row: Parser[Int] = """[A-Z]""".r ^^ { case s => s.charAt(0) - 'A' }
	val col: Parser[Int] = """[0-9]+""".r ^^ { case s => s.toInt - 1 }
	
	def justRowCol: Parser[WellNameSingleParsed] = row ~ col ^^ {
		case r ~ c => WellNameSingleParsed(None, r, c)
	}
	
	def plateRowCol: Parser[WellNameSingleParsed] = rep1sep(ident, ".") ~ "(" ~ justRowCol ~ ")" ^^ {
		case l ~ _ ~ rc ~ _ => rc.copy(labware_? = Some(l.mkString(".")))
	}
	
	def parse(input: String): ResultC[WellNameSingleParsed] = {
		val result = parseAll(justRowCol | plateRowCol, input)
		result match {
			case Success(x, _) => ResultC.unit(x)
			case NoSuccess(msg, _) => ResultC.error(msg)
		}
	}
}

/**
 * Parses a string of plates and wells.
 */
object WellNameSingleParser {

	/**
	 * Parse `input` as a string of plates and wells,
	 * and return a list of tuples of referenced plate ID and the wells referenced on those plate.
	 */
	def parse(input: String) = WellNameSingleParser0.parse(input)
}
