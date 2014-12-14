package roboliq.input

import roboliq.core._
import spray.json.JsValue
import scala.annotation.tailrec

object StringfParser {
	def parse(input: String): ResultE[String] = {
		def rx = """\$\{([^}]+)\}""".r
		val text_l = rx.split(input).toList
		val match_l = rx.findAllMatchIn(input).toList
		step(text_l, match_l, "")
	}
	
	private def step(text_l: List[String], match_l: List[scala.util.matching.Regex.Match], acc: String): ResultE[String] = {
		(text_l, match_l) match {
			case (Nil, Nil) => ResultE.unit(acc)
			case (s :: Nil, Nil) => ResultE.unit(acc + s)
			case (_, m :: match1_l) =>
				val name = m.subgroups.head
				val (prefix, text1_l) = text_l match {
					case s :: rest => (s, rest)
					case Nil => ("", Nil)
				}
				for {
					scope <- ResultE.getScope
					jsval <- ResultE.from(scope.get(name), s"variable `$name` not in scope")
					s = jsval.toText
					acc1 = acc + prefix + s
					res1 <- step(text1_l, match1_l, acc1)
				} yield res1
			case _ => ResultE.error(s"INTERNAL ERROR parsing $text_l and $match_l")
		}
	}
}
