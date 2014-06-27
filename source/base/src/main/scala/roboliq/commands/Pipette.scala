package roboliq.commands

import roboliq.entities.PipetteDestination
import roboliq.entities.LiquidSource
import roboliq.entities.PipetteAmount
import roboliq.entities.CleanIntensity
import roboliq.entities.TipModel
import roboliq.entities.LiquidVolume
import roboliq.entities.PipetteDestinations
import roboliq.entities.PipetteSources
import roboliq.entities.WellInfo
import roboliq.core.RsResult
import roboliq.entities.PipetteAmount_Volume
import roboliq.entities.PipetteAmount_Dilution
import roboliq.core.RsError
import roboliq.core.RsSuccess
import scala.annotation.tailrec
import roboliq.entities.Well
import roboliq.entities.WorldState
import roboliq.entities.Pipetter
import roboliq.pipette.planners.PipetteDevice
import roboliq.entities.EntityBase
import roboliq.pipette.planners.TipModelSearcher0
import roboliq.entities.Mixture
import roboliq.entities.Tip
import roboliq.entities.PipettePolicy
import roboliq.entities.PipettePosition
import roboliq.pipette.planners.TransferSimplestPlanner
import roboliq.core.RqSuccess
import roboliq.core.RqResult
import roboliq.entities.WorldStateBuilder
import roboliq.entities.WorldStateEvent
import roboliq.core.RqError
import roboliq.entities.Aliquot
import roboliq.entities.Distribution
import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import roboliq.entities.TipHandlingOverrides
import roboliq.input.commands.PipetterTipsRefresh
import roboliq.input.commands.PlanPath
import roboliq.pipette.planners.PipetteHelper
import roboliq.entities.TipWellVolumePolicy
import roboliq.input.commands.PipetterAspirate
import roboliq.input.commands.PipetterDispense
import aiplan.strips2.Strips
import roboliq.plan.AgentInstruction
import roboliq.plan.OperatorHandler
import spray.json.JsValue
import grizzled.slf4j.Logger
import roboliq.entities.Agent
import aiplan.strips2.Unique
import roboliq.input.Converter
import roboliq.plan.AgentInstruction
import roboliq.plan.OperatorInfo
import spray.json.JsString
import roboliq.plan.ActionHandler

/**
 * @param cleanBegin_? Clean before pipetting starts
 * @param cleanBetween_? Clean between aspirations
 * @param cleanEnd_? Clean after pipetting ends
 */
case class PipetteActionParams(
	destination_? : Option[PipetteDestinations],
	source_? : Option[PipetteSources],
	amount: List[PipetteAmount],
	clean_? : Option[CleanIntensity.Value],
	cleanBegin_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanEnd_? : Option[CleanIntensity.Value],
	pipettePolicy_? : Option[String],
	tipModel_? : Option[TipModel],
	tip_? : Option[Int],
	steps: List[PipetteStepParams]
)

case class PipetteStepParams(
	d_? : Option[PipetteDestination],
	s_? : Option[LiquidSource],
	a_? : Option[PipetteAmount],
	pipettePolicy_? : Option[String],
	clean_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel],
	tip_? : Option[Int]
)

sealed abstract trait StepA
sealed abstract trait StepB
sealed abstract trait StepC

case class StepA_Pipette(
	s: LiquidSource,
	d: WellInfo,
	v: LiquidVolume,
	tipModel_? : Option[TipModel],
	pipettePolicy_? : Option[String],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tip_? : Option[Int]
) extends StepA

case class StepA_Clean(
	clean: CleanIntensity.Value
) extends StepA with StepB with StepC

case class StepB_Pipette(
	s: List[WellInfo],
	d: WellInfo,
	v: LiquidVolume,
	tipModel: TipModel,
	pipettePolicy: PipettePolicy,
	cleanBefore: CleanIntensity.Value,
	cleanAfter: CleanIntensity.Value,
	tip_l: List[Tip],
	mixtureSrc: Mixture,
	mixtureDst: Mixture
) extends StepB

