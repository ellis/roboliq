package roboliq.translator.jshop

import roboliq.tokens.control.CommentToken
import roboliq.input.Protocol
import roboliq.tokens.Token
import roboliq.tokens.control.PromptToken

object JshopTranslator {
	
	def translate(protocol: Protocol, s: String): List[Token] = {
		val l = s.split("\r?\n")
		l.toList.flatMap(line => translateLine(protocol, line))
	}
	
	val RxOperator = """\(!(.*)\)""".r
	
	def translateLine(protocol: Protocol, line: String): List[Token] = {
		line match {
			case RxOperator(s) =>
				val l = s.split(' ')
				val op = l(0)
				val agent = l(1)
				op match {
					case "agent-activate" => Nil
					case "log" =>
						val textId = l(2)
						val text = protocol.idToObject(textId).toString
						CommentToken(text) :: Nil
					case "prompt" =>
						val textId = l(2)
						val text = protocol.idToObject(textId).toString
						PromptToken(text) :: Nil
					case "transporter-run" =>
						if (agent == "user") {
							PromptToken(s"Please move labware `${l(3)}` model `` from `` to ``") :: Nil
						}
						else {
							/*MovePlateToken()
		val deviceId_? : Option[String],
		val plate: Plate,
		val plateSrc: PlateLocation,
		val plateDest: PlateLocation*/
							PromptToken(s"Please move labware `${l(3)}` model `` from `` to ``") :: Nil
						}
					case _ => Nil
				}
			case _ => Nil
		}
	}
}