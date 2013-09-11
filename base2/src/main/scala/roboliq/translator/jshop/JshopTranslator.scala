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
					case spec: PipetteSpec => {
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
							itemToTipModel_m <- tipModelSearcher.searchGraph(item_l, itemToMixture_m, itemToModels_m)
							// Run transfer planner to get pippetting batches
							batch_l <- TransferPlanner.searchGraph(
								device,
								state0,
								SortedSet(tip_l : _*),
								itemToTipModel_m.head._2,
								PipettePolicy("POLICY", PipettePosition.Free),
								item_l
							)
						} yield {
							// use the Batch list to create clean, aspirate, dispense commands
							println("batch_l: "+batch_l)
							batch_l.flatMap(batch => {
								val twvpAsp_l = batch.item_l.map(item => {
									TipWellVolumePolicy(item.tip, item.src, item.volume, PipettePolicy("POLICY", PipettePosition.Free))
								})
								val twvpDis_l = batch.item_l.map(item => {
									TipWellVolumePolicy(item.tip, item.dst, item.volume, PipettePolicy("POLICY", PipettePosition.Free))
								})
								List(PipetterAspirate(twvpAsp_l), PipetterDispense(twvpDis_l))
							})
						}
					}
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
}
