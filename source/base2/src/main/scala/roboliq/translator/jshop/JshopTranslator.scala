/*package roboliq.translator.jshop

import scala.collection.immutable.SortedSet
import grizzled.slf4j.Logger
import roboliq.core._
import roboliq.entities._
import roboliq.input.commands._
import roboliq.input.commands
import roboliq.input.Protocol
import roboliq.evoware.translator.EvowareScriptBuilder
import roboliq.pipette.planners.TransferPlanner
import roboliq.pipette.planners.PipetteDevice
import roboliq.pipette.planners.TipModelSearcher1
import roboliq.input.commands.PipetteSpecList
import roboliq.pipette.planners.PipetteHelper
import roboliq.entities.TipHandlingOverrides
import roboliq.pipette.planners.TransferSimplestPlanner
import roboliq.pipette.planners.TipModelSearcher0
import roboliq.pipette.planners.PipetteDevice

object JshopTranslator {
	
	private val logger = Logger[this.type]
	
	def translate(
		protocol: Protocol,
		solution: String
	): RqResult[WorldState] = {
		val agentToBuilder_m = protocol.agentToBuilder_m.toMap
		val l = solution.split("\r?\n").toList
		val path0 = new PlanPath(Nil, protocol.state0.toImmutable)
		//val state0 = protocol.state0.toImmutable
		logger.debug(s"l: $l")
		def translateAll(line_l: List[String], path: PlanPath): RsResult[WorldState] = {
			line_l match {
				case Nil =>
					//path.print
					RsSuccess(path.state)
				case line :: rest =>
					translateLine(protocol, agentToBuilder_m, path, line.trim()).flatMap(path1 => translateAll(rest, path1))
			}
		}

		for {
			state1 <- translateAll(l, path0)
		} yield {
			// Let the builders know that we're done building
			agentToBuilder_m.values.foreach(_.end())
			state1
		}
	}
	
	val RxOperator = """\(!(.*)\)""".r
	
	private def translateLine(
		protocol: Protocol,
		agentToBuilder_m: Map[String, ClientScriptBuilder],
		path0: PlanPath,
		line: String
	): RqResult[PlanPath] = {
		line match {
			case "" => RsSuccess(path0)
			case RxOperator(s) =>
				println("s: "+s)
				s.split(' ').toList match {
					case "nop" :: specIdent :: Nil =>
	                    protocol.idToObject(specIdent) match {
                            case spec: commands.SetReagents =>
                            	handleOperator_SetReagents(protocol, agentToBuilder_m, path0, spec)
                            case _ =>
                            	RsError("invalid SetReagents")
                        }
					case operator :: agentIdent :: arg_l =>
						//val builder = agentToBuilder_m(agentIdent)
						/*def doit(
							path: PlanPath,
							commandToState_l: List[(Command, List[WorldStateEvent])]
						): RsResult[PlanPath] = {
							commandToState_l match {
								case Nil => RqSuccess(path)
								case (command, )
							}
							commandToState_l.foldLeft(RsSuccess(state0) : RsResult[WorldState]) { (state0_?, pair) =>
								val (command, event1_l) = pair
								for {
									state0 <- state0_?
									state1 <- builder.addCommand(
													protocol,
													state0,
													agentIdent,
													command
												)
									state2 <- WorldStateEvent.update(event1_l, state1)
								} yield state2
							}
						}*/
						for {
							path1 <- handleOperator(protocol, agentToBuilder_m, path0, operator, agentIdent, arg_l)
							//state1 <- doit(state0, commandToEvents_l)
						} yield path1
					
					case _ =>
						RsError(s"invalid operation line: $line")
				}
			case _ =>
				RsError(s"unrecognized line: "+line)
		}
	}
	
	/**
	 * Translate an operator to a Command
	 */
	private def handleOperator(
		protocol: Protocol,
		agentToBuilder_m: Map[String, ClientScriptBuilder],
		path0: PlanPath,
		operator: String,
		agentIdent: String,
		arg_l: List[String]
	): RsResult[PlanPath] = {
		val action_l_? : RqResult[List[Action]] = operator match {
			case "agent-activate" => RqSuccess(List(AgentActivate()))
			case "agent-deactivate" => RqSuccess(List(AgentDeactivate()))
			case "log" =>
				val List(textIdent) = arg_l
				val text = protocol.idToObject(textIdent).toString
				RqSuccess(List(Log(text)))
			
			case "peeler-run" =>
				val List(deviceIdent, specIdent, labwareIdent, siteIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Peeler](deviceIdent)
					_ <- protocol.eb.getEntityByIdent[PeelerSpec](specIdent)
					_ <- protocol.eb.getEntityByIdent[Plate](labwareIdent)
					_ <- protocol.eb.getEntityByIdent[Site](siteIdent)
				} yield List(PeelerRun(deviceIdent, specIdent, labwareIdent, siteIdent))

			case "pipetter-run" =>
				// TODO: the details of how to handle pipetting depends on the agent.
				// For the user agent, we should have a single command, whereas for Evoware, we need to do a lot of processing.
				// Possibly take that into consideration, and let the builder process the operator?
				val specIdent = arg_l(1)
				protocol.idToObject(specIdent) match {
					case spec: PipetteSpecList => {
						//println("spec list:")
						//spec.step_l.foreach(println)
						def step(step_l: List[PipetteSpec], path0: PlanPath): RqResult[PlanPath] = {
							step_l match {
								case Nil => RqSuccess(path0)
								case spec :: rest =>
									for {
										path1 <- handleOperator_PipetteSpec(protocol, agentToBuilder_m, path0, spec, arg_l)
										//.map(combineTipsRefreshCommands)
										path2 <- step(rest, path1)
									} yield path2
							}
						}
						val pathEmpty = new PlanPath(Nil, path0.state)
						step(spec.step_l, pathEmpty).map(_.action_r.reverse)
					}
					case spec: PipetteSpec =>
						for {
							path1 <- handleOperator_PipetteSpec(protocol, agentToBuilder_m, path0, spec, arg_l)
						} yield path1.action_r.reverse
						//.map(combineTipsRefreshCommands)
					case _ =>
						RsError("invalid PipetteSpec")
				}
				
			case "prompt" =>
				val List(textIdent) = arg_l
				val text = protocol.idToObject(textIdent).toString
				RqSuccess(List(Prompt(text)))
			
			case "sealer-run" =>
				val List(deviceIdent, specIdent, labwareIdent, siteIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Sealer](deviceIdent)
					_ <- protocol.eb.getEntityByIdent[SealerSpec](specIdent)
					_ <- protocol.eb.getEntityByIdent[Plate](labwareIdent)
					_ <- protocol.eb.getEntityByIdent[Site](siteIdent)
				} yield List(SealerRun(deviceIdent, specIdent, labwareIdent, siteIdent))
				
			case "shaker-run" =>
				val List(deviceIdent, specIdent, labwareIdent, siteIdent) = arg_l
				for {
					device <- protocol.eb.getEntityByIdent[Shaker](deviceIdent)
					spec <- protocol.eb.getEntityByIdent[ShakerSpec](specIdent)
					labware <- protocol.eb.getEntityByIdent[Labware](labwareIdent)
					site <- protocol.eb.getEntityByIdent[Site](siteIdent)
				} yield List(ShakerRun(device, spec, List(labware -> site)))
			
			case "thermocycler-close" =>
				val List(deviceIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Thermocycler](deviceIdent)
				} yield List(ThermocyclerClose(deviceIdent))
				
			case "thermocycler-open" =>
				val List(deviceIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Thermocycler](deviceIdent)
				} yield List(ThermocyclerOpen(deviceIdent))
				
			case "thermocycler-run" =>
				//val List(deviceIdent, specIdent, plateIdent) = arg_l
				val List(deviceIdent, specIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Thermocycler](deviceIdent)
					_ <- protocol.eb.getEntityByIdent[ThermocyclerSpec](specIdent)
					//_ <- protocol.eb.getEntityByIdent[Plate](plateIdent)
				} yield List(ThermocyclerRun(deviceIdent, specIdent/*, plateIdent*/))
				
			case "transporter-run" =>
				val List(deviceIdent, labwareIdent, modelIdent, originIdent, destinationIdent, vectorIdent) = arg_l
				for {
					labware <- protocol.eb.getEntityByIdent[Labware](labwareIdent)
					labwareModel = protocol.eb.labwareToModel_m(labware)
					origin <- protocol.eb.getEntityByIdent[Site](originIdent)
					destination <- protocol.eb.getEntityByIdent[Site](destinationIdent)
				} yield {
					List(TransporterRun(
						deviceIdent = deviceIdent,
						labware = labware,
						model = labwareModel,
						origin = origin,
						destination = destination,
						vectorIdent = vectorIdent
					))
				}

			case _ =>
				RsError(s"unknown operator: $operator")
		}
		
		val builder = agentToBuilder_m(agentIdent)

		def step(action_l: List[Action], path0: PlanPath): RqResult[PlanPath] = {
			action_l.foldLeft(RqSuccess(path0) : RqResult[PlanPath]) { (path0_?, action) =>
				val command = action.asInstanceOf[Command]
				for {
					path0 <- path0_?
					path1 <- path0.add(action)
					_ <- builder.addCommand(protocol, path0.state, agentIdent, command)
				} yield path1
			}
		}
		
		action_l_?.flatMap(action_l => step(action_l, path0))
	}
	
	private def handleOperator_SetReagents(
		protocol: Protocol,
		agentToBuilder_m: Map[String, ClientScriptBuilder],
		path0: PlanPath,
		spec: commands.SetReagents
	): RqResult[PlanPath] = {
		val builder = path0.state.toMutable
		val l = for ((wellInfo, reagentName) <- (spec.wells.l zip spec.reagents)) yield {
			val aliquot0 = builder.well_aliquot_m.getOrElse(wellInfo.well, Aliquot.empty)
			val substance = Substance(
				key = reagentName,
				label = Some(reagentName),
				description = None,
				kind = SubstanceKind.Liquid,
				tipCleanPolicy = TipCleanPolicy.TT,
				contaminants = Set(),
				costPerUnit_? = None,
				valuePerUnit_? = None,
				molarity_? = None,
				gramPerMole_? = None,
				celciusAndConcToViscosity = Nil,
				sequence_? = None
			)
			val mixture = Mixture(Left(substance))
			val aliquot = Aliquot(mixture, aliquot0.distribution)
			builder.well_aliquot_m(wellInfo.well) = aliquot
		}
		builder.well_aliquot_m.foreach(println)
		RqSuccess(new PlanPath(path0.action_r, builder.toImmutable))
	}
	
	private def handleOperator_PipetteSpec(
		protocol: Protocol,
		agentToBuilder_m: Map[String, ClientScriptBuilder],
		path0: PlanPath,
		spec: PipetteSpec,
		arg_l: List[String]
	): RqResult[PlanPath] = {
		val spec_l = spec.split
		def step(
			spec_l: List[PipetteSpec],
			path: PlanPath
		): RsResult[PlanPath] = {
			spec_l match {
				case Nil => RqSuccess(path)
				case spec :: rest =>
					for {
						path1 <- handleOperator_PipetteSpecSub(protocol, agentToBuilder_m, path, spec, arg_l)
						path2 <- step(rest, path1)
					} yield path2
			}
		}
		val x = step(spec_l, path0)
		//println("pipetteSpecState:")
		//x.state.well_aliquot_m.filter(_._1.label.get.contains("C12")).foreach(println)
		x
	}
	
	private def handleOperator_PipetteSpecSub(
		protocol: Protocol,
		agentToBuilder_m: Map[String, ClientScriptBuilder],
		path0: PlanPath,
		spec: PipetteSpec,
		arg_l: List[String]
	): RqResult[PlanPath] = {
		import roboliq.pipette.planners.TransferPlanner.{Item,BatchItem,Batch}
		
		// from the deviceIdent, we need to get a list of tips and tip models
		val deviceIdent = arg_l(0)
		val pipetter = protocol.eb.getEntity(deviceIdent).get.asInstanceOf[Pipetter]
		val tip_l = protocol.eb.pipetterToTips_m(pipetter)
		val tipModel_l = tip_l.flatMap(protocol.eb.tipToTipModels_m).distinct
		
		val device = new PipetteDevice
		//val tipModelSearcher = new TipModelSearcher1[Item, Mixture, TipModel]
		val tipModelSearcher = new TipModelSearcher0[Item, TipModel]
		
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
			item_l <- RsResult.toResultOfList((spec.destinations.l, source_l, volume_l).zipped.toList.map(tuple => {
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
			itemToMixture_l <- RsResult.toResultOfList(item_l.map(item => {
				path0.state.well_aliquot_m.get(item.src_l.head).map(item -> _.mixture).asRs(s"no liquid specified in source well: ${item.src_l.head}")
			}))
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
			tipCandidate_l = tip_l.filter(tip => protocol.eb.tipToTipModels_m.get(tip).map(_.contains(tipModel)).getOrElse(false))
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
				val tipOverridesAsp = TipHandlingOverrides(None, spec.cleanAfter_?.orElse(spec.clean_?), None, None, None)
				PipetterTipsRefresh(pipetter, tip_l.map(tip => {
					val tipState = path2.state.getTipState(tip)
					val washSpec = PipetteHelper.choosePreAspirateWashSpec(tipOverridesAsp, Mixture.empty, tipState, None)
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
			val tipOverridesAsp = TipHandlingOverrides(None, spec.cleanBetween_?.orElse(spec.clean_?), None, None, None)
			val refresh = PipetterTipsRefresh(pipetter, batch.item_l.map(item => {
				val mixtureSrc = path.state.well_aliquot_m.get(item.src).map(_.mixture).getOrElse(Mixture.empty)
				val mixtureDst = path.state.well_aliquot_m.get(item.dst).map(_.mixture).getOrElse(Mixture.empty)
				val tipState = path.state.getTipState(item.tip)
				val washSpecAsp = PipetteHelper.choosePreAspirateWashSpec(tipOverridesAsp, mixtureSrc, tipState, None)
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
	
	/*private def combineTipsRefreshCommands(
		commandToState_l: List[(Command, List[WorldStateEvent])]
	): List[(Command, List[WorldStateEvent])] = {
		def doit(
			l: List[(Command, List[WorldStateEvent])],
			acc_r: List[(Command, List[WorldStateEvent])]
		): List[(Command, List[WorldStateEvent])] = {
			l.span(_._1.isInstanceOf[PipetterTipsRefresh]) match {
				case (Nil, Nil) =>
					acc_r.reverse
				case (Nil, x :: rest) =>
					doit(rest, x :: acc_r)
				case (x :: Nil, rest) =>
					doit(rest, x :: acc_r)
				case (l1, rest) =>
					val (l2, state_l) = l1.unzip
					val l3 = l2.asInstanceOf[List[PipetterTipsRefresh]]
					val l4 = PipetterTipsRefresh.combine(l3)
					doit(rest, (l4 zip state_l).reverse ++ acc_r)
			}
		}
		doit(commandToState_l, Nil)
	}*/
}
*/