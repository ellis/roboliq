package aiplan.strips2

import Strips._
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._

object Pop {
	def pop(plan0: PartialPlan, indentLevel: Int = 0): Either[String, PartialPlan] = {
		val indent = "  " * indentLevel
		println()
		println(indent+"openGoals: "+plan0.openGoal_l)
		if (plan0.openGoal_l.isEmpty) {
			println("FOUND")
			println(plan0.toDot)
			Right(plan0)
		}
		else {
			val (consumer_i, precond_i) = plan0.openGoal_l.head
			val provider1_l = plan0.getExistingProviders(consumer_i, precond_i)
			val provider2_l = plan0.getNewProviders(consumer_i, precond_i)
			
			println(indent+"providers:")
			provider1_l.foreach(s => println(indent+"  "+s))
			provider2_l.foreach(s => println(indent+"  "+s))
			println()
			def chooseAction(
				l: List[(Either[Operator, Int], Map[String, String])]
			): Either[String, PartialPlan] = {
				l match {
					case Nil =>
						Left(s"Couldn't find an action to fulfill ${consumer_i}/${precond_i}")
					case (either, binding_m) :: rest =>
						println(s"${indent}try $either with ${binding_m}")
						either match {
							case Left(op) =>
								for {
									plan1 <- plan0.addAction(op)
									provider_i = plan1.action_l.size - 1
									plan2 <- plan1.addLink(CausalLink(provider_i, consumer_i, precond_i), binding_m, Map())
									plan3 <- (pop(plan2, indentLevel + 1) match {
										case Right(x) => Right(x)
										case Left(msg) => chooseAction(rest)
									}).right
								} yield plan3
							case Right(provider_i) =>
								for {
									plan1 <- plan0.addLink(CausalLink(provider_i, consumer_i, precond_i), binding_m, Map())
									plan2 <- (pop(plan1, indentLevel + 1) match {
										case Right(x) => Right(x)
										case Left(msg) => chooseAction(rest)
									}).right
								} yield plan2
						}
				}
			}
			
			chooseAction(provider1_l ++ provider2_l)
		}
	}
}
