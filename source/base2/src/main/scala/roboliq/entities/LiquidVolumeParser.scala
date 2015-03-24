package roboliq.entities

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._

object LiquidVolumeParser {
	def parse(input: String): RsResult[LiquidVolume] = {
		LiquidVolumeParser0.parse(input)
	}
}

private object LiquidVolumeParser0 extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	//def realConst: Parser[BigDecimal] = """(?:-)?\d+(?:(?:\.\d+E(?:-)?\d+)|(?:\.\d+)|(?:E(?:-)?\d+))""".r ^^ { num =>
	def realConst: Parser[BigDecimal] = """[+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+-]?\d+)?""".r ^^ { num =>
		BigDecimal(num)
	}
	
	def unit: Parser[String] = """[num]?l""".r ^^ { s => s }
	
	def complete: Parser[LiquidVolume] = realConst ~ unit ^^ {
		case n ~ u => u match {
			case "nl" => LiquidVolume.nl(n)
			case "ul" => LiquidVolume.ul(n)
			case "ml" => LiquidVolume.ml(n)
			case "l" => LiquidVolume.l(n)
		}
	}
	
	def parse(input: String): RsResult[LiquidVolume] = {
		parseAll(complete, input) match {
			case Success(volume, _) => RsSuccess(volume)
			case NoSuccess(msg, _) => RsError(msg)
		}
	}
}