package roboliq.entities

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._

object AmountSpecParser {
	def parse(input: String): RsResult[AmountSpec] = {
		AmountSpecParser0.parse(input)
	}
}

private object AmountSpecParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	//def realConst: Parser[BigDecimal] = """(?:-)?\d+(?:(?:\.\d+E(?:-)?\d+)|(?:\.\d+)|(?:E(?:-)?\d+))""".r ^^ { num =>
	def realConst: Parser[BigDecimal] = """[+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+-]?\d+)?""".r ^^ { num =>
		BigDecimal(num)
	}
	
	def volumeUnit: Parser[String] = """[num]?l""".r ^^ { s => s }
	
	def volume: Parser[AmountSpec] = realConst ~ volumeUnit ^^ {
		case n ~ u => u match {
			case "nl" => Amount_l(LiquidVolume.nl(n).l)
			case "ul" => Amount_l(LiquidVolume.ul(n).l)
			case "ml" => Amount_l(LiquidVolume.ml(n).l)
			case "l" => Amount_l(LiquidVolume.l(n).l)
		}
	}
	
	def dilutionInvFactor: Parser[AmountSpec] = realConst ~ 'x' ^^ {
		case den ~ _ => Amount_x(1, den)
	}
	
	def dilutionDiv: Parser[AmountSpec] = realConst ~ '/' ~ realConst ^^ {
		case num ~ _ ~ den => Amount_x(num, den)
	}
	
	def dilutionOdds: Parser[AmountSpec] = realConst ~ ':' ~ realConst ^^ {
		case num ~ _ ~ den => Amount_x(num, num + den)
	}
	
	def complete: Parser[AmountSpec] = volume | dilutionInvFactor | dilutionDiv | dilutionOdds
	
	def parse(input: String): RsResult[AmountSpec] = {
		parseAll(complete, input) match {
			case Success(volume, _) => RsSuccess(volume)
			case NoSuccess(msg, _) => RsError(msg)
		}
	}
}