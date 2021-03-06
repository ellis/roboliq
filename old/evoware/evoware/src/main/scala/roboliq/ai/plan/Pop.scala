package roboliq.ai.plan

import roboliq.ai.strips._
import grizzled.slf4j.Logger
import scalaz._
import Scalaz._
import scala.annotation.tailrec
import ailib.ch03.DebugSpec
import java.io.File
import scala.collection.immutable.Vector

sealed trait PopState {
	val plan: PartialPlan
}
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
	indentLevel: Int
) extends PopState
case class PopState_HandleActions1(
	plan: PartialPlan,
	goalToProvider_l: List[((Int, Int), (Either[Operator, Int], Map[String, String]))],
	indentLevel: Int
) extends PopState

sealed trait GroundState
case class GroundState_SelectVariable(
	plan: PartialPlan,
	indentLevel: Int
) extends GroundState
case class GroundState_HandleVariable(
	plan: PartialPlan,
	name: String,
	indentLevel: Int
) extends GroundState
case class GroundState_ChooseVariableBinding(
	plan: PartialPlan,
	name: String,
	value_l: List[String],
	indentLevel: Int
) extends GroundState
case class GroundState_ChooseUnordered(
	plan: PartialPlan,
	indentLevel: Int
) extends GroundState
case class GroundState_HandleOrdering(
	plan: PartialPlan,
	indentLevel: Int
) extends GroundState
case class GroundState_Done(
	plan: PartialPlan
) extends GroundState

object Pop {
	private val logger = Logger("Pop")

