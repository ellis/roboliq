package roboliq.core

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty


/**
 * Represents the first command handler expansion which is performed without knowledge of the
 * context of the command in the overall protocol.
 * @see [[roboliq.core.CmdHandler]]
 */
sealed trait Expand1Result
/** Indicates that the first expansion had errors. */
case class Expand1Errors() extends Expand1Result
/** Indicates that the first expansion produced a list of child commands. */
case class Expand1Cmds(cmds: List[CmdBean], doc: String) extends Expand1Result
/** Indicates that the first expansion produced a list resources required for expansion. */
case class Expand1Resources(resources: List[NeedResource]) extends Expand1Result

/**
 * Represents the second command handler expansion after resources have been procured and
 * the states of objects in known.
 * @see [[roboliq.core.CmdHandler]]
 */
sealed abstract class Expand2Result
/** Indicates that the second expansion had errors. */
case class Expand2Errors() extends Expand2Result
/** Indicates that the second expansion produced a list of child commands. */
case class Expand2Cmds(cmds: List[CmdBean], events: List[EventBean], doc: String, docMarkDown: String = null) extends Expand2Result
/** Indicates that the second expansion produced a list of final tokens to be passed to the robot translator. */
case class Expand2Tokens(cmds: List[CmdToken], events: List[EventBean], doc: String, docMarkDown: String = null) extends Expand2Result

/**
 * Base class for all command handlers, but
 * please derive your command handlers from [[roboliq.core.CmdHandlerA]] instead.
 * 
 * @see [[roboliq.core.CmdHandlerA]]
 */
abstract class CmdHandler {
	/** Return true if this handler wants to process the given command */
	def canHandle(command: CmdBean): Boolean

	/**
	 * First expansion attempt.
	 * If the command can be expanded into child commands without any further context,
	 * then return [[roboliq.core.Expand1Cmds]].
	 * If the command requires resources before it can be expanded,
	 * then return [[roboliq.core.Expand1Resources]].
	 * Otherwise if there is an error, return [[robliq.core.Expand1Errors]].
	 */
	def expand1(command: CmdBean, messages: CmdMessageWriter): Expand1Result

	/**
	 * Second expansion attempt using a context `ctx` which should hold any required resources
	 * and knows the states of all objects.
	 * If the command war expanded into child commands,
	 * then return [[roboliq.core.Expand2Cmds]].
	 * If the command war expanded into final tokens for the robot translator,
	 * then return [[roboliq.core.Expand2Tokens]].
	 * Otherwise if there were errors, return [[robliq.core.Expand2Errors]].
	 */
	def expand2(command: CmdBean, ctx: ProcessorContext, messages: CmdMessageWriter): Expand2Result
}

/**
 * A more specialized derivation of [[roboliq.core.CmdHandler]] which takes
 * a type parameters `A` of the CmdBean class it will process in order to
 * guarantee more type safety for derived handlers.
 */
abstract class CmdHandlerA[A <: CmdBean : Manifest] extends CmdHandler {
	type CmdType = A
	
	def canHandle(command: CmdBean): Boolean = {
		manifest[A].erasure.isInstance(command)
	}

	def expand1(command: CmdBean, messages: CmdMessageWriter): Expand1Result = {
		expand1A(command.asInstanceOf[A], messages)
	}

	/**
	 * Type-safe version of `expand1`.
	 * @see expand1
	 */
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result
	
	def expand2(
		command: CmdBean,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		expand2A(command.asInstanceOf[A], ctx, messages)
	}
		
	/**
	 * Type-safe version of `expand2`.
	 * @see expand2
	 */
	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result
}
