package roboliq.processor

import scala.language.implicitConversions

import roboliq.core._


sealed abstract class Step
case class Step_Lookup(l: LookupList) extends Step
case class Step_SubCommands(cmd_l: List[Command], events: List[EventBean], doc: String, docMarkDown: String = null)
case class Step_Done() extends Step

object Step {
	implicit def fromLookupList(l: LookupList): Step =
		Step_Lookup(l)
		
	//implicit def toResult(step: CompilerStep): RqResult[CompilerStep] =
	//	RqSuccess(step)
}
