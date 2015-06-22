package roboliq.evoware.translator

import grizzled.slf4j.Logger
import roboliq.core.ResultC
import spray.json.JsObject
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.evoware.parser.CarrierSiteIndex
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import roboliq.input.JsConverter

/*
    objects:
      plate1: { type: Plate, model: ourlab.mario_model1, location: ourlab.mario_P1 }
      ourlab:
        type: Namespace
        mario: { type: EvowareRobot }
        mario_arm1: { type: Transporter, evowareName: ROMA1 }
        mario_P1: { type: Site, evowareGrid: 10, evowareSite: 2 }
        mario_P2: { type: Site, evowareGrid: 10, evowareSite: 4 }
        model1: { type: PlateModel, evowareName: D-BSSE 96 Well Plate }
    steps: [
      {command: instruction.transporter.movePlate, agent: ourlab.mario, equipment: ourlab.mario_arm1, program: Narrow, object: plate1, destination: ourlab.mario_P2},
      {set: {plate1: {location: ourlab.mario_P2}}},
      {command: instruction.transporter.movePlate, agent: ourlab.mario, equipment: ourlab.mario_arm1, program: Narrow, object: plate1, destination: ourlab.mario_P1},
      {set: {plate1: {location: ourlab.mario_P1}}}
    ]
 */

case class EvowareScript(
	index: Int,
	siteToModel_m: Map[CarrierSiteIndex, EvowareLabwareModel],
	cmd_l: Vector[String]
)

case class TranslationItem(
	token: String,
	siteToModel_l: Map[CarrierSiteIndex, EvowareLabwareModel]
)

private case class TranslationResult(
	item_l: List[TranslationItem],
	state1: JsObject
)

private object TranslationResult {
	def empty(state1: JsObject) = TranslationResult(Nil, state1)
}

class EvowareCompiler(
	agentName: String,
	config: EvowareConfig,
	handleUserInstructions: Boolean
) {
	private val logger = Logger[this.type]

	val script_l = new ArrayBuffer[EvowareScript]
	private var scriptIndex: Int = 0
	val cmds = new ArrayBuffer[String]
	val siteToModel_m = new HashMap[CarrierSiteIndex, EvowareLabwareModel]

	def buildScript(
		input: JsObject
	): ResultC[String] = {
		for {
			objects <- JsConverter.fromJs[JsObject](input, "objects")
			step_l <- JsConverter.fromJs[List[JsObject]](input, "steps")
		} yield ()
		???
	}
	
	private def handleStep(step: JsObject): ResultC[Unit] = {
		for {
			commandName_? <- JsConverter.fromJs[Option[String]](step, "command")
			agentName_? <- JsConverter.fromJs[Option[String]](step, "agent")
			set_? <- JsConverter.fromJs[Option[JsObject]](step, "set")
			_ <- (commandName_?, agentName_?, set_?) match {
				case ((Some(commandName), Some(agentName), None)) =>
					if (agentName == this.agentName) {
						handleAgentCommand(com) CONTINUE
					}
					else if (agentName == "user" && handleUserInstructions) {
						handleUserCommand(commandName, agentName)
					}
					else {
						ResultC.unit(())
					}
				case (None, None, Some(set)) =>
					???
				case _ => ResultC.unit(())
			}
		} yield ()
	}
}