case class StepC_Pipette(
	s: WellInfo,
	d: WellInfo,
	v: LiquidVolume,
	tipModel: TipModel,
	pipettePolicy: PipettePolicy,
	cleanBefore: CleanIntensity.Value,
	cleanAfter: CleanIntensity.Value,
	tip: Tip,
	mixtureSrc: Mixture,
	mixtureDst: Mixture
) extends StepC

class PipetteActionHandler extends ActionHandler {

	def getActionName = "pipette"

	def getActionParamNames = List("agent", "device", "destination", "source", "amount", "clean", "cleanBegin", "cleanBetween", "cleanEnd", "pipettePolicy", "tipModel", "tip", "steps")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[OperatorInfo] = {
		for {
			params <- Converter.convActionAs[PipetteActionParams](paramToJsval_l, eb, state0)
		} yield {
			val sourceLabware_l = (params.source_?.map(_.sources).getOrElse(Nil) ++ params.steps.flatMap(_.s_?)).flatMap(_.l.map(_.labwareName)) 
			val destinationLabware_l = (params.destination_?.map(_.l).getOrElse(Nil) ++ params.steps.flatMap(_.d_?).map(_.wellInfo)).map(_.labwareName)
			//println("sourceLabware_l: "+sourceLabware_l)
			//println("destinationLabware_l: "+destinationLabware_l)
			val labwareIdent_l = (sourceLabware_l ++ destinationLabware_l).distinct
			val n = labwareIdent_l.size

			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = {
				"?agent" -> m.getOrElse("agent", "?agent") ::
				"?device" -> m.getOrElse("device", "?device") ::
				labwareIdent_l.zipWithIndex.map(pair => s"?labware${pair._2 + 1}" -> s"${pair._1}")
			}
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, s"pipette$n", binding, paramToJsval_l.toMap)
		}
	}
}

class PipetteOperatorHandler(n: Int) extends OperatorHandler {
	private val logger = Logger[this.type]
	
