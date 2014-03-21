package aiplan.strips2

import Strips._
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._

object Pop {
	def pop(plan0: PartialPlan, indentLevel: Int = 0): Either[String, PartialPlan] = {
		val indent = "  " * indentLevel
		println()
		println(s"${indent}actions:")
		(0 until plan0.action_l.size).foreach(i => println(indent+"| "+plan0.getActionText(i)))
		println(indent+"openGoals: "+plan0.openGoal_l)
		println(s"${indent}assignments: ${plan0.bindings.assignment_m}")
		println(s"${indent}variables: ${plan0.bindings.variable_m}")
		println(s"${indent}toDot:")
		plan0.toDot.split("\n").foreach(s => println(indent+s))
		if (plan0.openGoal_l.isEmpty) {
			println("FOUND")
			//println(plan0.toDot)
			Right(plan0)
		}
		else {
			val (consumer_i, precond_i) = plan0.openGoal_l.head
			val provider1_l = plan0.getExistingProviders(consumer_i, precond_i)
			val provider2_l = plan0.getNewProviders(consumer_i, precond_i)
			val provider_l = provider1_l ++ provider2_l
			
			println(indent+"providers:")
			provider_l.foreach(pair => {
				val op_s = pair._1 match {
					case Left(op) => op.toString
					case Right(i) => s"${plan0.getActionText(i)}"
				}
				println(s"$indent| ${op_s} using ${pair._2}")
			})
			def chooseAction(
				l: List[(Either[Operator, Int], Map[String, String])]
			): Either[String, PartialPlan] = {
				l match {
					case Nil =>
						Left(s"Couldn't find an action to fulfill ${consumer_i}/${precond_i}")
					case (either, binding_m) :: rest =>
						println(s"${indent}try $either with ${binding_m}")
						for {
							res <- (either match {
								case Left(op) =>
									println(indent+"op: "+op)
									for {
										plan1 <- plan0.addAction(op)
									} yield (plan1, plan1.action_l.size - 1)
								case Right(provider_i) => Right(plan0, provider_i)
							})
							(plan1, provider_i) = res
							link = CausalLink(provider_i, consumer_i, precond_i)
							plan2 <- plan1.addLink(link, binding_m, Map())
							planX <- handleThreats(plan2, link)
							plan3 <- (pop(plan2, indentLevel + 1) match {
								case Right(x) => Right(x)
								case Left(msg) => chooseAction(rest)
							}).right
						} yield plan3
				}
			}
			
			def handleThreats(plan0: PartialPlan, link: CausalLink): Either[String, PartialPlan] = {
				// Find threats on the link or created by the provider
				val threat0_l = plan0.findThreats
				val threat_l = threat0_l.toList.filter(pair => pair._1 == link.provider_i || pair._2 == link)
				println(s"${indent}  threats on link ${link} or from action")
				threat_l.foreach(pair => println(s"${indent}  | ${pair}"))
				threat_l.foldLeft(Right(plan0) : Either[String, PartialPlan]) { (plan_?, threat) =>
					val (action_i, link) = threat
					for {
						plan <- plan_?
						resolver_l = plan.getResolvers(action_i, link)
						_ = println(s"${indent}  resolvers: ${resolver_l}")
						plan1 <- chooseResolver(plan, resolver_l)
					} yield plan1
				}
			}
			
			def chooseResolver(
				plan0: PartialPlan,
				resolver_l: List[Resolver]
			): Either[String, PartialPlan] = {
				resolver_l match {
					case Nil => Left("No resolver found for the threat")
					case resolver :: rest =>
						resolver match {
							case Resolver_Ordering(before_i, after_i) =>
								for {
									plan1 <- plan0.addOrdering(before_i, after_i).right
									plan2 <- (pop(plan1, indentLevel + 1) match {
										case Right(x) => Right(x)
										case Left(msg) => chooseResolver(plan0, rest)
									}).right
								} yield plan2
							case Resolver_Inequality(name1, name2) =>
								for {
									plan1 <- plan0.addBindingNe(name1, name2).right
									plan2 <- (pop(plan1, indentLevel + 1) match {
										case Right(x) => Right(x)
										case Left(msg) => chooseResolver(plan0, rest)
									}).right
								} yield plan2
						}
				}
			}
			
			chooseAction(provider_l)
		}
	}
}
