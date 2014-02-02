package roboliq.input.commands

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._
import roboliq.entities._

object TitrationSeriesParser {
	def parseAmount(input: String): RsResult[TitrationAmount] = {
		TitrationAmountParser0.parseAmount(input)
	}
	def parseSource(input: String): RsResult[TitrationSource] = {
		TitrationAmountParser0.parseSource(input)
	}
}

private object TitrationAmountParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	def realConst: Parser[BigDecimal] = """[+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+-]?\d+)?""".r ^^ { num =>
		BigDecimal(num)
	}
	
	def unit: Parser[String] = """[num]?l""".r ^^ { s => s }
	
	def volume: Parser[LiquidVolume] = realConst ~ unit ^^ {
		case n ~ u => u match {
			case "nl" => LiquidVolume.nl(n)
			case "ul" => LiquidVolume.ul(n)
			case "ml" => LiquidVolume.ml(n)
			case "l" => LiquidVolume.l(n)
		}
	}
	
	def amtVolume: Parser[TitrationAmount] = volume ^^ {
		case v => TitrationAmount_Volume(v)
	}
	
	def amtRange: Parser[TitrationAmount] = "(range" ~ volume ~ volume ~ ")" ^^ {
		case _ ~ min ~ max ~ _ => TitrationAmount_Range(min, max)
	}
	
	def amt = amtVolume | amtRange
	
	def source: Parser[TitrationSource] = ident ~ opt(amt) ^^ {
		case source ~ amt_? => TitrationSource(source, amt_?)
	}
	
	def parseAmount(input: String): RsResult[TitrationAmount] = {
		parseAll(amt, input) match {
			case Success(x, _) => RsSuccess(x)
			case NoSuccess(msg, _) => RsError(msg)
		}
	}
	
	def parseSource(input: String): RsResult[TitrationSource] = {
		parseAll(source, input) match {
			case Success(x, _) => RsSuccess(x)
			case NoSuccess(msg, _) => RsError(msg)
		}
	}
}
