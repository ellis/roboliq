package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

sealed abstract class Expand2Result
case class Expand2Errors() extends Expand2Result
case class Expand2Cmds(cmds: List[CmdBean], events: List[EventBean]) extends Expand2Result
case class Expand2Tokens(cmds: List[CmdToken], events: List[EventBean]) extends Expand2Result

abstract class CmdHandler(val isFinal: Boolean) {
	/** Return true if this handler wants to process this given command */
	def canHandle(command: CmdBean): Boolean

	//def checkParams1(command: CmdBean, messages: CmdMessageWriter)
	//def checkParams2(command: CmdBean, ctx: ProcessorContext, messages: CmdMessageWriter)

	def expand1(
		command: CmdBean,
		messages: CmdMessageWriter
	): Option[
		Either[List[NeedResource], List[CmdBean]]
	]

	def expand2(
		command: CmdBean,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result
}

abstract class CmdHandlerA[A <: CmdBean : Manifest](isFinal: Boolean) extends CmdHandler(isFinal) {
	type CmdType = A
	
	def canHandle(command: CmdBean): Boolean = {
		command.isInstanceOf[A]
	}

	/*
	def checkParams1(command: CmdBean, messages: CmdMessageWriter) =
		checkParams1A(command.asInstanceOf[A], messages)
	
	def checkParams1A(cmd: A, messages: CmdMessageWriter)
	
	def checkParams2(command: CmdBean, ctx: ProcessorContext, messages: CmdMessageWriter) =
		checkParams2A(command.asInstanceOf[A], ctx, messages)
	
	def checkParams2A(cmd: A, ctx: ProcessorContext, messages: CmdMessageWriter)
	*/
	
	def expand1(
		command: CmdBean,
		messages: CmdMessageWriter
	): Option[
		Either[List[NeedResource], List[CmdBean]]
	] = {
		expand1(command.asInstanceOf[A], messages)
	}

	def expand1A(
		cmd: CmdType,
		messages: CmdMessageWriter
	): Option[
		Either[List[NeedResource], List[CmdBean]]
	]
	
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