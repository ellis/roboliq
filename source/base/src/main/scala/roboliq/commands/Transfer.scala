package roboliq.commands

import scala.collection.immutable.SortedSet
import scala.reflect.runtime.universe
import scala.runtime.ZippedTraversable3.zippedTraversable3ToTraversable
import aiplan.strips2.Strips
import aiplan.strips2.Strips._
import aiplan.strips2.Unique
import grizzled.slf4j.Logger
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.CleanIntensity
import roboliq.entities.EntityBase
import roboliq.entities.LiquidVolume
import roboliq.entities.Mixture
import roboliq.entities.PipetteDestinations
import roboliq.entities.PipettePolicy
import roboliq.entities.PipettePosition
import roboliq.entities.PipetteSources
import roboliq.entities.Pipetter
import roboliq.entities.TipHandlingOverrides
import roboliq.entities.TipModel
import roboliq.entities.TipWellVolumePolicy
import roboliq.entities.WellIdentParser
import roboliq.entities.WorldState
import roboliq.input.Converter
import roboliq.input.commands.PipetteSpec
import roboliq.input.commands.PipetterAspirate
import roboliq.input.commands.PipetterDispense
import roboliq.input.commands.PipetterTipsRefresh
import roboliq.input.commands.PlanPath
import roboliq.pipette.planners.PipetteDevice
import roboliq.pipette.planners.PipetteHelper
import roboliq.pipette.planners.TipModelSearcher0
import roboliq.pipette.planners.TransferPlanner.Item
import roboliq.pipette.planners.TransferSimplestPlanner
import roboliq.plan.ActionHandler
import roboliq.plan.AgentInstruction
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import roboliq.entities.WellInfo
import roboliq.entities.PipetteDestination
import roboliq.entities.LiquidSource


case class TransferActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source_? : Option[PipetteSources],
	destination_? : Option[PipetteDestinations],
	steps: List[TransferStepParams]
)

class TransferActionHandler extends ActionHandler {

	def getActionName = "transfer"

	def getActionParamNames = List("agent", "device", "source", "destination", "volume", "steps", "clean", "cleanBefore", "cleanBetween", "cleanAfter", "tipModel", "pipettePolicy")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[OperatorInfo] = {
		for {
			params <- Converter.convActionAs[TransferActionParams](paramToJsval_l, eb, state0)
		} yield {
			val sourceLabware_l = params.source_?.getOrElse(PipetteSources(Nil)).sources.flatMap(_.l.map(_.labwareName)) ++ params.steps.flatMap(_.s_?.map(_.l.map(_.labwareName)).getOrElse(Nil))
			val destinationLabware_l = params.destination_?.getOrElse(PipetteDestinations(Nil)).l.map(_.labwareName) ++ params.steps.map(_.d_?.map(_.wellInfo.labwareName).getOrElse(Nil))
			val labwareIdent_l = (sourceLabware_l ++ destinationLabware_l).distinct
			val n = labwareIdent_l.size

			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = {
				"?agent" -> m.getOrElse("agent", "?agent") ::
				"?device" -> m.getOrElse("device", "?device") ::
				labwareIdent_l.zipWithIndex.map(pair => s"?labware${pair._2 + 1}" -> s"${pair._1}")
			}
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, s"transfer$n", binding, paramToJsval_l.toMap)
		}
	}
}
/*
 * steps:
 * - {s: plate(A01), d: plate(A02), v: 20ul
 */

case class TransferInstructionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source_? : Option[PipetteSources],
	destination_? : Option[PipetteDestinations],
	volume : List[LiquidVolume],
	steps: List[TransferStepParams],
	contact_? : Option[PipettePosition.Value],
	clean_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel],
	pipettePolicy_? : Option[String]
)

case class TransferStepParams(
	s_? : Option[LiquidSource],
	d_? : Option[PipetteDestination],
	v_? : Option[LiquidVolume]/*,
	pipettePolicy_? : Option[String],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel]*/
)

class TransferOperatorHandler(n: Int) extends OperatorHandler {
	private val logger = Logger[this.type]
	
