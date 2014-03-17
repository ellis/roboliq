package aiplan.strips2

import Strips._
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._

object Pop {
	def pop(plan0: PartialPlan): Either[String, PartialPlan] = {
		if (plan0.openGoal_l.isEmpty) {
			Right(plan0)
		}
		else {
			val (consumer_i, precond_i) = plan0.openGoal_l.head
			val provider1_l = plan0.getExistingProviders(consumer_i, precond_i)
			val provider2_l = plan0.getNewProviders(consumer_i, precond_i)
			
			def chooseAction(
				l: List[(Either[Operator, Int], Map[String, String])]
			): Either[String, PartialPlan] = l match {
				case Nil =>
					Left(s"Couldn't find an action to fulfill ${consumer_i}/${precond_i}")
				case (either, binding_m) :: rest =>
					either match {
						case Left(op) =>
							for {
								plan1 <- plan0.addAction(op)
								provider_i = plan1.action_l.size - 1
								plan2 <- plan1.addLink(CausalLink(provider_i, consumer_i, precond_i), binding_m, Map())
							} yield plan2
						case Right(provider_i) =>
							for {
								plan1 <- plan0.addLink(CausalLink(provider_i, consumer_i, precond_i), binding_m, Map())
							} yield plan1
					}
			}
			
			chooseAction(provider1_l ++ provider2_l)
		}
	}
}
