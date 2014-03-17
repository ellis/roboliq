package aiplan.strips2

import Strips._
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._
/*
class Pop {
	type Agenda = List[(Int, Int)]
	
	def pop(problem: Problem, plan0: PartialPlan): Option[PartialPlan] = {
		if (plan0.openGoal_l.isEmpty) {
			Some(plan0)
		}
		else {
			val (consumer_i, precond_i) = plan0.openGoal_l.head
			val provider1_l = plan0.getExistingProviders(consumer_i, precond_i)
			val provider2_l = plan0.getNewProviders(problem.domain, consumer_i, precond_i)
		}
	}
	
	private def popWithAction(
		plan0: PartialPlan,
		agenda0: Agenda,
		consumer_i: Int,
		precond_i: Int,
		relevant: Either[Int, Operator],
		bindings: Map[String, String]
	): Option[(PartialPlan, Agenda)] = {
		val (provider_i, plan1, agenda1) = relevant match {
			case Right(action) =>
				val provider_i = plan0.action_l.size
				val plan1 = plan0.addAction(action).addOrdering(provider_i, consumer_i)
				val agendaNew = (0 until action.preconds.l.length).toList.map(provider_i -> _)
				val agenda1 = agenda0 ++ agendaNew
				(provider_i, plan1, agenda1)
			case Left(provider_i) =>
				(provider_i, plan0, agenda0)
		}
		val link = CausalLink(provider_i, consumer_i, precond_i)
		val plan2 = plan1.addLink(link)
		Some((plan2, agenda1))
	}
	
	def getProviders(plan: PartialPlan, action_i: Int, precond_i: Int): List[Either[Int, Operator]] = {
		null
	}
}
*/