	def getDomainOperator: Strips.Operator = {
		val name = s"transfer$n"
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
			device <- eb.getEntityAs[Pipetter](operator.paramName_l(1))
			params <- Converter.convInstructionAs[TransferInstructionParams](instructionParam_m, eb, state0)
			spec <- getPipetteSpec(params)
			path <- handlePipetteSpec(eb, state0, spec, device)
		} yield {
			path.action_r.reverse.map(x => AgentInstruction(agent, x))
		}
	}
	
	private def getPipetteSpec(params: TransferInstructionParams): RqResult[PipetteSpec] = {
		for {
			destination_l <- getDestinations(params)
			source_l <- getSources(params)
			volume_l <- getVolumes(params)
			// TODO: Add warning if no destinations and sources are not provided
		} yield {
			PipetteSpec(
				PipetteSources(source_l),
				PipetteDestinations(destination_l),
				volume_l,
				params.pipettePolicy_?,
				params.clean_?,
				params.cleanBefore_?,
				params.cleanBetween_?,
				params.cleanAfter_?,
				params.tipModel_?
			)
		}
	}

	/**
	 * Get list of WellInfo destinations
	 */
	private def getDestinations(params: TransferInstructionParams): RqResult[List[WellInfo]] = {
		(params.destination_?, params.steps) match {
			case (None, Nil) => RqSuccess(Nil)
			case (None, step_l) =>
				RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
					RqResult.from(step.d_?.map(_.wellInfo), s"missing destination in step ${i+1}")
				}
			case (Some(destinations), Nil) => RqSuccess(destinations.l)
			case (Some(destinations), step_l) =>
				destinations.l match {
					case Nil =>
						RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
							RqResult.from(step.d_?.map(_.wellInfo), s"missing destination in step ${i+1}")
						}
					case one :: Nil =>
						RqSuccess(step_l.map(_.d_?.map(_.wellInfo).getOrElse(one)))
					case l =>
						for {
							_ <- RqResult.assert(l.size == step_l.size, "`destination` must either contain a single destination or a list the same length as `steps`")
						} yield (l zip step_l).map(pair => pair._2.d_?.map(_.wellInfo).getOrElse(pair._1))
				}
		}
	}
	
	/**
	 * Get list of LiquidSource
	 */
	private def getSources(params: TransferInstructionParams): RqResult[List[LiquidSource]] = {
		(params.source_?, params.steps) match {
			case (None, Nil) => RqSuccess(Nil)
			case (None, step_l) =>
				RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
					RqResult.from(step.s_?, s"missing source in step ${i+1}")
				}
			case (Some(sources), Nil) => RqSuccess(sources.sources)
			case (Some(sources), step_l) =>
				sources.sources match {
					case Nil =>
						RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
							RqResult.from(step.s_?, s"missing source in step ${i+1}")
						}
					case one :: Nil =>
						RqSuccess(step_l.map(_.s_?.getOrElse(one)))
					case l =>
						for {
							_ <- RqResult.assert(l.size == step_l.size, "`source` must either contain a single source or a list the same length as `steps`")
						} yield (l zip step_l).map(pair => pair._2.s_?.getOrElse(pair._1))
				}
		}
	}
	
	/**
	 * Get list of LiquidVolume
	 */
	private def getVolumes(params: TransferInstructionParams): RqResult[List[LiquidVolume]] = {
		(params.volume, params.steps) match {
			case (Nil, Nil) => RqSuccess(Nil)
			case (Nil, step_l) =>
				RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
					RqResult.from(step.v_?, s"missing volume in step ${i+1}")
				}
			case (volume_l, Nil) => RqSuccess(volume_l)
			case (volume_l, step_l) =>
				volume_l match {
					case Nil =>
						RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
							RqResult.from(step.v_?, s"missing volume in step ${i+1}")
						}
					case one :: Nil =>
						RqSuccess(step_l.map(_.v_?.getOrElse(one)))
					case l =>
						for {
							_ <- RqResult.assert(l.size == step_l.size, "`volume` must either contain a single source or a list the same length as `steps`")
						} yield (l zip step_l).map(pair => pair._2.v_?.getOrElse(pair._1))
				}
		}
	}

	private def handlePipetteSpec(
		eb: EntityBase,
		state0: WorldState,
		spec: PipetteSpec,
		device: Pipetter
	): RqResult[PlanPath] = {
		val path0 = new PlanPath(Nil, state0)
		val spec_l = spec.split
		def step(
			spec_l: List[PipetteSpec],
			path: PlanPath
		): RqResult[PlanPath] = {
			spec_l match {
				case Nil => RqSuccess(path)
				case spec :: rest =>
					for {
						path1 <- handlePipetteSpecSub(eb, path0, spec, device)
						path2 <- step(rest, path1)
					} yield path2
			}
		}
		val x = step(spec_l, path0)
		//println("pipetteSpecState:")
		//x.state.well_aliquot_m.filter(_._1.label.get.contains("C12")).foreach(println)
		x
	}
	
	private def handlePipetteSpecSub(
		eb: EntityBase,
		path0: PlanPath,
		spec: PipetteSpec,
		pipetter: Pipetter
	): RqResult[PlanPath] = {
		import roboliq.pipette.planners.TransferPlanner.{Item,BatchItem,Batch}
		
		val tip_l = eb.pipetterToTips_m(pipetter)
		val tipModel_l = spec.tipModel_? match {
			case Some(tipModel) => List(tipModel)
			case _ => tip_l.flatMap(eb.tipToTipModels_m).distinct
		}
		
		val device = new PipetteDevice
		//val tipModelSearcher = new TipModelSearcher1[Item, Mixture, TipModel]
		val tipModelSearcher = new TipModelSearcher0[Item, Mixture, TipModel]
		
		val source_l = spec.sources.sources match {
			case Nil => Nil
			case x :: Nil => List.fill(spec.destinations.l.length)(x)
			case x => x
		}
		
		val volume_l = spec.volume_l match {
			case Nil => Nil
			case x :: Nil => List.fill(spec.destinations.l.length)(x)
			case x => x
		}

		var path = path0
		
		//println("source_l: "+source_l)
		//println("volume_l: "+volume_l)
		//println("spec.destinations.l: "+spec.destinations.l)

		for {
			_ <- RqSuccess(()) // Dummy to let compile know that this should be an RqResult monad
			// sources for the liquid we want to transfer
			//src_l <- RsResult.toResultOfList(source_l.map(state.getWell))
			
			_ <- RqResult.assert(!source_l.isEmpty, "Source must be specified for pipetting")
			_ <- RqResult.assert(source_l.length == spec.destinations.l.length, "Must specify same number of sources and destinations")
			_ <- RqResult.assert(!volume_l.isEmpty, "Volumes must be specified for pipetting")
			_ <- RqResult.assert(volume_l.length == spec.destinations.l.length, "Same number of volumes must be specied as there are desination wells")
			
			// Create list of items for TransferPlanner
			item_l <- RqResult.toResultOfList((spec.destinations.l, source_l, volume_l).zipped.toList.map(tuple => {
				val (dst, src, volume) = tuple
				// FIXME: for debug only
				if (src.l.isEmpty) {
					println("src_l empty:")
					println(spec.destinations.l.length)
					println(source_l)
					println(volume_l)
					assert(!src.l.isEmpty)
				}
				// ENDFIX
				RqSuccess(Item(src.l.map(_.well), dst.well, volume))
			}))
			
			//_ = println("item_l: "+item_l)
			
			// Map of item to its source mixture
			itemToMixture_l <- RqResult.mapAll(item_l)(item => {
				RqResult.from(path0.state.well_aliquot_m.get(item.src_l.head).map(item -> _.mixture), s"no liquid specified in source well: ${item.src_l.head}")
			})
			itemToMixture_m = itemToMixture_l.toMap
			// TODO: need to track liquids in wells as we go along in case
			// a former destination well becomes a source well and doesn't have
			// the same liquid contents as in the initial state.
			itemToModels_m = for ((item, mixture) <- itemToMixture_m) yield {
				item -> device.getDispenseAllowableTipModels(tipModel_l, mixture, item.volume)
			}
			/* Have to use TipModelSearcher0 instead of 1 for 384 well plates
			// Choose a single tip model
			itemToTipModel_m <- tipModelSearcher.searchGraph(item_l, itemToMixture_m, itemToModels_m)
			// TODO: produce a warning if the user specified a tip model which isn't available for all items
			tipModelCandidate_l = spec.tipModel_? match {
				case Some(x) => Set(x)
				case None => itemToTipModel_m.toList.map(_._2).toSet
			} 
			_ <- RsResult.assert(tipModelCandidate_l.size == 1, "TransferPlanner can only handle a single tip model at a time")
			tipModel = tipModelCandidate_l.head
			*/
			//_ = println("volumes: "+item_l.map(_.volume))
			//_ = println("tipModels: ")
			//_ = itemToModels_m.values.foreach(println)
			tipModel <- tipModelSearcher.searchGraph(tipModel_l, itemToModels_m)
			// Filter for those tips which can be used with the tip model
			tipCandidate_l = tip_l.filter(tip => eb.tipToTipModels_m.get(tip).map(_.contains(tipModel)).getOrElse(false))
			// TODO: Need to choose pipette policy intelligently
			pipettePolicy_s = spec.pipettePolicy_?.getOrElse("POLICY")
			pipettePolicy = PipettePolicy(pipettePolicy_s, PipettePosition.getPositionFromPolicyNameHack(pipettePolicy_s))
			// Run transfer planner to get pippetting batches
			batch_l <- TransferSimplestPlanner.searchGraph(
				device,
				path0.state,
				SortedSet(tipCandidate_l : _*),
				tipModel, //itemToTipModel_m.head._2,
				pipettePolicy,
				item_l
			)
			
			// Refresh command before pipetting starts
			refreshBefore_l = {
				spec.cleanBefore_?.orElse(spec.clean_?) match {
					case Some(intensity) if intensity != CleanIntensity.None =>
						val tip_l = batch_l.flatMap(_.item_l.map(_.tip))
						PipetterTipsRefresh(pipetter, tip_l.map(tip => {
							(tip, intensity, Some(tipModel))
						})) :: Nil
					case _ => Nil
				}
			}
			path1 <- path0.add(refreshBefore_l)
	
			path2 <- getAspDis(path1, spec, pipetter, device, tipModel, pipettePolicy, batch_l)

			refreshAfter_l = {
				val tipOverrides = TipHandlingOverrides(None, spec.cleanAfter_?.orElse(spec.clean_?), None, None, None)
				PipetterTipsRefresh(pipetter, tip_l.map(tip => {
					val tipState = path2.state.getTipState(tip)
					val washSpec = PipetteHelper.choosePreAspirateWashSpec(tipOverrides, Mixture.empty, tipState)
					(tip, washSpec.washIntensity, None)
				})) :: Nil
			}
			path3 <- path2.add(refreshAfter_l)
		} yield path3
	}

	private def getAspDis(
		path0: PlanPath,
		spec: PipetteSpec,
		pipetter: Pipetter,
		device: PipetteDevice,
		tipModel: TipModel,
		pipettePolicy: PipettePolicy,
		batch_l: List[roboliq.pipette.planners.TransferSimplestPlanner.Batch]
	): RqResult[PlanPath] = {
		var path = path0
		// use the Batch list to create clean, aspirate, dispense commands
		logger.debug("batch_l: "+batch_l)
		batch_l.foreach(batch => {
			val tipOverrides = TipHandlingOverrides(None, spec.cleanBetween_?.orElse(spec.clean_?), None, None, None)
			val refresh = PipetterTipsRefresh(pipetter, batch.item_l.map(item => {
				val mixtureSrc = path.state.well_aliquot_m.get(item.src).map(_.mixture).getOrElse(Mixture.empty)
				val mixtureDst = path.state.well_aliquot_m.get(item.dst).map(_.mixture).getOrElse(Mixture.empty)
				val tipState = path.state.getTipState(item.tip)
				val washSpecAsp = PipetteHelper.choosePreAspirateWashSpec(tipOverrides, mixtureSrc, tipState)
				val washSpecDis = PipetteHelper.choosePreDispenseWashSpec(tipOverrides, mixtureSrc, mixtureDst, tipState)
				val washSpec = washSpecAsp + washSpecDis
				//logger.debug(s"refresh tipState: ${tipState} -> ${washSpec.washIntensity} -> ${tipState_~}")
				(item.tip, washSpec.washIntensity, Some(tipModel))
			}))
			path = path.add(refresh) match {
				case RqError(e, w) => return RqError(e, w)
				case RqSuccess(x, _) => x
			}
			
			val twvpAspToEvents0_l = batch.item_l.map(item => {
				TipWellVolumePolicy(item.tip, item.src, item.volume, pipettePolicy)
			})
			val twvpAsp_ll = device.groupSpirateItems(twvpAspToEvents0_l, path.state)
			val asp_l = twvpAsp_ll.map(PipetterAspirate)
			path = path.add(asp_l) match {
				case RqError(e, w) => return RqError(e, w)
				case RqSuccess(x, _) => x
			}

			val twvpDisToEvents0_l = batch.item_l.map(item => {
				TipWellVolumePolicy(item.tip, item.dst, item.volume, pipettePolicy)
			})
			val twvpDis_ll = device.groupSpirateItems(twvpDisToEvents0_l, path.state)
			val dis_l = twvpDis_ll.map(PipetterDispense)
			path = path.add(dis_l) match {
				case RqError(e, w) => return RqError(e, w)
				case RqSuccess(x, _) => x
			}
		})
		
		RqSuccess(path)
	}	
}
