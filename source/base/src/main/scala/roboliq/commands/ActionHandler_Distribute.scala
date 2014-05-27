package roboliq.commands

import grizzled.slf4j.Logger
import scala.Option.option2Iterable
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.plan.ActionHandler
import roboliq.plan.ActionPlanInfo
import spray.json.JsNull
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import roboliq.input.commands.ShakerRun
import roboliq.entities.EntityBase
import roboliq.entities.Shaker
import roboliq.input.Converter
import roboliq.entities.ShakerSpec
import roboliq.entities.Labware
import roboliq.entities.Site
import roboliq.input.commands.Command
import roboliq.plan.Instruction
import roboliq.entities.Agent
import roboliq.entities.WorldState
import roboliq.input.commands.Distribute
import roboliq.entities.PipetteSources
import roboliq.entities.PipetteDestinations
import roboliq.entities.LiquidVolume
import roboliq.entities.PipettePosition
import roboliq.entities.CleanIntensity
import roboliq.entities.WellIdentParser
import roboliq.input.commands.PipetteSpec
import roboliq.entities.TipModel
import roboliq.entities.Pipetter
import roboliq.entities.TipHandlingOverrides
import roboliq.input.commands.PipetterTipsRefresh
import roboliq.pipette.planners.PipetteHelper
import roboliq.entities.TipWellVolumePolicy
import roboliq.input.commands.PlanPath
import roboliq.pipette.planners.PipetteDevice
import roboliq.pipette.planners.TipModelSearcher0
import roboliq.input.commands.PipetterAspirate
import roboliq.input.commands.PipetterDispense
import roboliq.entities.Mixture
import roboliq.entities.PipettePolicy
import roboliq.pipette.planners.TransferSimplestPlanner
import scala.collection.immutable.SortedSet


object ActionHandler_Distribute {
	case class ActionParams(
		agent_? : Option[String],
		device_? : Option[String],
		source: String,
		destination: String
	)
	
	case class InstructionParams(
		agent_? : Option[String],
		device_? : Option[String],
		source: PipetteSources,
		destination: PipetteDestinations,
		volume: List[LiquidVolume],
		contact_? : Option[PipettePosition.Value],
		sterilize_? : Option[CleanIntensity.Value],
		sterilizeBefore_? : Option[CleanIntensity.Value],
		sterilizeBetween_? : Option[CleanIntensity.Value],
		sterilizeAfter_? : Option[CleanIntensity.Value],
		tipModel_? : Option[TipModel],
		pipettePolicy_? : Option[String]
	)
}

class ActionHandler_Distribute extends ActionHandler {
	import ActionHandler_Distribute._

	private val logger = Logger[this.type]
	
	def getActionName = "distribute1"

	def getActionParamNames = List("agent", "device", "source", "destination", "volume")

	private def getDomainOperator(n: Int): Strips.Operator = {
		val name = s"distribute$n"
		val paramName_l = "?agent" :: "?device" :: (1 to n).flatMap(i => List(s"?labware$i", s"?model$i", s"?site$i", s"?siteModel$i")).toList
		val paramTyp_l = "agent" :: "pipetter" :: List.fill(n)(List("labware", "model", "site", "siteModel")).flatten
		val preconds =
			Strips.Literal(true, "agent-has-device", "?agent", "?device") ::
			Strips.Literal(true, "device-can-site", "?device", "?site1") ::
			(1 to n).flatMap(i => List(
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
	
	def getActionPlanInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase
	): RqResult[ActionPlanInfo] = {
		for {
			params <- Converter.convActionAs[ActionParams](paramToJsval_l, eb)
			// TODO: handle reagent sources (in addition to these labware sources)
			parsedSource_l <- WellIdentParser.parse(params.source)
			parsedDestination_l <- WellIdentParser.parse(params.destination)
		} yield {
			val sourceLabware_l = parsedSource_l.map(_._1)
			val destinationLabware_l = parsedDestination_l.map(_._1)
			val labwareIdent_l = (sourceLabware_l ++ destinationLabware_l).distinct
			val n = labwareIdent_l.size
			val domainOperator = getDomainOperator(n)

			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = {
				"?agent" -> m.getOrElse("agent", "?agent") ::
				"?device" -> m.getOrElse("device", "?device") ::
				labwareIdent_l.zipWithIndex.map(pair => s"?labware${pair._2 + 1}" -> s"${pair._1}")
			}
			val binding = binding_l.toMap
			val planAction = domainOperator.bind(binding)

			ActionPlanInfo(id, paramToJsval_l, domainOperator, Nil, Nil, planAction)
		}
	}
	
	def getInstruction(
		planInfo: ActionPlanInfo,
		planned: Strips.Operator,
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[Instruction]] = {
		val m0 = planInfo.paramToJsval_l.toMap


		for {
			agent <- eb.getEntityAs[Agent](planned.paramName_l(0))
			device <- eb.getEntityAs[Pipetter](planned.paramName_l(1))
			params <- Converter.convInstructionAs[InstructionParams](planInfo.paramToJsval_l, eb, state0)
			spec = PipetteSpec(
				params.source,
				params.destination,
				params.volume,
				params.pipettePolicy_?,
				params.sterilize_?,
				params.sterilizeBefore_?,
				params.sterilizeBetween_?,
				params.sterilizeAfter_?,
				params.tipModel_?
			)
			path <- handlePipetteSpec(eb, state0, spec, device)
		} yield {
			path.action_r.reverse.map(x => Instruction(agent, x))
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
		val tipModel_l = tip_l.flatMap(eb.tipToTipModels_m).distinct
		
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
				spec.cleanBefore_?.orElse(spec.sterilize_?) match {
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
				val tipOverridesAsp = TipHandlingOverrides(None, spec.cleanAfter_?.orElse(spec.sterilize_?), None, None, None)
				PipetterTipsRefresh(pipetter, tip_l.map(tip => {
					val tipState = path2.state.getTipState(tip)
					val washSpec = PipetteHelper.choosePreAspirateWashSpec(tipOverridesAsp, Mixture.empty, tipState)
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
			val tipOverridesAsp = TipHandlingOverrides(None, spec.sterilizeBetween_?.orElse(spec.sterilize_?), None, None, None)
			val refresh = PipetterTipsRefresh(pipetter, batch.item_l.map(item => {
				val mixtureSrc = path.state.well_aliquot_m.get(item.src).map(_.mixture).getOrElse(Mixture.empty)
				val mixtureDst = path.state.well_aliquot_m.get(item.dst).map(_.mixture).getOrElse(Mixture.empty)
				val tipState = path.state.getTipState(item.tip)
				val washSpecAsp = PipetteHelper.choosePreAspirateWashSpec(tipOverridesAsp, mixtureSrc, tipState)
				val washSpecDis = PipetteHelper.choosePreDispenseWashSpec(tipOverridesAsp, mixtureSrc, mixtureDst, tipState)
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