	def pop(plan0: PartialPlan, indentLevel: Int = 0): Either[String, PartialPlan] = {
		val indent = "  " * indentLevel
		println()
		println(s"${indent}actions:")
		(0 until plan0.action_l.size).foreach(i => println(indent+"| "+plan0.getActionText(i)))
		println(indent+"openGoals: "+plan0.openGoal_l)
		println(s"${indent}assignments: ${plan0.bindings.assignment_m}")
		println(s"${indent}options: ${plan0.bindings.option_m}")
		println(s"${indent}nes: ${plan0.bindings.ne_m}")
		println(s"${indent}toDot:")
		plan0.toDot(showInitialState=false).split("\n").foreach(s => println(indent+s))
		if (plan0.openGoal_l.isEmpty) {
			println("FOUND")
			//println(plan0.toDot)
			Right(plan0)
		}
		else {
			val (consumer_i, precond_i) = plan0.openGoal_l.head
			val provider1_l = plan0.getExistingProviders(consumer_i, precond_i).reverse
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
				val (threat_l, threatOther_l) = threat0_l.toList.span(pair => pair._1 == link.provider_i || pair._2 == link)
				//val threat_l = threat0_l.toList.filter(pair => pair._1 == link.provider_i || pair._2 == link)
				
				println(s"${indent}  threats on link ${link} or from action")
				threat_l.foreach(pair => println(s"${indent}  | ${pair}"))
				println(s"${indent}  other threats")
				threatOther_l.foreach(pair => println(s"${indent}  | ${pair}"))

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
	
	def step(stack_r: List[PopState]): Either[String, List[PopState]] = {
		val ps = stack_r.head
		/*// FIXME: for debug only
		if (ps.plan.action_l.size > 11) {
			println("action11:")
			println(ps.plan.action_l(11))
			assert(ps.plan.action_l(11).effects.l.size == 4)
		}
		// ENDFIX*/
		//println("ps: "+ps)
		val next_? : Either[String, Option[PopState]] = ps match {
			case x: PopState_Done => Right(None)
			case x: PopState_SelectGoal => stepSelectGoal(x).right.map(Some(_))
			case x: PopState_HandleGoal => stepHandleGoal(x).right.map(Some(_))
			case x: PopState_ChooseAction => stepChooseAction(x).right.map(Some(_))
			case x: PopState_HandleAction => stepHandleAction(x).right.map(Some(_))
			case x: PopState_HandleActions1 => stepHandleActions1(x).right.map(Some(_))
		}
		//println("next_?: "+next_?)
		next_? match {
			case Right(None) => Right(stack_r)
			case Right(Some(next)) => Right(next :: stack_r)
			case Left(s) =>
				// Save to file system
				{
					val path = new File("log-plan-failed.dot").getPath
					roboliq.utils.FileUtils.writeToFile(path, stack_r.head.plan.toDot(showInitialState=true))
					//1/0
				}
				val stack2_r = stack_r.dropWhile {
					case PopState_ChooseAction(plan, goal, _ :: provider_l, indentLevel) => false
					case _ => true
				}
				stack2_r match {
					case PopState_ChooseAction(plan, goal, _ :: provider_l, indentLevel) :: rest =>
						val ps2 = PopState_ChooseAction(plan, goal, provider_l, indentLevel)
						Right(ps2 :: rest)
					case _ => Left(s)
				}
		}
	}
	
	def stepSelectGoal(x: PopState_SelectGoal): Either[String, PopState] = {
		val indent = "  " * x.indentLevel
		val plan0 = x.plan
		println()
		println(s"${indent}SelectGoal")
		println(s"${indent}actions:")
		(0 until plan0.action_l.size).foreach(i => println(indent+"| "+plan0.getActionText(i)))
		println(s"${indent}openGoals:")
		// Sort the goals so that actions earlier in the ordering get handled first
		def orderGoals(a: (Int, Int), b: (Int, Int)): Boolean = {
			if (a._1 == b._1) a._2 < b._2
			else if (plan0.orderings.map.getOrElse(a._1, Set()).contains(b._1)) true
			else if (plan0.orderings.map.getOrElse(b._1, Set()).contains(a._1)) false
			else a._1 < b._1
		}
		val goal0_l = plan0.openGoal_l.toList.sortWith(orderGoals)
		val goalToProviders_l = goal0_l.map(goal => {
			val (consumer_i, precond_i) = goal
			val provider1a_l = plan0.getExistingProviders(consumer_i, precond_i)
			// Sort existing providers in two different ways:
			// 1) for actions ordered before this action, choose the later actions first (the ones closest to this action)
			// 2) for actions ordered at the same point or after this action, sort ascending by ordering
			/*val successors_l = plan0.orderings.map.getOrElse(consumer_i, Set())
			val provider1_l = provider1a_l.sortWith((pair1, pair2) => {
				val (Right(action1_i), _) = pair1
				val (Right(action2_i), _) = pair2
				(successors_l.contains(action1_i), successors_l.contains(action2_i)) match {
					case (true, false) => true
					case (false, true) => false
					case (true, true) =>
						if (plan0.orderings.map.getOrElse(action1_i, Set()).contains(action2_i)) false
						else if (plan0.orderings.map.getOrElse(action2_i, Set()).contains(action1_i)) true
						else action1_i < action2_i
					case (false, false) =>
						if (plan0.orderings.map.getOrElse(action1_i, Set()).contains(action2_i)) true
						else if (plan0.orderings.map.getOrElse(action2_i, Set()).contains(action1_i)) false
						else action1_i < action2_i
				}
			})*/
			val provider1_l = provider1a_l.reverse
			val provider2_l = plan0.getNewProviders(consumer_i, precond_i)
			(goal, provider1_l ++ provider2_l)
		}).sortWith((pair1, pair2) => {
			val (a, provider1_l) = pair1
			val (b, provider2_l) = pair2
			(provider1_l.length, provider2_l.length) match {
				case (l1, l2) if l1 == l2 => orderGoals(a, b)
				case (0, _) => true
				case (_, 0) => false
				case (1, _) => true
				case (_, 1) => false
				case (l1, l2) =>
					// For open goals for a single action, sort by number of alternative solutions: 
					if (a._1 == b._1 && l1 != l2)
						l1 < l2
					else
						orderGoals(a, b)
			} 
		})
		val goal_l = goalToProviders_l.map(_._1)
		goalToProviders_l.foreach(pair => {
			val (goal, provider_l) = pair
			println(s"${indent}| ${goal} "+plan0.bindings.bind(plan0.action_l(goal._1).preconds.l(goal._2))+s" (${provider_l.length})")
		})
		//println(s"${indent}assignments: ${plan0.bindings.assignment_m}")
		//println(s"${indent}variables: ${plan0.bindings.variable_m}")
		//println(s"${indent}toDot:")
		//plan0.toDot(showInitialState=false).split("\n").foreach(s => println(indent+s))
		if (plan0.openGoal_l.isEmpty) {
			//println("FOUND")
			//println(plan0.toDot)
			Right(PopState_Done(plan0))
		}
		else {
			goalToProviders_l.takeWhile(_._2.size == 1) match {
				case Nil =>
					Right(PopState_HandleGoal(plan0, goal_l.head, x.indentLevel))
				case l =>
					val l2 = l.map { case (goal, List(provider)) => (goal, provider) }
					Right(PopState_HandleActions1(plan0, l2, x.indentLevel))
			}
		}
	}
	
	def stepHandleGoal(x: PopState_HandleGoal): Either[String, PopState] = {
		val indent = "  " * x.indentLevel
		val plan0 = x.plan
		val (consumer_i, precond_i) = x.goal
		val goalAction = plan0.bindings.bind(plan0.action_l(consumer_i).preconds.l(precond_i))
		println(s"${indent}HandleGoal ${x.goal} ${goalAction}")
		val provider1_l = plan0.getExistingProviders(consumer_i, precond_i).reverse
		val provider2_l = plan0.getNewProviders(consumer_i, precond_i)
		val provider_l = provider1_l ++ provider2_l
		Right(PopState_ChooseAction(x.plan, x.goal, provider_l, x.indentLevel))
	}
	
	def stepChooseAction(x: PopState_ChooseAction): Either[String, PopState] = {
		val indent = "  " * x.indentLevel
		println(s"${indent}ChooseAction")
		println(s"${indent}providers:")
		x.provider_l.foreach(pair => println(s"${indent}| $pair"))
		x.provider_l match {
			case Nil =>
				Left(s"Couldn't find an action to fulfill ${x.goal}")
			case provider :: _ =>
				println(s"${indent}try $provider")
				Right(PopState_HandleAction(x.plan, x.goal, provider, x.indentLevel + 1))
		}
	}
	
	def stepHandleAction(x: PopState_HandleAction): Either[String, PopState] = {
		val indent = "  " * x.indentLevel
		val (consumer_i, precond_i) = x.goal
		val (either, binding_m) = x.provider
		println(s"${indent}HandleAction")
		println(s"${indent}try $either with ${binding_m}")

		def handleThreats(plan0: PartialPlan, link: CausalLink): Either[String, PartialPlan] = {
			// Find threats on the link or created by the provider
			val threat0_l = plan0.findThreats
			val (threat_l, threatOther_l) = threat0_l.toList.span(pair => pair._1 == link.provider_i || pair._2 == link)
			//val threat_l = threat0_l.toList.filter(pair => pair._1 == link.provider_i || pair._2 == link)
			
			println(s"${indent}  threats on link ${link} or from action")
			threat_l.foreach(pair => println(s"${indent}  | ${pair}"))
			println(s"${indent}  other threats")
			threatOther_l.foreach(pair => println(s"${indent}  | ${pair}"))

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
			//case Left(msg) => Right(PopState_ChooseAction(x.plan, x.goal, x.provider_l, x.indentLevel - 1))
			case Left(msg) => Left(msg)
		}
	}

	
	def stepHandleActions1(x: PopState_HandleActions1): Either[String, PopState] = {
		def step(goalToProvider_l: List[((Int, Int), (Either[Operator, Int], Map[String, String]))], plan: PartialPlan): Either[String, PopState] = {
			goalToProvider_l match {
				case Nil => Right(PopState_SelectGoal(plan, x.indentLevel + 1))
				case ((goal, provider)) :: rest =>
					val x2 = PopState_HandleAction(plan, goal, provider, x.indentLevel)
					stepHandleAction(x2) match {
						case Left(msg) => Left(msg)
						case Right(PopState_SelectGoal(plan2, _)) => step(rest, plan2)
						case _ => Left("stepHandleActions1: internal error")
					}
			}
		}
		step(x.goalToProvider_l, x.plan)
	}
	
	def stepToEnd(x: PopState): Either[String, PartialPlan] = {
		@tailrec
		def loop(stack_r: List[PopState], n: Int): Either[String, PartialPlan] = {
			// Save to file system
			{
				val path = new File("log-plan-step.dot").getPath
				roboliq.utils.FileUtils.writeToFile(path, stack_r.head.plan.toDot(showInitialState=true))
			}
			if (n >= 1000) return Left(s"debugger end at step ${n}")
			//println(s"stepToEnd: step $n: ${stack_r}")
			step(stack_r) match {
				case Left(msg) => Left(msg)
				case Right(PopState_Done(plan) :: _) => Right(plan)
				case Right(stack2_r) => loop(stack2_r, n + 1)
			}
		}
		loop(List(x), 1)
	}

	case class GroundNode(
		label: String,
		state: PartialPlan,
		parentOpt: Option[GroundNode],
		orderingsMinMap: Map[Int, Set[Int]]
	) extends ailib.ch03.Node[PartialPlan] {
		def plan = state
		override def toString = s"GroundNode($label)"
	}
	case class GroundAction(label: String, newplan: PartialPlan)
	
	class GroundProblem(plan0: PartialPlan) extends ailib.ch03.Problem[PartialPlan, GroundAction, GroundNode] {
		val state0 = plan0
		val root = GroundNode("ROOT", plan0, None, plan0.orderings.getMinimalMap)
		
		def goalTest(plan: PartialPlan): Boolean = {
			val variable_m = plan.bindings.option_m.filter(pair => pair._2.size > 1)
			variable_m.isEmpty && plan.orderings.getMinimalMap.forall(_._2.size == 1)
		}
		
		def actions(plan: PartialPlan): Iterable[GroundAction] = {
			val out = new scala.collection.mutable.ArrayBuffer[String]

			// Assign all unassigned variables
			val variable_m = plan.bindings.option_m.filter(pair => pair._2.size > 1)
			val binding0_l = variable_m.toList.flatMap(pair => pair._2.map(pair._1 -> _))
			val bindingAction_l = binding0_l.flatMap { pair =>
				val (name, value) = pair
				val label = s"$name = $value"
				plan.addBindingEq(name, value) match {
					case Left(_) => None
					case Right(plan1) => out += label; Some(GroundAction(label, plan1))
				}
			}
			
			// Arrange actions in an complete ordering
			// First, choose an ordering of actions so that preconditions are fulfilled in the order they were specified
			// (This does not yet respecting the ordering constraints)
			var actionOrdering_l = Vector[Int](0)
			var actionQueue_l = Vector.range(2, plan.action_l.size)
			println()
			println("START")
			while (!actionQueue_l.isEmpty) {
				//println("actionOrdering_l: "+actionOrdering_l)
				//println("actionQueue_l: "+actionQueue_l)
				val action_i = actionQueue_l.head
				// Get list of providers for this action in the order of specified preconditions
				val before_l = plan.link_l.filter(_.consumer_i == action_i).toList.sortBy(_.precond_i).map(_.provider_i).distinct
				// Get list of providers which haven't already been ordered
				val remaining_l = before_l.filterNot(actionOrdering_l.contains)
				//println(action_i)
				//println(before_l)
				//println(remaining_l)
				// If the list is empty, we can now put this action in the ordering and remove it from the queue
				if (remaining_l.isEmpty) {
					actionOrdering_l = actionOrdering_l :+ action_i
					actionQueue_l = actionQueue_l.tail
				}
				// Otherwise, prepend the providers to the actionQueue
				else {
					actionQueue_l = remaining_l.toVector ++ actionQueue_l.filterNot(remaining_l.contains)
				}
			}
			
			println("actionOrdering_l: "+actionOrdering_l)

			// Second, respect the ordering constraints:
			val orderingsMinMap = plan.orderings.getMinimalMap
			println("orderingsMinMap: "+orderingsMinMap.toList.sortBy(_._1))
			// The underdetermined ordering pairs are all pairs within any orderingsMinMap value of size > 1
			// orderingsMinMap.values gives us a list of lists of actions which may be either before or after each other
			// We want all pair-wise combinations of these items
			val underdetermined_l = orderingsMinMap.values.toList.filter(_.size > 1).flatMap(greater_l => greater_l.toList.combinations(2).toList).map { case List(a, b) => (a, b) }
			// For all pairs which may be before or after each other,
			// get two lists: one whose orderings correspond to actionOrdering_l, and one that doesn't
			val (desired_l, undesired_l) = underdetermined_l.map({ case is@(i1, i2) =>
				val j1 = actionOrdering_l.indexOf(i1)
				val j2 = actionOrdering_l.indexOf(i2)
				val pairOfPairs = (is, is.swap)
				if (j1 < j2) pairOfPairs else pairOfPairs.swap
			}).unzip
			// We'll try the desired orderings first, then the undesired ones
			println("desired_l: "+desired_l)
			val ordering0_l = desired_l ++ undesired_l
			val orderingAction_l = ordering0_l.flatMap { pair =>
				val (before_i, after_i) = pair
				val label = s"${before_i} < ${after_i}"
				plan.addOrdering(before_i, after_i) match {
					case Left(_) => None
					case Right(plan1) => out += label; Some(GroundAction(label, plan1))
				}
			}

			println("will try: "+out.toList.mkString(", "))
			println()

			logger.debug(plan.toString + " " + out.toList.mkString(", "))
			bindingAction_l ++ orderingAction_l
		}
		
		def childNode(parent: GroundNode, action: GroundAction): GroundNode =
			GroundNode(action.label, action.newplan, Some(parent), action.newplan.orderings.getMinimalMap)
	}
	
	/**
	 * Find a fully grounded plan, i.e. all variables assigned and fully ordered actions
	 */
	def groundPlan(plan: PartialPlan): Either[String, PartialPlan] = {
		val search = new ailib.ch03.TreeSearch[PartialPlan, GroundAction, GroundNode]
		val problem = new GroundProblem(plan)
		val frontier = new ailib.ch03.DepthFirstFrontier[PartialPlan, GroundNode]
		//val debug = new DebugSpec(true, false, false)
		val debug = new DebugSpec(true, true, true)
		search.run(problem, frontier, debug) match {
			case None => Left("Couldn't find ground plan")
			case Some(node) => Right(node.plan)
		}
	}

	/*
	def groundStep(gs: GroundState): Either[String, GroundState] = {
		gs match {
			case x: GroundState_Done => Right(x)
			case x: GroundState_SelectVariable => stepSelectVariable(x)
			case x: GroundState_ChooseVariableBinding => stepChooseVariableBinding(x)
			case x: GroundState_HandleVariable => stepHandleVariable(x)
			case x: GroundState_ChooseUnordered => stepChooseUnordered(x)
			case x: GroundState_HandleOrdering => stepHandleOrdering(x)
		}
	}
	
	def stepSelectVariable(x: GroundState_SelectVariable): Either[String, GroundState] = {
		x.plan.bindings.variable_m.headOption match {
			case None =>
				Right(GroundState_ChooseUnordered(x.plan, x.indentLevel))
			case Some((name, b)) => 
				Right(GroundState_ChooseVariableBinding(x.plan, name, b.option_l.toList.sorted, x.indentLevel + 1))
		}
	}
	
	def stepChooseVariableBinding(x: GroundState_ChooseVariableBinding): Either[String, GroundState] = {
		x.value_l match {
			case Nil => Left(s"Unable to find value for `${x.name}`")
			case value :: rest =>
				for {
					plan1 <- x.plan.addBindingEq(x.name, value).right
				} yield GroundState_SelectVariable(x.plan, x.indentLevel + 1)
				x.plan
		}
	}
	
	def groundPlan(plan: PartialPlan): Either[String, PartialPlan] = {
		@tailrec
		def loop(gs: GroundState): Either[String, PartialPlan] = {
			groundStep(gs) match {
				case Left(msg) => Left(msg)
				case Right(GroundState_Done(plan)) => Right(plan)
				case Right(step1) => loop(step1)
			}
		}
		loop(GroundState_SelectVariable(plan, 0))
	}*/
}
