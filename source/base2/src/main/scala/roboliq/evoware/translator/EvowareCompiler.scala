package roboliq.evoware.translator

import grizzled.slf4j.Logger
import roboliq.core.ResultC
import spray.json.JsObject
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.evoware.parser.CarrierSiteIndex
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import roboliq.input.JsConverter
import scala.reflect.runtime.universe.TypeTag

/*
    objects:
      plate1: { type: Plate, model: ourlab.mario_model1, location: ourlab.mario_P1 }
      ourlab:
        type: Namespace
        mario:
          type: Namespace
          evoware: { type: EvowareRobot }
          arm1: { type: Transporter, evowareRoma: 0 }
          P1: { type: Site, evowareCarrier: "some carrier", evowareGrid: 10, evowareSite: 2 }
          P2: { type: Site, evowareCarrier: "some carrier", evowareGrid: 10, evowareSite: 4 }
        model1: { type: PlateModel, evowareName: D-BSSE 96 Well Plate }
    steps: [
      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P2},
      {set: {plate1: {location: ourlab.mario.P2}}},
      {command: instruction.transporter.movePlate, agent: ourlab.mario.evoware, equipment: ourlab.mario.arm1, program: Narrow, object: plate1, destination: ourlab.mario.P1},
      {set: {plate1: {location: ourlab.mario.P1}}}
    ]
 */

case class EvowareScript(
	index: Int,
	siteToModel_m: Map[(Int, Int), EvowareLabwareModel],
	cmd_l: Vector[String]
)

case class TranslationItem(
	token: String,
	siteToModel_l: Map[(Int, Int), EvowareLabwareModel]
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
	//config: EvowareConfig,
	handleUserInstructions: Boolean
) {
	private val logger = Logger[this.type]

	val script_l = new ArrayBuffer[EvowareScript]
	private var scriptIndex: Int = 0
	val cmds = new ArrayBuffer[String]
	val siteToModel_m = new HashMap[CarrierSiteIndex, EvowareLabwareModel]

	def buildScript(
		input: JsObject
	): ResultC[List[Token]] = {
		for {
			objects <- JsConverter.fromJs[JsObject](input, "objects")
			step_l <- JsConverter.fromJs[List[JsObject]](input, "steps")
			token_l <- ResultC.map(step_l.zipWithIndex)(pair => ResultC.context("step "+(pair._2+1)) {
				handleStep(objects, pair._1)
			}).map(_.flatten)
		} yield token_l
	}
	
	private def handleStep(
		objects: JsObject,
		step: JsObject
	): ResultC[Option[Token]] = {
		println(s"handleStep: $step")
		for {
			commandName_? <- JsConverter.fromJs[Option[String]](step, "command")
			agentName_? <- JsConverter.fromJs[Option[String]](step, "agent")
			set_? <- JsConverter.fromJs[Option[JsObject]](step, "set")
			token_? <- (commandName_?, agentName_?, set_?) match {
				case ((Some(commandName), Some(agentName), None)) =>
					if (agentName == this.agentName) {
						handleCommand(objects, commandName, agentName, step)
					}
					else if (agentName == "user" && handleUserInstructions) {
						handleUserCommand(objects, commandName, step)
					}
					else {
						println("command ignored due to agent: "+step)
						ResultC.unit(None)
					}
				case (None, None, Some(set)) =>
					???
				case (None, None, None) =>
					ResultC.unit(None)
				case _ =>
					ResultC.error(s"don't know how to handle command=${commandName_?}, agent=${agentName_?}")
			}
			_ = println("token_?: "+token_?)
		} yield token_?
	}
	
	private def handleUserCommand(
		objects: JsObject,
		commandName: String,
		step: JsObject
	): ResultC[Option[Token]] = {
		commandName match {
			case _ =>
				ResultC.error(s"unhandled user command: $commandName in $step")
		}
	}
	
	private def handleCommand(
		objects: JsObject,
		commandName: String,
		agentName: String,
		step: JsObject
	): ResultC[Option[Token]] = {
		val map = Map[String, (JsObject, JsObject) => ResultC[Option[Token]]](
			"instruction.transporter.movePlate" -> handleTransporterMovePlate
		)
		map.get(commandName) match {
			case Some(fn) => fn(objects, step)
			case None =>
				ResultC.error(s"unknown command: $commandName in $step")
		}
	}
	
	def lookupAs[A : TypeTag](
		objects: JsObject,
		objectName: String,
		fieldName: String
	): ResultC[A] = {
		val field_l = (objectName+"."+fieldName).split('.').toList
		println(s"lookupAs($objectName, $fieldName): ${field_l}")
		JsConverter.fromJs[A](objects, field_l)
	}
	
	private def handleTransporterMovePlate(
		objects: JsObject,
		step: JsObject
	): ResultC[Option[Token]] = {
		println(s"handleTransporterMovePlate: $step")
		for {
			x <- JsConverter.fromJs[TransporterMovePlate](step)
			_ = println("A")
			romaIndex <- lookupAs[Int](objects, x.equipment, "evowareRoma")
			_ = println("B")
			programName <- ResultC.from(x.program_?, "required `program` parameter")
			_ = println("C")
			plateModelName <- lookupAs[String](objects, x.`object`, "model")
			_ = println("D")
			plateOrigName <- lookupAs[String](objects, x.`object`, "location")
			_ = println("E")
			plateOrigCarrierName <- lookupAs[String](objects, plateOrigName, "evowareCarrier")
			_ = println("F")
			plateOrigGrid <- lookupAs[Int](objects, plateOrigName, "evowareGrid")
			_ = println("G")
			plateOrigSite <- lookupAs[Int](objects, plateOrigName, "evowareSite")
			_ = println("H")
			plateDestCarrierName <- lookupAs[String](objects, x.destination, "evowareCarrier")
			_ = println("I")
			plateDestGrid <- lookupAs[Int](objects, x.destination, "evowareGrid")
			_ = println("J")
			plateDestSite <- lookupAs[Int](objects, x.destination, "evowareSite")
			_ = println("K")
		} yield {
			val bMoveBackToHome = x.evowareMoveBackToHome_? != Some(false) // 1 = move back to home position
			val line = List(
				s""""$plateOrigGrid"""",
				s""""$plateDestGrid"""",
				if (bMoveBackToHome) 1 else 0,
				0, //if (lidHandling == NoLid) 0 else 1,
				0, // speed: 0 = maximum, 1 = taught in vector dialog
				romaIndex,
				0, //if (lidHandling == RemoveAtSource) 1 else 0,
				"\"\"", //'"'+(if (lidHandling == NoLid) "" else iGridLid.toString)+'"',
				s""""$plateModelName"""",
				s""""$programName"""",
				"\"\"",
				"\"\"",
				s""""$plateOrigCarrierName"""",
				"\"\"", //'"'+sCarrierLid+'"',
				s""""$plateDestCarrierName"""",
				s""""${plateOrigSite+1}"""",
				"(Not defined)", // '"'+(if (lidHandling == NoLid) "(Not defined)" else (iSiteLid+1).toString)+'"',
				s""""${plateDestSite+1}""""
			).mkString("Transfer_Rack(", ",", ");")
			println(s"line: $line")
			Some(Token(line, Map((plateOrigGrid, plateOrigSite) -> plateModelName, (plateDestGrid, plateDestSite) -> plateModelName)))
		}
	}
}
