package roboliq.parsers

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._

sealed trait Amount
case class Amount_Dilution(num: BigDecimal, den: BigDecimal) extends Amount
case class Amount_Number(num: BigDecimal) extends Amount
case class Amount_Variable(name: String) extends Amount
case class Amount_Volume(volume: LiquidVolume) extends Amount

object AmountParser {
	def parse(input: String): ResultC[Amount] = {
		AmountParser0.parse(input)
	}
}

private object AmountParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	def realConst: Parser[BigDecimal] = """[+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+-]?\d+)?""".r ^^ { num =>
		BigDecimal(num)
	}
	
	def volumeUnit: Parser[String] = """[num]?l""".r ^^ { s => s }
	
	def volume: Parser[Amount] = realConst ~ volumeUnit ^^ {
		case n ~ u => u match {
			case "nl" => Amount_Volume(LiquidVolume.nl(n))
			case "ul" => Amount_Volume(LiquidVolume.ul(n))
			case "ml" => Amount_Volume(LiquidVolume.ml(n))
			case "l" => Amount_Volume(LiquidVolume.l(n))
		}
	}
	
	def dilution: Parser[Amount] = realConst ~ ':' ~ realConst ^^ {
		case num ~ _ ~ den => Amount_Dilution(num, den)
	}
	
	def number: Parser[Amount] = realConst ^^ {
		case num => Amount_Number(num)
	}
	
	def variable: Parser[Amount] = ident ^^ {
		case name => Amount_Variable(name)
	}
	
	def complete: Parser[Amount] = volume | dilution | number | variable
	
	def parse(input: String): ResultC[Amount] = {
		parseAll(complete, input) match {
			case Success(volume, _) => ResultC.unit(volume)
			case NoSuccess(msg, _) => ResultC.error(msg)
		}
	}
}