	def getDomainOperator: Strips.Operator = {
		val name = s"pipette$n"
		val paramName_l = "?agent" :: "?device" :: (1 to n).flatMap(i => List(s"?labware$i", s"?model$i", s"?site$i", s"?siteModel$i")).toList
		val paramTyp_l = "agent" :: "pipetter" :: List.fill(n)(List("labware", "model", "site", "siteModel")).flatten
		val preconds =
			Strips.Literal(true, "agent-has-device", "?agent", "?device") ::
			Strips.Literal(Strips.Atom("ne", (1 to n).map(i => s"?site$i")), true) ::
			(1 to n).flatMap(i => List(
				Strips.Literal(true, "device-can-site", "?device", s"?site$i"),
				Strips.Literal(true, "model", s"?labware$i", s"?model$i"),
				Strips.Literal(true, "location", s"?labware$i", s"?site$i"),
				Strips.Literal(true, "model", s"?site$i", s"?siteModel$i"),
				Strips.Literal(true, "stackable", s"?siteModel$i", s"?model$i")
			)).toList

		Strips.Operator(
			name = name,
			paramName_l = paramName_l,
			paramTyp_l = paramTyp_l,
			preconds = Strips.Literals(Unique(preconds : _*)),
			effects = aiplan.strips2.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[AgentInstruction]] = {
		for {
			agent <- eb.getEntityAs[Agent](operator.paramName_l(0))
			pipetter <- eb.getEntityAs[Pipetter](operator.paramName_l(1))
			params <- Converter.convInstructionAs[PipetteActionParams](instructionParam_m, eb, state0)
			stepA_l <- paramsToA(params)
			stepB_l <- aToB(stepA_l, eb, state0, pipetter)
			stepC_ll <- bToC(params, stepB_l)
			device = new PipetteDevice
			instruction_l <- cToInstruction(state0, params, pipetter, device, stepC_ll)
		} yield {
			//println("stepA_l: "+stepA_l)
			//println("stepB_l: "+stepB_l)
			//println("stepC_ll: "+stepC_ll)
			instruction_l.map(instruction => AgentInstruction(agent, instruction))
		}
	}

	private def paramsToA(
		params: PipetteActionParams
	): RsResult[List[StepA]] = {
		val dn = params.destination_?.map(_.l.size).getOrElse(0)
		val sn = params.source_?.map(_.sources.size).getOrElse(0)
		val an = params.amount.size
		val stepn = params.steps.size
		val n_l = List(dn, sn, an, stepn)
		val n = n_l.max

		val d_l: List[Option[WellInfo]] = params.destination_? match {
			case None => List.fill(n)(None)
			case Some(x) => x.l.map(Some(_))
		}
		val s_l: List[Option[LiquidSource]] = params.source_? match {
			case None => List.fill(n)(None)
			case Some(x) => x.sources.map(Some(_))
		}
		val a_l: List[Option[PipetteAmount]] = params.amount match {
			case Nil => List.fill(n)(None)
			case x => x.map(Some(_))
		}
		val step_l: List[Option[PipetteStepParams]] = params.steps match {
			case Nil => List.fill(n)(None)
			case x => x.map(Some(_))
		}
		
		val all_l: List[((Option[WellInfo], Option[LiquidSource], Option[PipetteAmount]), Option[PipetteStepParams])] =
			(d_l, s_l, a_l).zipped.toList zip step_l
		
		for {
			// TODO: construct better error messages
			_ <- RsResult.mapFirst(n_l){x => RsResult.assert(x == 0 || x == 1 || x == n, "`destination`, `source`, `amount`, and `steps` lists must have compatible sizes")}
			stepA_l <- sub(params, d_l, s_l, a_l, step_l, 0, Nil)
		} yield stepA_l
	}
	
	@tailrec
	private def sub(
		params: PipetteActionParams,
		d_l: List[Option[WellInfo]],
		s_l: List[Option[LiquidSource]],
		a_l: List[Option[PipetteAmount]],
		step_l: List[Option[PipetteStepParams]],
		step_i: Int,
		stepA_r: List[StepA]
	): RsResult[List[StepA]] = {
		step_l match {
			case Nil => RsSuccess(stepA_r.reverse)
			case step_? :: step_rest =>
				val stepA_? = for {
					d <- RsResult.from(step_?.flatMap(_.d_?.map(_.wellInfo)).orElse(d_l.head), s"destination must be specified for step ${step_i+1}")
					s <- RsResult.from(step_?.flatMap(_.s_?).orElse(s_l.head), s"source must be specified for step ${step_i+1}")
					a <- RsResult.from(step_?.flatMap(_.a_?).orElse(a_l.head), s"amount must be specified for step ${step_i+1}")
					v <- a match {
						case PipetteAmount_Volume(volume) => RsSuccess(volume)
						case _ => RsError("INTERNAL: don't yet handle non-volume amounts")
					}
				} yield {
					StepA_Pipette(
						s,
						d,
						v,
						step_?.flatMap(_.tipModel_?).orElse(params.tipModel_?),
						step_?.flatMap(_.pipettePolicy_?).orElse(params.pipettePolicy_?),
						step_?.flatMap(_.cleanBefore_?).orElse(step_?.flatMap(_.clean_?)).orElse(params.cleanBetween_?).orElse(params.clean_?),
						step_?.flatMap(_.cleanAfter_?).orElse(step_?.flatMap(_.clean_?)).orElse(params.cleanBetween_?).orElse(params.clean_?),
						step_?.flatMap(_.tip_?).orElse(params.tip_?)
					)
				}
				stepA_? match {
					case RsError(e, w) => RsError(e, w)
					case RsSuccess(stepA, _) =>
						sub(params, d_l.tail, s_l.tail, a_l.tail, step_l.tail, step_i + 1, stepA :: stepA_r)
				}
		}
	}

	/**
	 * Split the list as necessary, then run aToB2() for each sublist
	 */
	private def aToB(
		stepA_l: List[StepA],
		eb: EntityBase,
		state0: WorldState,
		pipetter: Pipetter
	): RsResult[List[StepB]] = {
		def next(stepA_ll: List[List[StepA]], step_i: Int, state0: WorldState): RsResult[List[StepB]] = {
			stepA_ll match {
				case Nil => RsSuccess(Nil)
				case l :: rest =>
					for {
						ret1 <- aToB2(step_i, l, eb, state0, pipetter)
						(state1, l2) = ret1
						l3 <- next(rest, step_i + l.size, state1)
					} yield {
						l2 ++ l3
					}
			}
		}
		val stepA_ll = splitA(stepA_l)
		next(stepA_ll, 0, state0)
	}

	/**
	 * Split this list into multiple lists, whenever a prior destination well is used as a source well. 
	 */
	private def splitA(stepA_l: List[StepA]): List[List[StepA]] = {
		val i = splitIndex(stepA_l)
		if (i <= 0) {
			List(stepA_l)
		}
		else {
			val (left, right) = stepA_l.splitAt(i)
			left :: splitA(right)
		}
	}
	
	private def splitIndex(stepA_l: List[StepA]): Int = {
		var destSeen_l = Set[Well]()
		for ((stepA, i) <- stepA_l.zipWithIndex) {
			stepA match {
				case x: StepA_Pipette =>
					if (!x.s.l.map(_.well).toSet.intersect(destSeen_l).isEmpty)
						return i
					destSeen_l += x.d.well
				case _ =>
			}
		}
		-1
	}
	
	/**
	 * - for all pipette steps for which no tipModel is assigned, get the possible tipModels for that step
	 * - see if any tip model can be used for
	 *   - all steps
	 *   - all steps without explicit tipModel
	 *   - split steps by source, and try to assign tip model to each source
	 *   - for any remaining sources which couldn't be assigned a single tipModel, assign as best as possible
	 * - choose pipettePolicy
	 * - assign cleanBefore, cleanAfter, and tip set
	 * @param index0 this represents the index of stepA_l.head within the complete list of steps specified in the action
	 */
	private def aToB2(
		index0: Int,
		stepA_l: List[StepA],
		eb: EntityBase,
		state0: WorldState,
		pipetter: Pipetter
	): RsResult[(WorldState, List[StepB])] = {
		val device = new PipetteDevice
		val tipAll_l = eb.pipetterToTips_m(pipetter)
		val tipModelAll_l = tipAll_l.flatMap(eb.tipToTipModels_m).distinct
		val step1_l = stepA_l.collect { case x: StepA_Pipette => x }
		
		// TODO: FIXME: HACK: This is a temporary hack for 'getPolicy' -- need to define pipette policies in config file
		def getPolicy(step: StepA_Pipette, tipModel: TipModel): RsResult[PipettePolicy] = {
			step.pipettePolicy_? match {
				case Some(name) => RsSuccess(PipettePolicy(name, PipettePosition.getPositionFromPolicyNameHack(name)))
				case None => val name = "POLICY"; RsSuccess(PipettePolicy(name, PipettePosition.getPositionFromPolicyNameHack(name)))
			}
		}
		
		for {
			// Get possible tip models for each step
			ret1 <- processStepA1(eb, state0, tipAll_l, device, stepA_l, 0, Nil)
			(state1, stepToInfo1_m) = ret1
			stepToTipModels_m = stepToInfo1_m.mapValues(_._4)
			// Choose tip model for each step
			stepToTipModel_m <- chooseTipModel(tipModelAll_l, stepToTipModels_m)
			// Assign pipette policy for each step
			stepToPolicy_m <- RsResult.mapAll(stepToTipModel_m)(pair => getPolicy(pair._1, pair._2).map(pair._1 -> _)).map(_.toMap)
			// Now get StepB objects with pipettePolicy, cleanBefore, cleanAfter, tip set
			stepAToStepB_m <- processStepA2(eb, stepToInfo1_m, stepToTipModel_m, stepToPolicy_m)
		} yield {
			val stepB_l = stepA_l.map {
				case x: StepA_Pipette => stepAToStepB_m(x)
				case x: StepA_Clean => x
			}
			(state1, stepB_l)
		}
	}

	// Go through stepA_l and for each pipetting step, find the 4-tuple (source mixture, destination mixture (before dispense), possible tips, possible tip models)
	@tailrec
	private def processStepA1(
		eb: EntityBase,
		state: WorldState,
		tipAll_l: List[Tip],
		device: PipetteDevice,
		stepA_l: List[StepA],
		step_i: Int,
		acc_r: List[(StepA_Pipette, (Mixture, Mixture, List[Tip], List[TipModel]))]
	): RsResult[(WorldState, Map[StepA_Pipette, (Mixture, Mixture, List[Tip], List[TipModel])])] = {
		val (state2, acc2_r) = stepA_l match {
			case Nil =>
				return RsSuccess((state, acc_r.toMap))
			case (step: StepA_Pipette) :: _ =>
				val src = step.s.l.head.well
				val x = for {
					mixtureSrc <- RsResult.from(state.well_aliquot_m.get(src).map(_.mixture), s"step ${step_i + 1}: no liquid specified in source well: ${step.s.l.head}")
					mixtureDst = state.well_aliquot_m.get(step.d.well).map(_.mixture).getOrElse(Mixture.empty)
					// If a tip is explicitly assigned, use it, otherwise consider all available tips 
					tip_l <- step.tip_? match {
						case None => RsSuccess(tipAll_l)
						case Some(tip_i) =>
							tipAll_l.find(_.index == tip_i) match {
								case None => RsError(s"invalid tip index ${tip_i}")
								case Some(tip) => RsSuccess(List(tip))
							}
					}
					_ <- RsResult.assert(!tip_l.isEmpty, s"step ${step_i + 1}: no tips available")
					// Get tip models for the selected tips
					tipModelPossible_l = step.tipModel_? match {
						case None => tip_l.flatMap(eb.tipToTipModels_m).distinct
						case Some(tipModel) => List(tipModel)
					}
					// Get the subset valid tip models for this item's source mixture and volume
					tipModel_l = device.getDispenseAllowableTipModels(tipModelPossible_l, mixtureSrc, step.v)
					_ <- RsResult.assert(!tipModel_l.isEmpty, s"step ${step_i + 1}: no valid tip models founds")
					// Update state
					event_l = getWellEventsA(step)
					state1 <- WorldStateEvent.update(event_l, state)
				} yield {
					val item = step -> ((mixtureSrc, mixtureDst, tip_l, tipModel_l))
					(state1, item :: acc_r)
				}
				x match {
					case RsSuccess(y, _) => y
					case RsError(e, w) => return RsError(e, w)
				}
			case _ =>
				(state, acc_r)
		}
		processStepA1(eb, state2, tipAll_l, device, stepA_l.tail, step_i + 1, acc2_r)
	}
	
	private class WorldStateEventStepA(step: StepA_Pipette) extends WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = updateWorldStateA(step, state)
	}
	
