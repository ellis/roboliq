package roboliq.translator.jshop

import roboliq.core._
import roboliq.entities._
import roboliq.commands._
import roboliq.input.Protocol
import roboliq.evoware.translator.EvowareScriptBuilder
import roboliq.input.PipetteSpec
import roboliq.pipette.planners.TransferPlanner
import roboliq.pipette.planners.PipetteDevice
import roboliq.pipette.planners.TipModelSearcher1
import scala.collection.immutable.SortedSet
import roboliq.commands.PipetterAspirate
import roboliq.input.PipetteSpecList

object JshopTranslator {
	
	def translate(
		protocol: Protocol,
		solution: String,
		agentToBuilder_m: Map[String, ClientScriptBuilder]
	): RqResult[Unit] = {
		val l = solution.split("\r?\n").toList
		val state0 = protocol.state0.toImmutable
		println(s"l: $l")
		def translateStep(line_l: List[String], state: WorldState): RsResult[Unit] = {
			line_l match {
				case Nil => RsSuccess(())
				case line :: rest =>
					translateLine(protocol, agentToBuilder_m, state, line.trim()).flatMap(state => translateStep(rest, state))
			}
		}

		for {
			_ <- translateStep(l, state0)
		} yield {
			// Let the builders know that we're done building
			agentToBuilder_m.values.foreach(_.end())
			()
		}
	}
	
	val RxOperator = """\(!(.*)\)""".r
	
