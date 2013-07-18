package roboliq.translator.jshop

object JshopTranslator {
	
	def translate(s: String): Unit = {
		val l = s.split("\r?\n")
		l.map(translateLine)
	}
	
	val RxOperator = """\(!.*\)""".r
	
	def translateLine(line: String): Unit = {
		line match {
			case RxOperator(s) =>
				val l = s.split(' ')
				val op = l(0)
				val agent = l(1)
				op match {
					case "agent-activate" =>
					case "transporter-run" =>
					case _ =>
				}
		}
	}
}