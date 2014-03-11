package aiplan.strips2

import Strips._

class Pop {
	type Agenda = List[(Int, Int)]
	
	def pop(plan0: PartialPlan, agenda0: Agenda): Option[PartialPlan] = {
		agenda0 match {
			case Nil => Some(plan0)
			case (action_i, precond_i) :: rest =>
				val relevant_l = getProviders(plan0, action_i, precond_i)
				
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