	private def translateLine(protocol: Protocol, agentToBuilder_m: Map[String, ClientScriptBuilder], state0: WorldState, line: String): RsResult[WorldState] = {
		line match {
			case "" => RsSuccess(state0)
			case RxOperator(s) =>
				s.split(' ').toList match {
					case operator :: agentIdent :: arg_l =>
						val builder = agentToBuilder_m(agentIdent)
						def doit(state0: WorldState, command_l: List[Command]): RsResult[WorldState] = {
							command_l match {
								case Nil => RsSuccess(state0)
								case command :: rest =>
									val state1_? = builder.addCommand(
										protocol,
										state0,
										agentIdent,
										command
									)
									state1_?.flatMap(state1 => doit(state1, rest))
							}
						}
						for {
							command_l <- handleOperator(protocol, agentToBuilder_m, state0, operator, agentIdent, arg_l)
							state1 <- doit(state0, command_l)
						} yield state1
					
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
		state0: WorldState,
		operator: String,
		agentIdent: String,
		arg_l: List[String]
	): RsResult[List[Command]] = {
		operator match {
			case "agent-activate" => RsSuccess(List(AgentActivate()))
			case "agent-deactivate" => RsSuccess(List(AgentDeactivate()))
			case "log" =>
				val List(textIdent) = arg_l
				val text = protocol.idToObject(textIdent).toString
				RsSuccess(List(Log(text)))
			case "pipetter-run" =>
				val specIdent = arg_l(1)
				protocol.idToObject(specIdent) match {
					case spec: PipetteSpecList => {
						for {
							command_ll <- RsResult.toResultOfList(spec.step_l.map(spec2 => handleOperator_PipetteSpec(protocol, agentToBuilder_m, state0, spec2, arg_l)))
						} yield command_ll.flatten
					}
					case spec: PipetteSpec =>
						handleOperator_PipetteSpec(protocol, agentToBuilder_m, state0, spec, arg_l)
					case _ =>
						RsError("invalid PipetteSpec")
				}
			case "prompt" =>
				val List(textIdent) = arg_l
				val text = protocol.idToObject(textIdent).toString
				RsSuccess(List(Prompt(text)))
			
			case "sealer-run" =>
				val List(deviceIdent, specIdent, labwareIdent, siteIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Sealer](deviceIdent)
					_ <- protocol.eb.getEntityByIdent[SealerSpec](specIdent)
					_ <- protocol.eb.getEntityByIdent[Plate](labwareIdent)
					_ <- protocol.eb.getEntityByIdent[Site](siteIdent)
				} yield {
					List(SealerRun(deviceIdent, specIdent, labwareIdent, siteIdent))
				}
			
			case "thermocycler-close" =>
				val List(deviceIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Thermocycler](deviceIdent)
				} yield {
					List(ThermocyclerClose(deviceIdent))
				}
				
			case "thermocycler-open" =>
				val List(deviceIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Thermocycler](deviceIdent)
				} yield {
					List(ThermocyclerOpen(deviceIdent))
				}
				
			case "thermocycler-run" =>
				val List(deviceIdent, specIdent) = arg_l
				for {
					_ <- protocol.eb.getEntityByIdent[Thermocycler](deviceIdent)
					_ <- protocol.eb.getEntityByIdent[ThermocyclerSpec](specIdent)
				} yield {
					List(ThermocyclerRun(deviceIdent, specIdent))
				}
				
			case "transporter-run" =>
				RsSuccess(List(TransporterRun(
					deviceIdent = arg_l(0),
					labwareIdent = arg_l(1),
					modelIdent = arg_l(2),
					originIdent = arg_l(3),
					destinationIdent = arg_l(4),
					vectorIdent = arg_l(5)
				)))

			case _ =>
				RsError(s"unknown operator: $operator")
		}
	}
	
	private def handleOperator_PipetteSpec(
		protocol: Protocol,
		agentToBuilder_m: Map[String, ClientScriptBuilder],
		state0: WorldState,
		spec: PipetteSpec,
		arg_l: List[String]
	): RsResult[List[Command]] = {
		import roboliq.pipette.planners.TransferPlanner.{Item,BatchItem,Batch}
		
		// from the deviceIdent, we need to get a list of tips and tip models
		val deviceIdent = arg_l(0)
		val pipetter = protocol.eb.getEntity(deviceIdent).get.asInstanceOf[Pipetter]
		val tip_l = protocol.eb.pipetterToTips_m(pipetter)
		val tipModel_l = tip_l.flatMap(protocol.eb.tipToTipModels_m).distinct
		
		val device = new PipetteDevice
		val tipModelSearcher = new TipModelSearcher1[Item, Mixture, TipModel]
		
		for {
			// sources for the liquid we want to transfer
			src_l <- RsResult.toResultOfList(spec.source_l.map(state0.getWell))
			
			// Create list of items for TransferPlanner
			item_l <- RsResult.toResultOfList(spec.destination_l.map(dstKey => {
				state0.getWell(dstKey).map(dst => Item(src_l, dst, spec.volume))
			}))
			
			// Map of item to its source mixture
			itemToMixture_l <- RsResult.toResultOfList(item_l.map(item => {
				state0.well_aliquot_m.get(item.src_l.head).map(item -> _.mixture).asRs("no liquid specified in source well")
			}))
			itemToMixture_m = itemToMixture_l.toMap
			// TODO: need to track liquids in wells as we go along in case
			// a former destination well becomes a source well and doesn't have
			// the same liquid contents as in the initial state.
			itemToModels_m = for ((item, mixture) <- itemToMixture_m) yield {
				item -> device.getDispenseAllowableTipModels(tipModel_l, mixture, item.volume)
			}
			// Choose a single tip model
			itemToTipModel_m <- tipModelSearcher.searchGraph(item_l, itemToMixture_m, itemToModels_m)
			tipModelCandidate_l = itemToTipModel_m.toList.map(_._2).toSet
			_ <- RsResult.assert(tipModelCandidate_l.size == 1, "TransferPlanner can only handle a single tip model at a time")
			tipModel = tipModelCandidate_l.head
			// Filter for those tips which can be used with the tip model
			tipCandidate_l = tip_l.filter(tip => protocol.eb.tipToTipModels_m.get(tip).map(_.contains(tipModel)).getOrElse(false))
			// TODO: Need to choose pipette policy intelligently
			pipettePolicy_s = spec.pipettePolicy_?.getOrElse("POLICY")
			pipettePolicy = PipettePolicy(pipettePolicy_s, PipettePosition.getPositionFromPolicyNameHack(pipettePolicy_s))
			// Run transfer planner to get pippetting batches
			batch_l <- TransferPlanner.searchGraph(
				device,
				state0,
				SortedSet(tipCandidate_l : _*),
				itemToTipModel_m.head._2,
				pipettePolicy,
				item_l
			)
		} yield {
			// use the Batch list to create clean, aspirate, dispense commands
			println("batch_l: "+batch_l)
			batch_l.flatMap(batch => {
				val twvpAsp0_l = batch.item_l.map(item => {
					TipWellVolumePolicy(item.tip, item.src, item.volume, pipettePolicy)
				})
				val twvpAsp_ll = device.groupSpirateItems(twvpAsp0_l, state0)
				val asp_l = twvpAsp_ll.map(twvp_l => PipetterAspirate(twvp_l))

				val twvpDis0_l = batch.item_l.map(item => {
					TipWellVolumePolicy(item.tip, item.dst, item.volume, pipettePolicy)
				})
				val twvpDis_ll = device.groupSpirateItems(twvpDis0_l, state0)
				val dis_l = twvpDis_ll.map(twvp_l => PipetterDispense(twvp_l))
				
				asp_l ++ dis_l
			})
		}
	}
}
