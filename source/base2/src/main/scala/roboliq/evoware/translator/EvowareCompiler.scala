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
import roboliq.evoware.parser.CarrierNameGridSiteIndex
import roboliq.evoware.parser.EvowareTableData

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
	line_l: Vector[String],
	siteToNameAndModel_m: Map[CarrierNameGridSiteIndex, (String, String)]
)

class EvowareCompiler(
	agentName: String,
	//config: EvowareConfig,
	handleUserInstructions: Boolean
) {
	private val logger = Logger[this.type]

	/*val script_l = new ArrayBuffer[EvowareScript]
	private var scriptIndex: Int = 0
	val cmds = new ArrayBuffer[String]
	val siteToModel_m = new HashMap[CarrierSiteIndex, EvowareLabwareModel]*/

	def buildTokens(
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
	
	def buildScripts(
		token_l: List[Token]
	): List[EvowareScript] = {
		val script_l = new ArrayBuffer[EvowareScript]()
		val map = new HashMap[CarrierNameGridSiteIndex, (String, String)]()
		var index = 1
		var line_l = Vector[String]()
		for (token <- token_l) {
			val conflict = token.siteToNameAndModel_m.exists(pair => map.get(pair._1) match {
				case None => false
				case Some(s) => s != pair._2
			})
			if (conflict) {
				val script = new EvowareScript(index, line_l, map.toMap)
				script_l += script
				index += 1
				line_l = Vector()
				map.clear
			}
			line_l +:= token.line
			map ++= token.siteToNameAndModel_m
		}
		if (!line_l.isEmpty) {
			val script = new EvowareScript(index, line_l, map.toMap)
			script_l += script
		}
		script_l.toList
	}

	def generateScriptContents(
		tableData: EvowareTableData,
		basename: String,
		script_l: List[EvowareScript]
	): ResultC[List[(String, Array[Byte])]] = {
		ResultC.map(script_l) { script =>
			val filename = basename + (if (script.index <= 1) "" else f"_${script.index}%02d") + ".esc"
			//logger.debug("generateScripts: filename: "+filename)
			for {
				bytes <- generateWithHeader(tableData, script)
			} yield filename -> bytes
		}
	}
	
	private def generateWithHeader(
		tableData: EvowareTableData,
		script: EvowareScript
	): ResultC[Array[Byte]] = {
		for {
			sHeader <- tableData.toStringWithLabware(script.siteToNameAndModel_m)
		} yield {
			val sCmds = script.line_l.mkString("\n")
			val os = new java.io.ByteArrayOutputStream()
			writeLines(os, sHeader)
			writeLines(os, sCmds);
			os.toByteArray()
		}
	}
	
	private def writeLines(output: java.io.OutputStream, s: String) {
		val as = s.split("\r?\n")
		for (sLine <- as if !s.isEmpty) {
			val bytes = sLine.map(_.asInstanceOf[Byte]).toArray
			output.write(bytes)
			output.write("\r\n".getBytes())
		}
	}
	
	private def handleStep(
		objects: JsObject,
		step: JsObject
	): ResultC[Option[Token]] = {
		//println(s"handleStep: $step")
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
			//_ = println("token_?: "+token_?)
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
		//println(s"lookupAs($objectName, $fieldName): ${field_l}")
		JsConverter.fromJs[A](objects, field_l)
	}
	
	private def handleTransporterMovePlate(
		objects: JsObject,
		step: JsObject
	): ResultC[Option[Token]] = {
		//println(s"handleTransporterMovePlate: $step")
		for {
			x <- JsConverter.fromJs[TransporterMovePlate](step)
			romaIndex <- lookupAs[Int](objects, x.equipment, "evowareRoma")
			programName <- ResultC.from(x.program_?, "required `program` parameter")
			plateModelName0 <- lookupAs[String](objects, x.`object`, "model")
			plateModelName <- lookupAs[String](objects, plateModelName0, "evowareName")
			plateOrigName <- lookupAs[String](objects, x.`object`, "location")
			plateOrigCarrierName <- lookupAs[String](objects, plateOrigName, "evowareCarrier")
			plateOrigGrid <- lookupAs[Int](objects, plateOrigName, "evowareGrid")
			plateOrigSite <- lookupAs[Int](objects, plateOrigName, "evowareSite")
			plateDestName = x.destination
			plateDestCarrierName <- lookupAs[String](objects, plateDestName, "evowareCarrier")
			plateDestGrid <- lookupAs[Int](objects, x.destination, "evowareGrid")
			plateDestSite <- lookupAs[Int](objects, x.destination, "evowareSite")
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
			//println(s"line: $line")
			val siteToNameAndModel_m = Map(
				CarrierNameGridSiteIndex(plateOrigCarrierName, plateOrigGrid, plateOrigSite) -> (plateOrigName, plateModelName),
				CarrierNameGridSiteIndex(plateDestCarrierName, plateDestGrid, plateDestSite) -> (plateDestName, plateModelName)
			)
			Some(Token(line, siteToNameAndModel_m))
		}
	}
}
