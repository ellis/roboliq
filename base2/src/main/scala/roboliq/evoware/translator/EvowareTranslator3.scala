package roboliq.evoware.translator

import scala.collection.mutable.ArrayBuffer
import grizzled.slf4j.Logger
import roboliq.core._
import roboliq.entities._
import roboliq.evoware.parser._
import roboliq.tokens._

class EvowareTranslator3 {
	private val logger = Logger[this.type]

	val scripts = new ArrayBuffer[EvowareScript]
	val builder = new EvowareScriptBuilder
	
	def addToken(token: Token): RsResult[Unit] = {
		
	}

	private def translate(cmd1: Token, builder: EvowareScriptBuilder): RqResult[Unit] = {
		for { cmds0 <- cmd1 match {
			case c: control.CommentToken => comment(c)
			case c: control.PromptToken => prompt(c)
			case c: control.low.CallToken => call(c)
			case c: control.low.ExecToken => execute(c)
			//case c: pipette.low.AspirateToken => aspirate(builder, c)
			//case c: pipette.low.DispenseToken => dispense(builder, c)
			//case c: DetectLevelToken => detectLevel(builder, c)
			//case c: L1C_EvowareFacts => facts(builder, c)
			//case c: EvowareSubroutineToken => subroutine(builder, c)
			//case c: MixToken => mix(builder, c.items)
			case c: transport.EvowareTransporterRunToken => transportLabware(builder, c)
			//case c: L1C_TipsGet => tipsGet(c)
			//case c: L1C_TipsDrop => tipsDrop(c)
			//case c: L1C_Timer => timer(c.args)
			//case c: L1C_SaveCurrentLocation => Success(Seq())
			//case c: TipsWashToken => clean(builder, c)
		}} yield {
			builder.cmds ++= cmds0
			()
		}
	}

	private def comment(cmd: control.CommentToken): RqResult[Seq[L0C_Command]] = {
		RqSuccess(Seq(L0C_Comment(cmd.text)))
	}
	
	private def prompt(cmd: control.PromptToken): RqResult[Seq[L0C_Command]] = {
		RqSuccess(Seq(L0C_Prompt(cmd.text)))
	}
	
	private def call(cmd: control.low.CallToken): RqResult[Seq[L0C_Command]] = {
		RqSuccess(List(L0C_Subroutine(
			cmd.text
		)))
	}
	
	private def execute(cmd: control.low.ExecToken): RqResult[Seq[L0C_Command]] = {
		val nWaitOpts = (cmd.waitTillDone, cmd.checkResult) match {
			case (true, true) => 6
			case _ => 2
		}
		val sResultVar = if (cmd.checkResult) "RESULT" else ""
		RqSuccess(Seq(L0C_Execute(cmd.text, nWaitOpts, sResultVar)))
	}
}