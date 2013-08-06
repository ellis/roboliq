package roboliq.translator.jshop

import roboliq.tokens.control.CommentToken
import roboliq.core._
import roboliq.input.Protocol
import roboliq.tokens.Token
import roboliq.tokens.control.PromptToken
import roboliq.tokens.transport.EvowareTransporterRunToken
import roboliq.entities.WorldStateBuilder
import roboliq.entities.ClientScriptBuilder
import roboliq.evoware.translator.EvowareScriptBuilder
import roboliq.entities.WorldState

object JshopTranslator2 {
	
	def translate(
		protocol: Protocol,
		solution: String,
		agentToBuilder_m: Map[String, ClientScriptBuilder]
	): RqResult[Unit] = {
		val l = solution.split("\r?\n").toList
		val state0 = protocol.state0.toImmutable
		println(s"l: $l")
		def translateStep(line_l: List[String], state: WorldState): RsResult[Unit] = {
			line_l match {
				case Nil => RsSuccess(())
				case line :: rest =>
					translateLine(protocol, agentToBuilder_m, state, line).flatMap(state => translateStep(rest, state))
			}
		}

		for {
			_ <- translateStep(l, state0)
		} yield {
			// Let the builders know that we're done building
			agentToBuilder_m.values.foreach(_.end())
			()
		}
	}
	
	val RxOperator = """\(!(.*)\)""".r
	
	def translateLine(protocol: Protocol, agentToBuilder_m: Map[String, ClientScriptBuilder], state0: WorldState, line: String): RsResult[WorldState] = {
		line match {
			case RxOperator(s) =>
				s.split(' ').toList match {
					case operation :: agentIdent :: arg_l =>
						val builder = agentToBuilder_m(agentIdent)
						for {
							state <- builder.addOperation(
								protocol,
								state0,
								operation,
								agentIdent,
								arg_l
							)
						} yield state
					case _ =>
						RsError(s"invalid operation line: $line")
				}
		}
	}
}