	private def getWellEventsA(step: StepA_Pipette): List[WorldStateEvent] = List(new WorldStateEventStepA(step))

	private def updateWorldStateA(step: StepA_Pipette, state: WorldStateBuilder): RqResult[Unit] = {
		// Add liquid to destination
		val dst = step.d
		val src = step.s
		val volume = step.v
		val srcAliquot = state.well_aliquot_m.getOrElse(src.l.head.well, Aliquot.empty)
		val dstAliquot0 = state.well_aliquot_m.getOrElse(dst.well, Aliquot.empty)
		val amount = Distribution.fromVolume(volume)
		val aliquot = Aliquot(srcAliquot.mixture, amount)
		val x = for {
			dstAliquot1 <- dstAliquot0.add(aliquot)
		} yield {
			//println(s"update: ${dst.well.label} ${dstAliquot0} + ${aliquot} -> ${dstAliquot1}")
			state.well_aliquot_m(dst.well) = dstAliquot1
		}
		x match {
			case RqError(e, w) => return RqError(e, w)
			case _ =>
		}
		RqSuccess(())
	}
	
	/*
	 * - see if any tip model can be used for
	 *   - all steps
	 *   - all steps without explicit tipModel
	 *   - split steps by source, and try to assign tip model to each source
	 *   - for any remaining sources which couldn't be assigned a single tipModel, assign as best as possible
	 */
	private def chooseTipModel(
		tipModelAll_l: List[TipModel],
		stepToTipModels_m: Map[StepA_Pipette, List[TipModel]]
	): RsResult[Map[StepA_Pipette, TipModel]] = {
		// See whether any tipModels can be used for all steps
		val tipModelSearcher = new TipModelSearcher0[StepA_Pipette, TipModel]
		tipModelSearcher.searchGraph(tipModelAll_l, stepToTipModels_m) match {
			case RsSuccess(tipModel, _) => RsSuccess(stepToTipModels_m.mapValues(_ => tipModel))
			case _ =>
				val m = stepToTipModels_m.mapValues(_.head)
				RsSuccess(m, List("INTERNAL: not yet implemented to have more than one tip model per pipetting set, so setting the tip model individually for each step"))
		}
	}
	
