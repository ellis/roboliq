package aiplan.strips2

import Strips._
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._

sealed trait PopState
case class PopState_Done(
	plan: PartialPlan
) extends PopState
case class PopState_SelectGoal(
	plan: PartialPlan,
	indentLevel: Int
) extends PopState
case class PopState_HandleGoal(
	plan: PartialPlan,
	goal: (Int, Int),
	indentLevel: Int
) extends PopState
case class PopState_ChooseAction(
	plan: PartialPlan,
	goal: (Int, Int),
	provider_l: List[(Either[Operator, Int], Map[String, String])],
	indentLevel: Int
) extends PopState
case class PopState_HandleAction(
	plan: PartialPlan,
	goal: (Int, Int),
	provider: (Either[Operator, Int], Map[String, String]),
	provider_l: List[(Either[Operator, Int], Map[String, String])],
	indentLevel: Int
) extends PopState
//case class PopState_SelectThreat()

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
							plan3 <- (pop(planX, indentLevel + 1) match {
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
	
	def step(ps: PopState): Either[String, PopState] = {
		ps match {
			case x: PopState_Done => Right(x)
			case x: PopState_SelectGoal => stepSelectGoal(x)
			case x: PopState_HandleGoal => stepHandleGoal(x)
			case x: PopState_ChooseAction => stepChooseAction(x)
			case x: PopState_HandleAction => stepHandleAction(x)
		}
	}
	
	def stepSelectGoal(x: PopState_SelectGoal): Either[String, PopState] = {
		val indent = "  " * x.indentLevel
		val plan0 = x.plan
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
			Right(PopState_Done(plan0))
		}
		else {
			Right(PopState_HandleGoal(plan0, plan0.openGoal_l.head, x.indentLevel + 1))
		}
	}
	
	def stepHandleGoal(x: PopState_HandleGoal): Either[String, PopState] = {
		val plan0 = x.plan
		val (consumer_i, precond_i) = x.goal
		val provider1_l = plan0.getExistingProviders(consumer_i, precond_i)
		val provider2_l = plan0.getNewProviders(consumer_i, precond_i)
		val provider_l = provider1_l ++ provider2_l
		Right(PopState_ChooseAction(x.plan, x.goal, provider_l, x.indentLevel + 1))
	}
	
	def stepChooseAction(x: PopState_ChooseAction): Either[String, PopState] = {
		val indent = "  " * x.indentLevel
		x.provider_l match {
			case Nil =>
				Left(s"Couldn't find an action to fulfill ${x.goal}")
			case provider :: rest =>
				println(s"${indent}try $provider")
				Right(PopState_HandleAction(x.plan, x.goal, provider, rest, x.indentLevel + 1))
		}
	}
	
	def stepHandleAction(x: PopState_HandleAction): Either[String, PopState] = {
		val indent = "  " * x.indentLevel
		val (consumer_i, precond_i) = x.goal
		val (either, binding_m) = x.provider
		println(s"${indent}try $either with ${binding_m}")

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
							plan0.addOrdering(before_i, after_i)
						case Resolver_Inequality(name1, name2) =>
							plan0.addBindingNe(name1, name2)
					}
			}
		}

		val plan1_? = for {
			res <- (either match {
				case Left(op) =>
					println(indent+"op: "+op)
					for {
						plan1 <- x.plan.addAction(op)
					} yield (plan1, plan1.action_l.size - 1)
				case Right(provider_i) => Right(x.plan, provider_i)
			})
			(plan1, provider_i) = res
			link = CausalLink(provider_i, consumer_i, precond_i)
			plan2 <- plan1.addLink(link, binding_m, Map())
			plan3 <- handleThreats(plan2, link)
		} yield plan3
		
		plan1_? match {
			case Right(plan1) => Right(PopState_SelectGoal(plan1, x.indentLevel + 1))
			case Left(msg) => Right(PopState_ChooseAction(x.plan, x.goal, x.provider_l, x.indentLevel - 3))
		}
	}
}
