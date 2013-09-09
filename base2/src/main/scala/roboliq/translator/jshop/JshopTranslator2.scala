package roboliq.translator.jshop

import roboliq.tokens.control.CommentToken
import roboliq.core._
import roboliq.entities._
import roboliq.input.Protocol
import roboliq.tokens.Token
import roboliq.tokens.control.PromptToken
import roboliq.tokens.transport.EvowareTransporterRunToken
import roboliq.evoware.translator.EvowareScriptBuilder
import roboliq.input.PipetteSpec
import roboliq.pipette.planners.TransferPlanner
import roboliq.pipette.planners.PipetteDevice
import roboliq.pipette.planners.TipModelSearcher1
import scala.collection.immutable.SortedSet

object JshopTranslator2 {
	
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
	
	def translateLine(protocol: Protocol, agentToBuilder_m: Map[String, ClientScriptBuilder], state0: WorldState, line: String): RsResult[WorldState] = {
		line match {
			case "" => RsSuccess(state0)
			case RxOperator(s) =>
				s.split(' ').toList match {
					case operation :: agentIdent :: arg_l =>
						// Pipetting operations need to be handled separately
						if (operation == "pipetter-run") {
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
											batch.item_l
										})
										// translate that list of commands for the given agent
										val builder = agentToBuilder_m(agentIdent)
										for {
											state <- builder.addOperation(
												protocol,
												state0,
												operation,
												agentIdent,
												arg_l
											)
										} yield state
										state0 // FIXME: return new state
									}
									//RsError("function incomplete")
								}
								case _ =>
									RsError("invalid PipetteSpec")
							}
						}
						// Non-pipetting operations can be passed as-is
						else {
							val builder = agentToBuilder_m(agentIdent)
							for {
								state <- builder.addOperation(
									protocol,
									state0,
									operation,
									agentIdent,
									arg_l
								)
							} yield state
						}
					case _ =>
						RsError(s"invalid operation line: $line")
				}
			case _ =>
				RsError(s"unrecognized line: "+line)
		}
	}
}