	/**
	 * Using all the decisions we've gathered for each StepA so far, create the equivalent StepB objects
	 */
	private def processStepA2(
		eb: EntityBase,
		stepToInfo1_m: Map[StepA_Pipette, (Mixture, Mixture, List[Tip], List[TipModel])],
		stepToTipModel_m: Map[StepA_Pipette, TipModel],
		stepToPolicy_m: Map[StepA_Pipette, PipettePolicy]
	): RsResult[Map[StepA_Pipette, StepB_Pipette]] = {
		val stepAToStepB_m = stepToInfo1_m.map { case (stepA, (mixtureSrc, mixtureDst, tip_l, _)) =>
			val tipModel = stepToTipModel_m(stepA)
			val pipettePolicy = stepToPolicy_m(stepA)
			val cleanBefore = stepA.cleanBefore_?.getOrElse(CleanIntensity.max(mixtureSrc.tipCleanPolicy.enter, mixtureDst.tipCleanPolicy.enter))
			val cleanAfter = stepA.cleanAfter_?.getOrElse(
				if (pipettePolicy.pos == PipettePosition.Free) mixtureSrc.tipCleanPolicy.exit
				else CleanIntensity.max(mixtureSrc.tipCleanPolicy.exit, mixtureDst.tipCleanPolicy.exit)
			)
			val tip2_l = tip_l.filter(eb.tipToTipModels_m(_).contains(tipModel))
			val stepB = StepB_Pipette(
				stepA.s.l,
				stepA.d,
				stepA.v,
				tipModel,
				pipettePolicy,
				cleanBefore,
				cleanAfter,
				tip2_l,
				mixtureSrc,
				mixtureDst
			)
			stepA -> stepB
		}
		RsSuccess(stepAToStepB_m)
	}
	
