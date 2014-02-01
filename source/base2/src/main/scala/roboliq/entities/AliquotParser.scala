package roboliq.entities

import scala.util.parsing.combinator.JavaTokenParsers
import roboliq.core._

object AliquotParser {
	def parseAliquot(input: String, nameToSubstance_m: Map[String, Substance]): RsResult[Aliquot] = {
		new AliquotParser0(nameToSubstance_m).parseAliquot(input)
	}
}

private class AliquotParser0(nameToSubstance_m: Map[String, Substance]) extends JavaTokenParsers {
	import scala.util.parsing.combinator._

	def realConst: Parser[BigDecimal] = """[+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+-]?\d+)?""".r ^^ { num =>
		BigDecimal(num)
	}

	def unit: Parser[String] = """[num]?l""".r ^^ { s => s }

	def substance: Parser[Substance] = ident ^^ { case ident => nameToSubstance_m(ident) }
	
	//Either[Substance, List[Aliquot]]
	def mixture: Parser[Mixture] = (substance | aliquotList) ^^ {
		case s: Substance => Mixture(Left(s))
		case aliquot_l => Mixture(Right(aliquot_l.asInstanceOf[List[Aliquot]]))
	}
	
	def aliquot: Parser[Aliquot] = mixture ~ "@" ~ volume ^^ {
		case m ~ _ ~ n => Aliquot(m, Distribution_Singular(SubstanceUnits.Liter, n.l))
	}
	
	def aliquotList: Parser[List[Aliquot]] = "(" ~ rep1sep(aliquot, "+") ~ ")" ^^ {
		case "(" ~ aliquot_l ~ ")" => aliquot_l
	}
	
	def volume: Parser[LiquidVolume] = realConst ~ unit ^^ {
		case n ~ u => u match {
			case "nl" => LiquidVolume.nl(n)
			case "ul" => LiquidVolume.ul(n)
			case "ml" => LiquidVolume.ml(n)
			case "l" => LiquidVolume.l(n)
		}
	}
	
	def parseAliquot(input: String): RsResult[Aliquot] = {
		parseAll(aliquot, input) match {
			case Success(x, _) => RsSuccess(x)
			case NoSuccess(msg, _) => RsError(msg)
		}
	}
}