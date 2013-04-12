package roboliq.handlers.system

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.system._
import roboliq.compiler._


class L3P_Prompt extends CommandCompilerL3 {
	type CmdType = L3C_Prompt
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		Success(Seq(L2C_Prompt(cmd.s)))
	}
}
