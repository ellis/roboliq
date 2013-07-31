package roboliq.translator.jshop

import roboliq.tokens.control.CommentToken
import roboliq.input.Protocol
import roboliq.tokens.Token
import roboliq.tokens.control.PromptToken
import roboliq.tokens.transport.EvowareTransporterRunToken

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
				val agentIdent = l(1)
				val identToAgentObject: Map[String, Object] = protocol.agentToIdentToInternalObject.get(agentIdent).map(_.toMap).getOrElse(Map())
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
						val List(deviceIdent, labwareIdent, modelIdent, originIdent, destinationIdent, vectorIdent) = l.toList.drop(2)
						if (agentIdent == "user") {
							val model = protocol.eb.getEntity(modelIdent).get
							val modelLabel = model.label.getOrElse(model.key)
							val origin = protocol.eb.getEntity(originIdent).get
							val originLabel = origin.label.getOrElse(origin.key)
							val destination = protocol.eb.getEntity(destinationIdent).get
							val destinationLabel = destination.label.getOrElse(destination.key)
							PromptToken(s"Please move labware `${labwareIdent}` model `${modelLabel}` from `${originLabel}` to `${destinationLabel}`") :: Nil
						}
						else {
							val roma_i: Int = identToAgentObject(deviceIdent).asInstanceOf[Integer]
							val model = identToAgentObject(modelIdent).asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel]
							val origin = identToAgentObject(originIdent).asInstanceOf[roboliq.evoware.parser.CarrierSite]
							val destination = identToAgentObject(destinationIdent).asInstanceOf[roboliq.evoware.parser.CarrierSite]
							val vectorClass = identToAgentObject(vectorIdent).toString
							EvowareTransporterRunToken(
								roma_i = roma_i,
								vectorClass = vectorClass,
								model = model,
								origin = origin,
								destination = destination
							) :: Nil
						}
					case _ => Nil
				}
			case _ => Nil
		}
	}
}