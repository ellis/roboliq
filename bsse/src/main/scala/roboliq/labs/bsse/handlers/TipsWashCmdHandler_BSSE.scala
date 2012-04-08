package roboliq.labs.bsse.handlers

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet

import roboliq.core._
import roboliq.commands.pipette._
import roboliq.devices.pipette._
import roboliq.robots.evoware.commands._
import roboliq.labs.bsse.devices._


class TipsWashCmdHandler_BSSE extends CmdHandlerA[TipsWashCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonNull("intensity")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		val tips = if (cmd.tips == null) Nil else cmd.tips.toList
		Expand1Resources(tips.map(tip => NeedTip(tip)).toList)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		val lTip = cmd.tips.toList.map(tip => ctx.ob.findTip_?(tip, messages)).flatten
		val lTipAll = ctx.ob.findAllTips() match {
			case Error(ls) => ls.foreach(messages.addError); Nil
			case Success(l) => l
		}
		if (messages.hasErrors)
			return Expand2Errors()

		//val washProgram = if (cmd.washProgram != null) cmd.washProgram.toInt else 0
		val intensity = WashIntensity.withName(cmd.intensity)
		val sIntensity = intensity match {
			case WashIntensity.None => return Expand2Tokens(Nil, Nil)
			case WashIntensity.Light => "Light"
			case WashIntensity.Thorough => "Thorough"
			case WashIntensity.Decontaminate => "Decontaminate"
		}
		
		val b1000 = lTip.exists(_.index < 4)
		val b50 = lTip.exists(_.index >= 4)

		val sScriptBase = """C:\Program Files\TECAN\EVOware\database\scripts\Roboliq\Roboliq_Clean_"""+sIntensity+"_"

		// Create final tokens
		val tokens = List(
			if (b1000) Some(EvowareSubroutineToken(sScriptBase+"1000.esc")) else None,
			if (b50) Some(EvowareSubroutineToken(sScriptBase+"0050.esc")) else None
		).flatten
		// Events
		val events = lTipAll.flatMap(tip => {
			if (tip.index < 4 && b1000) Some(TipCleanEventBean(tip, intensity))
			else if (tip.index >= 4 && b50) Some(TipCleanEventBean(tip, intensity))
			else None
		})
		
		Expand2Tokens(tokens, events)
	}
}