	def bToC(
		params: PipetteActionParams,
		stepB_l: List[StepB]
	): RsResult[List[List[StepC]]] = {
		val sourceUsed_l = mutable.Queue[Well]()
		def pickNextSource(src_l: List[WellInfo]): WellInfo = {
			// Get index of these sources in the queue
			val index_l = src_l.map(wellInfo => sourceUsed_l.indexOf(wellInfo.well))
			// If any of the sources hasn't been used yet,
			val notyet = index_l.indexOf(-1)
			if (notyet != -1) {
				// return the first one of the unused sources
				src_l(notyet)
			}
			else {
				// otherwise, pick the source with the lowest index (hasn't been used for the longest)
				val l1 = src_l zip index_l
				val l2 = l1.sortBy(_._2)
				l2.head._1
			}
		}

		val tipUsed_l = mutable.Queue[Tip]()
		def pickNextTip(tip_l: List[Tip]): Tip = {
			// Get index of these sources in the queue
			val index_l = tip_l.map(tip => tipUsed_l.indexOf(tip))
			// If any of the sources hasn't been used yet,
			val notyet = index_l.indexOf(-1)
			if (notyet != -1) {
				// return the first one of the unused sources
				tip_l(notyet)
			}
			else {
				// otherwise, pick the source with the lowest index (hasn't been used for the longest)
				val l1 = tip_l zip index_l
				val l2 = l1.sortBy(_._2)
				l2.head._1
			}
		}

		val tipToCleanRequired_m = new HashMap[Tip, CleanIntensity.Value]
		val tipToMixture_m = new HashMap[Tip, Mixture]
		val tipHadWetDispense_l = new HashSet[Tip]
		val stepC_l = new ArrayBuffer[StepC]
		val stepC_ll = new ArrayBuffer[List[StepC]]
		
		def doClean(step_? : Option[StepA_Clean]) {
			tipToCleanRequired_m.clear
			tipToMixture_m.clear
			tipHadWetDispense_l.clear
			
			if (!stepC_l.isEmpty) {
				stepC_ll += stepC_l.toList
				stepC_l.clear
			}
			
			step_?.foreach(stepC_l += _)
		}
		
		// Simple grouping: Group steps together until a cleaning is required.
		// A cleaning is required when:
		// - an explicit clean command is encountered
		// - a tip is re-used and the user explicitly specifies `cleanBetween` as some value other than None
		// - a tip is re-used, `cleanBetween` is undefined, and previous tip dispense was wet contact
		// - a tip is re-used, `cleanBetween` is undefined, and previous tip aspirate was from a different mixture
		stepB_l.foreach {
			case stepB: StepB_Pipette =>
				val src = pickNextSource(stepB.s)
				sourceUsed_l.dequeueFirst(_.eq(src.well))
				sourceUsed_l += src.well
				
				val tip = pickNextTip(stepB.tip_l)
				tipUsed_l.dequeueFirst(_.eq(tip))
				tipUsed_l += tip
				
				val clean: Boolean = tipToCleanRequired_m.get(tip) match {
					case None => false
					case Some(CleanIntensity.None) => false
					// tip is about to be reused since last cleaning:
					case Some(clean0) =>
						// If use specifies what to do:
						if (params.cleanBetween_?.isDefined) {
							params.cleanBetween_? != Some(CleanIntensity.None)
						}
						// If tip had previous wet dispense or we're now aspirating from a different mixture
						else {
							tipHadWetDispense_l.contains(tip) || stepB.mixtureSrc != tipToMixture_m(tip)
						}
				}
				
				if (clean) {
					doClean(None)
				}
		
				val stepC = StepC_Pipette(
					src,
					stepB.d,
					stepB.v,
					stepB.tipModel,
					stepB.pipettePolicy,
					stepB.cleanBefore,
					stepB.cleanAfter,
					tip,
					stepB.mixtureSrc,
					stepB.mixtureDst
				)
				stepC_l += stepC
				
				tipToCleanRequired_m(tip) = stepB.cleanAfter
				tipToMixture_m(tip) = stepB.mixtureSrc
				tipHadWetDispense_l(tip) = (stepB.pipettePolicy.pos != PipettePosition.Free)
				
			case step: StepA_Clean =>
				doClean(Some(step))
		}
		
		doClean(None)
		
		RsSuccess(stepC_ll.toList)
	}
	
