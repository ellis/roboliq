package roboliq.entities

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._

object PipetteAmountParser {
	def parse(input: String): RsResult[PipetteAmount] = {
		PipetteAmountParser0.parse(input)
	}
}

private object PipetteAmountParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	//def realConst: Parser[BigDecimal] = """(?:-)?\d+(?:(?:\.\d+E(?:-)?\d+)|(?:\.\d+)|(?:E(?:-)?\d+))""".r ^^ { num =>
	def realConst: Parser[BigDecimal] = """[+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+-]?\d+)?""".r ^^ { num =>
		BigDecimal(num)
	}
	
	def volumeUnit: Parser[String] = """[num]?l""".r ^^ { s => s }
	
	def volume: Parser[PipetteAmount] = realConst ~ volumeUnit ^^ {
		case n ~ u => u match {
			case "nl" => PipetteAmount_Volume(LiquidVolume.nl(n))
			case "ul" => PipetteAmount_Volume(LiquidVolume.ul(n))
			case "ml" => PipetteAmount_Volume(LiquidVolume.ml(n))
			case "l" => PipetteAmount_Volume(LiquidVolume.l(n))
		}
	}
	
	def dilution: Parser[PipetteAmount] = realConst ~ ':' ~ realConst ^^ {
		case num ~ _ ~ den => PipetteAmount_Dilution(num, den)
	}
	
	def complete: Parser[PipetteAmount] = volume | dilution
	
	def parse(input: String): RsResult[PipetteAmount] = {
		parseAll(complete, input) match {
			case Success(volume, _) => RsSuccess(volume)
			case NoSuccess(msg, _) => RsError(msg)
		}
	}
}