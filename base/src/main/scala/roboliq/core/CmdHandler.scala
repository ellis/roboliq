package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty


sealed abstract class Expand1Result
case class Expand1Errors() extends Expand1Result
case class Expand1Cmds(cmds: List[CmdBean]) extends Expand1Result
case class Expand1Resources(resources: List[NeedResource]) extends Expand1Result

sealed abstract class Expand2Result
case class Expand2Errors() extends Expand2Result
case class Expand2Cmds(cmds: List[CmdBean], events: List[EventBean]) extends Expand2Result
case class Expand2Tokens(cmds: List[CmdToken], events: List[EventBean]) extends Expand2Result

abstract class CmdHandler {
	/** Return true if this handler wants to process this given command */
	def canHandle(command: CmdBean): Boolean

	def expand1(command: CmdBean, messages: CmdMessageWriter): Expand1Result

	def expand2(command: CmdBean, ctx: ProcessorContext, messages: CmdMessageWriter): Expand2Result
}

abstract class CmdHandlerA[A <: CmdBean : Manifest] extends CmdHandler {
	type CmdType = A
	
	def canHandle(command: CmdBean): Boolean = {
		manifest[A].erasure.isInstance(command)
	}

	/*
	def checkParams1(command: CmdBean, messages: CmdMessageWriter) =
		checkParams1A(command.asInstanceOf[A], messages)
	
	def checkParams1A(cmd: A, messages: CmdMessageWriter)
	
	def checkParams2(command: CmdBean, ctx: ProcessorContext, messages: CmdMessageWriter) =
		checkParams2A(command.asInstanceOf[A], ctx, messages)
	
	def checkParams2A(cmd: A, ctx: ProcessorContext, messages: CmdMessageWriter)
	*/
	
	def expand1(command: CmdBean, messages: CmdMessageWriter): Expand1Result = {
		expand1A(command.asInstanceOf[A], messages)
	}

	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result
	
	def expand2(
		command: CmdBean,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		expand2A(command.asInstanceOf[A], ctx, messages)
	}
		
	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result
}