	private def cToInstruction(
		state0: WorldState,
		params: PipetteActionParams,
		pipetter: Pipetter,
		device: PipetteDevice,
		stepC_ll: List[List[StepC]]
	): RqResult[List[roboliq.input.commands.Action]] = {
		var path = new PlanPath(Nil, state0)
		//var state = state0
		//var instruction_l = new ArrayBuffer[roboliq.input.commands.Action]
		
		// use the Batch list to create clean, aspirate, dispense commands
		//logger.debug("batch_l: "+batch_l)
		val cleanBegin_? = params.cleanBegin_?.orElse(params.clean_?)
		val tip_l = SortedSet(stepC_ll.flatten.collect({ case x: StepC_Pipette => x.tip }) : _*).toList

		stepC_ll.foreach(stepC_l => {
			val pipetteC_l = stepC_l collect { case x: StepC_Pipette => x }
			val refresh_l = {
				// If this is the first instruction and the user specified cleanBegin or clean:
				if (path.action_r.isEmpty && cleanBegin_?.isDefined) {
					if (cleanBegin_? == CleanIntensity.None) {
						Nil
					}
					else {
						val tipToModel_m = new HashMap[Tip, TipModel]
						stepC_ll.flatten.collect({ case x: StepC_Pipette => x }).foreach { stepC =>
							if (!tipToModel_m.contains(stepC.tip)) {
								tipToModel_m(stepC.tip) = stepC.tipModel
							}
						}
						val refresh = PipetterTipsRefresh(pipetter, tip_l.map(tip => {
							(tip, cleanBegin_?.get, tipToModel_m.get(tip))
						}))
						List(refresh)
					}
				}
				else {
					val tipOverrides = TipHandlingOverrides(None, params.cleanBetween_?.orElse(params.clean_?), None, None, None)
					// TODO: FIXME: need to handle explicit StepA_Clean steps -- right now they are just ignored
					val refresh = PipetterTipsRefresh(pipetter, pipetteC_l.map(stepC => {
						val tipState = path.state.getTipState(stepC.tip)
						val washSpecAsp = PipetteHelper.choosePreAspirateWashSpec(tipOverrides, stepC.mixtureSrc, tipState)
						val washSpecDis = PipetteHelper.choosePreDispenseWashSpec(tipOverrides, stepC.mixtureSrc, stepC.mixtureDst, tipState)
						val washSpec = washSpecAsp + washSpecDis
						//logger.debug(s"refresh tipState: ${tipState} -> ${washSpec.washIntensity} -> ${tipState_~}")
						(stepC.tip, washSpec.washIntensity, Some(stepC.tipModel))
					}))
					List(refresh)
				}
			}
			
			path = path.add(refresh_l) match {
				case RqError(e, w) => return RqError(e, w)
				case RqSuccess(x, _) => x
			}
			
			val twvpAspToEvents0_l = pipetteC_l.map(stepC => {
				TipWellVolumePolicy(stepC.tip, stepC.s.well, stepC.v, stepC.pipettePolicy)
			})
			val twvpAsp_ll = device.groupSpirateItems(twvpAspToEvents0_l, path.state)
			val asp_l = twvpAsp_ll.map(PipetterAspirate)
			path = path.add(asp_l) match {
				case RqError(e, w) => return RqError(e, w)
				case RqSuccess(x, _) => x
			}

			val twvpDisToEvents0_l = pipetteC_l.map(stepC => {
				TipWellVolumePolicy(stepC.tip, stepC.d.well, stepC.v, stepC.pipettePolicy)
			})
			val twvpDis_ll = device.groupSpirateItems(twvpDisToEvents0_l, path.state)
			val dis_l = twvpDis_ll.map(PipetterDispense)
			path = path.add(dis_l) match {
				case RqError(e, w) => return RqError(e, w)
				case RqSuccess(x, _) => x
			}
		})

		val refreshAfter_l = {
			val tipOverrides = TipHandlingOverrides(None, params.cleanEnd_?.orElse(params.clean_?), None, None, None)
			PipetterTipsRefresh(pipetter, tip_l.map(tip => {
				val tipState = path.state.getTipState(tip)
				val washSpec = PipetteHelper.choosePreAspirateWashSpec(tipOverrides, Mixture.empty, tipState)
				(tip, washSpec.washIntensity, None)
			})) :: Nil
		}
		
		path = path.add(refreshAfter_l) match {
			case RqError(e, w) => return RqError(e, w)
			case RqSuccess(x, _) => x
		}
		
		RqSuccess(path.action_r.reverse)
	}
}
