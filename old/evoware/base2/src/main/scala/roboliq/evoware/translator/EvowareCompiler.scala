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
import roboliq.utils.JsonUtils
import spray.json.JsString
import scala.math.Ordering.Implicits.seqDerivedOrdering
import roboliq.utils.MiscUtils
import spray.json.JsBoolean
import roboliq.parsers.WellNameSingleParsed
import roboliq.parsers.WellNameSingleParser
import roboliq.parsers.AmountParser
import roboliq.parsers.Amount_Volume
import roboliq.parsers.Amount_Variable
import java.text.NumberFormat
import java.util.Locale
import java.text.DecimalFormat

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

private case class LabwareInfo(
	labwareName: String,
	labwareModelName0: String,
	labwareModelName: String,
	siteName: String,
	cngs: CarrierNameGridSiteIndex
)

private case class LabwareModelInfo(
	labwareModelName0: String,
	labwareModelName: String,
	rowCount: Int,
	colCount: Int
)

class EvowareCompiler(
	agentName: String,
	handleUserInstructions: Boolean
) {
	private val logger = Logger[this.type]

	// Decimal formatter for a maximum of 2 decimal places, using '.' as decimal point separator
	private val df = {
		val df = NumberFormat.getNumberInstance(Locale.US).asInstanceOf[DecimalFormat]
		df.setMaximumFractionDigits(2)
		df.setMinimumFractionDigits(0)
		df.setGroupingUsed(false)
		df
	}

	def buildTokens(
		input: JsObject
	): ResultC[List[Token]] = {
		for {
			objects0 <- JsConverter.fromJs[JsObject](input, "objects")
			steps <- JsConverter.fromJs[JsObject](input, "steps")
			effects <- JsConverter.fromJs[Option[JsObject]](input, "effects").map(_.getOrElse(JsObject()))
			token_l <- handleStep(Vector(), steps, objects0, effects)
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
			if (!token.line.isEmpty)
				line_l :+= token.line
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
	
	/**
	 * If the step has a description, add a Comment token
	 * If the step has sub-steps, handle them
	 * Else if the step has a command for this agent, handle it
	 */
	private def handleStep(
		path: Vector[String],
		step: JsObject,
		objects: JsObject,
		effects: JsObject
	): ResultC[List[Token]] = {
		val substep_l = step.fields.keys.toList
				.filter(key => key.isEmpty || key(0).isDigit) // Keys that start with digits (or is empty)
				.sortWith((s1, s2) => MiscUtils.compareNatural(s1, s2) < 0) // Natural sort order
		//println(s"handleStep($path, $step)")
		for {
			description_? <- JsConverter.fromJs[Option[String]](step, "description")
			commandName_? <- JsConverter.fromJs[Option[String]](step, "command")
			agentName_? <- JsConverter.fromJs[Option[String]](step, "agent")
			
			// Handle command substeps or command
			token_l <- {
				if (!substep_l.isEmpty) {
					handleSteps(path, step, substep_l, objects, effects)
				}
				else {
					(commandName_?, agentName_?) match {
						case ((Some(commandName), Some(agentName))) =>
							if (agentName == this.agentName) {
								handleCommand(path, step, commandName, agentName, objects, effects)
							}
							else if (agentName == "user" && handleUserInstructions) {
								handleUserCommand(path, step, commandName, objects, effects)
							}
							else {
								println("command ignored due to agent: "+step)
								ResultC.unit(Nil)
							}
						case (None, None) =>
							ResultC.unit(Nil)
						case _ =>
							ResultC.error(s"don't know how to handle command=${commandName_?}, agent=${agentName_?}")
					}
				}
			}
			//_ = println("token_?: "+token_?)
		} yield {
			val id = path.mkString(".")
			val comment_l = description_?.map(s => Token(createCommentLine(id + ":" + s))).toList
			effects.fields.get(id) match {
				case Some(o: JsObject) =>
					//println(s"effects[$id]: $o")
					val let = JsObject(o.fields.map(pair => {
						val o2 = JsonUtils.makeSimpleObject(pair._1, pair._2)
						o2.fields.head
					}))
					comment_l ++ token_l ++ List(Token("", let, Map()))
				case _ => comment_l ++ token_l
			}
		}
	}
	
	private def handleSteps(
		path: Vector[String],
		steps: JsObject,
		substep_l: List[String],
		objects0: JsObject,
		effects: JsObject
	): ResultC[List[Token]] = {
		//println(s"handleSteps($path, ${key_l})")
		var objects = objects0
		for {
			token_ll <- ResultC.map(substep_l) { key =>
				steps.fields(key) match {
					case step: JsObject =>
						for {
							token_l <- handleStep(path :+ key, step, objects, effects)
							objects1 <- ResultC.context("effects") {
								token_l.foldLeft(ResultC.unit(objects)) { (acc, token) => acc.flatMap(objects => JsonUtils.mergeMaps(objects, token.let)) }
							}
						} yield {
							objects = objects1
							token_l
						}
					case _ => ResultC.unit(Nil)
				}
			}
		} yield token_ll.flatten
	}
	
	/*
	private def handleLet(
		objects: JsObject,
		let: JsObject
	): ResultC[List[Token]] = {
		ResultC.unit(List(Token("", let, Map())))
	}*/
	
	private def handleUserCommand(
		path: Vector[String],
		step: JsObject,
		commandName: String,
		objects: JsObject,
		effects: JsObject
	): ResultC[List[Token]] = {
		commandName match {
			case _ =>
				ResultC.error(s"unhandled user command: $commandName in $step")
		}
	}
	
	private def handleCommand(
		path: Vector[String],
		step: JsObject,
		commandName: String,
		agentName: String,
		objects: JsObject,
		effects: JsObject
	): ResultC[List[Token]] = {
		val map = Map[String, (JsObject, JsObject) => ResultC[List[Token]]](
			"evoware._facts" -> handleFacts,
			"pipetter._aspirate" -> handlePipetterAspirate,
			"pipetter._dispense" -> handlePipetterDispense,
			"pipetter._pipette" -> handlePipetterPipette,
			"pipetter._cleanTips" -> handlePipetterCleanTips,
			"timer._start" -> handleTimerStart,
			"timer._wait" -> handleTimerWait,
			"transporter._movePlate" -> handleTransporterMovePlate
		)
		map.get(commandName) match {
			case Some(fn) => fn(objects, step)
			case None =>
				ResultC.error(s"unknown command: $commandName in $step")
		}
	}
	
	def lookupAs[A : TypeTag](
		objects: JsObject,
		objectName: String
	): ResultC[A] = {
		val field_l = objectName.split('.').toList
		//println(s"lookupAs($objectName, $fieldName): ${field_l}")
		JsConverter.fromJs[A](objects, field_l)
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
	
	private def handleFacts(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
		for {
			inst <- JsConverter.fromJs[EvowareFacts](step)
		} yield {
			val line = createFactsLine(inst.factsEquipment, inst.factsVariable, inst.factsValue_?.getOrElse(""))
			List(Token(line))
		}
	}

	private def handlePipetterAspirate(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
		for {
			inst <- JsConverter.fromJs[PipetterSpirate](step)
			result <- handlePipetterSpirate(objects, inst.program, inst.items, "Aspirate")
		} yield result
	}
	
	private def handlePipetterDispense(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
		for {
			inst <- JsConverter.fromJs[PipetterSpirate](step)
			result <- handlePipetterSpirate(objects, inst.program, inst.items, "Dispense")
		} yield result
	}
	
	private def handlePipetterPipette(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
		for {
			inst <- JsConverter.fromJs[PipetterSpirate2](step)
			// Group items together that having increasing syringe indexes
			item_ll = {
				var item_ll = Vector[List[PipetterItem2]]()
				var item_l = inst.items
				var syringe = -1
				while (!item_l.isEmpty) {
					val item_l2 = item_l.takeWhile(item => {
						if (item.syringe > syringe) {
							syringe = item.syringe
							true
						}
						else {
							syringe = -1
							false
						}
					})
					item_ll :+= item_l
					item_l = item_l.drop(item_l.length)
				}
				item_ll.toList
			}
			// For each group, create aspirate and dispense commands
			token_ll <- ResultC.map(item_ll) { item_l =>
				val asp_l = item_l.map { item => PipetterItem(item.syringe, item.source, item.volume) }
				val dis_l = item_l.map { item => PipetterItem(item.syringe, item.destination, item.volume) }
				for {
					l1 <- handlePipetterSpirate(objects, stripQuotes(inst.program), asp_l, "Aspirate")
					l2 <- handlePipetterSpirate(objects, stripQuotes(inst.program), dis_l, "Dispense")
				} yield l1 ++ l2
			}
		} yield token_ll.flatten
	}
	
	private def stripQuotes(s: String): String = {
		if (s.startsWith("\"") && s.endsWith("\"")) {
			s.substring(1, s.length - 1)
		}
		else
			s
	}
	
	private def handlePipetterSpirate(
		objects: JsObject,
		program: String,
		items: List[PipetterItem],
		func: String
	): ResultC[List[Token]] = {
		if (items.isEmpty) return ResultC.unit(Nil)
		
		for {
			// Get WellPosition and CarrierSite for each item
			tuple_l <- ResultC.map(items) { item =>
				for {
					wellPosition <- WellNameSingleParser.parse(item.well)
					labwareName <- ResultC.from(wellPosition.labware_?, "incomplete well specification; please also specify the labware")
					labwareInfo <- getLabwareInfo(objects, labwareName)
				} yield {
					(item, wellPosition, labwareInfo)
				}
			}
			token_l <- handlePipetterSpirateDoGroup(objects, program, func, tuple_l)
		} yield token_l
	}

	private def handlePipetterSpirateDoGroup(
		objects: JsObject,
		program: String,
		func: String,
		tuple_l: List[(PipetterItem, WellNameSingleParsed, LabwareInfo)]
	): ResultC[List[Token]] = {
		if (tuple_l.isEmpty) return ResultC.unit(Nil)
		val col = tuple_l.head._2.col
		val labwareInfo = tuple_l.head._3
		// Get all items on the same labware and in the same column
		val tuple_l2 = tuple_l.takeWhile(tuple => tuple._2.col == col && tuple._3 == labwareInfo)
		val (tuple_l3, tipSpacing) = {
			if (tuple_l2.length == 1) {
				tuple_l.take(1) -> 1
			}
			// If there are multiple items, group the ones that are acceptably spaced 
			else {
				val syringe0 = tuple_l.head._1.syringe
				val row0 = tuple_l.head._2.row
				val dsyringe = tuple_l(1)._1.syringe - syringe0
				val drow = tuple_l(1)._2.row - row0
				// Syringes and rows should have ascending indexes, and the spacing should be 4 at most
				if (dsyringe <= 0 || drow <= 0 || drow / dsyringe > 4) {
					tuple_l.take(1) -> 1
				}
				else {
					// Take as many items as preserve the initial deltas for syringe and row
					tuple_l2.zipWithIndex.takeWhile({ case (tuple, index) =>
						tuple._2.row == row0 + index * drow && tuple._1.syringe == syringe0 + index * dsyringe
					}).map(_._1) -> drow
				}
			}
		}
		for {
			token_l1 <- handlePipetterSpirateHandleGroup(objects, program, func, tuple_l3, labwareInfo, tipSpacing)
			token_l2 <- handlePipetterSpirateDoGroup(objects, program, func, tuple_l.drop(tuple_l3.size))
		} yield token_l1 ++ token_l2
	}
	
	private def handlePipetterSpirateHandleGroup(
		objects: JsObject,
		program: String,
		func: String,
		tuple_l: List[(PipetterItem, WellNameSingleParsed, Any)],
		labwareInfo: LabwareInfo,
		tipSpacing: Int
	): ResultC[List[Token]] = {
		// Calculate syringe mask
		val syringe_l = tuple_l.map(_._1.syringe)
		val syringeMask = encodeSyringes(syringe_l)
		
		val well_l = tuple_l.map(_._2)
		val volume_l = Array.fill(12)("0")
		
		for {
			labwareModelInfo <- getLabwareModelInfo(objects, labwareInfo.labwareModelName0)
			plateMask <- encodeWells(labwareModelInfo.rowCount, labwareModelInfo.colCount, well_l)
			// Create a list of volumes for each used tip, leaving the remaining values at 0
			_ <- ResultC.foreach(tuple_l) { tuple =>
				for {
					amount <- AmountParser.parse(tuple._1.volume)
					syringe = tuple._1.syringe
					_ <- ResultC.assert(syringe >= 1 && syringe <= 12, s"invalid syringe value $syringe: must be between 1 and 12")
					volumeString <- amount match {
						case Amount_Volume(volume) =>
							ResultC.unit(df.format(volume.ul))
						case Amount_Variable(name) =>
							ResultC.unit(name)
						case _ =>
							ResultC.error(s"invalid volume `${tuple._1.volume}`: expected liquid volume or evoware variable name, but got $amount")
					}
				} yield {
					volume_l(syringe - 1) = s""""$volumeString""""
				}
			}
		} yield {
			val line = List(
				syringeMask,
				s""""$program"""",
				volume_l.mkString(","),
				labwareInfo.cngs.gridIndex, labwareInfo.cngs.siteIndex,
				tipSpacing,
				s""""$plateMask"""",
				0,
				0
			).mkString(func+"(", ",", ");")
	
			val siteToNameAndModel_m: Map[CarrierNameGridSiteIndex, (String, String)] = {
				// Don't set labware for the "System" liquid site
				if (labwareInfo.cngs.carrierName == "System") Map()
				else Map(labwareInfo.cngs -> (labwareInfo.siteName, labwareInfo.labwareModelName))
			}
			
			List(Token(line, JsObject(), siteToNameAndModel_m))
		}
	}
	
	private def handlePipetterCleanTips(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
		
		def handleScript(filename: String): ResultC[String] = {
			val line = List(
				'"'+filename+'"',
				0
			).mkString("Subroutine(", ",", ");")
			ResultC.unit(line)
		}
		
		def handleWashProgram(inst: PipetterWashTips, jsobj: JsObject): ResultC[String] = {
			for {
				program <- JsConverter.fromJs[PipetterWashProgram](jsobj)
			} yield {
				val syringeMask = encodeSyringes(inst.syringes)
				val bUNKNOWN1 = false
				val line = List(
					syringeMask,
					program.wasteGrid, program.wasteSite,
					program.cleanerGrid, program.cleanerSite,
					'"'+df.format(program.wasteVolume)+'"',
					program.wasteDelay,
					'"'+df.format(program.cleanerVolume)+'"',
					program.cleanerDelay,
					program.airgapVolume,
					program.airgapSpeed,
					program.retractSpeed,
					(if (program.fastWash) 1 else 0),
					(if (bUNKNOWN1) 1 else 0),
					1000,
					0
				).mkString("Wash(", ",", ");")
				line
			}
		}
		
		for {
			inst <- JsConverter.fromJs[PipetterWashTips](step)
			jsobj <- inst.program match {
				case JsString(programName) => lookupAs[JsObject](objects, programName)
				case program: JsObject => ResultC.unit(program)
				case _ => ResultC.error(s"program: expected either an EvowareWashProgram; received ${inst.program}")
			}
			line <- jsobj.fields.get("script") match {
				case Some(JsString(filename)) => handleScript(filename)
				case _ => handleWashProgram(inst, jsobj)
			}
		} yield {
			//val let = JsonUtils.makeSimpleObject(inst.`object`+".sealed", JsBoolean(true))
			List(Token(line, JsObject(), Map()))
		}
	}
	
	/**
	 * Encode a list of syringes as an evoware bitmask
	 */
	protected def encodeSyringes(list: Iterable[Int]): Int =
		list.foldLeft(0) { (sum, syringe) => sum | (1 << (syringe - 1)) }

	/**
	 * Encode a list of wells on a plate as an evoware bitmask
	 */
	protected def encodeWells(rows: Int, cols: Int, well_l: Traversable[WellNameSingleParsed]): ResultC[String] = {
		//println("encodeWells:", holder.nRows, holder.nCols, aiWells)
		val nWellMaskChars = math.ceil(rows * cols / 7.0).asInstanceOf[Int]
		val amWells = new Array[Int](nWellMaskChars)
		for (well <- well_l) {
			val index = well.row + well.col * rows
			val iChar = index / 7;
			val iWell1 = index % 7;
			if (iChar >= amWells.size) {
				return ResultC.error("INTERNAL ERROR: encodeWells: index out of bounds -- "+(rows, cols, well, index, iChar, iWell1, well_l))
			}
			amWells(iChar) += 1 << iWell1
		}
		val sWellMask = amWells.map(encode).mkString
		val sPlateMask = f"$cols%02X$rows%02X" + sWellMask
		ResultC.unit(sPlateMask)
	}

	/**
	 * Encode a number as a character for evoware
	 */
	private def encode(n: Int): Char = ('0' + n).asInstanceOf[Char]
	//private def hex(n: Int): Char = Integer.toString(n, 16).toUpperCase.apply(0)
	
	private def handleTimerStart(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
		for {
			equipment <- JsConverter.fromJs[String](step, "equipment")
			id <- lookupAs[Int](objects, equipment, "evowareId")
		} yield {
			val line = List(
				'"'+id.toString+'"'
			).mkString("StartTimer(", ",", ");")
			List(Token(line))
		}
	}
	
	private def handleTimerWait(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
		for {
			equipment <- JsConverter.fromJs[String](step, "equipment")
			till <- JsConverter.fromJs[Int](step, "till")
			id <- lookupAs[Int](objects, equipment, "evowareId")
		} yield {
			val line = List(
				'"'+id.toString+'"',
				'"'+till.toString+'"'
			).mkString("WaitTimer(", ",", ");")
			List(Token(line))
		}
	}

	/*
case class L0C_WaitTimer(
	id: Int,
	nSeconds: Int
) extends L0C_Command {
	override def toString = {
		List(
			'"'+id.toString+'"',
			'"'+nSeconds.toString+'"'
		).mkString("WaitTimer(", ",", ");")
	}
}
		
	}*/
	
	private def handleTransporterMovePlate(
		objects: JsObject,
		step: JsObject
	): ResultC[List[Token]] = {
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
				s""""$plateOrigSite"""",
				"(Not defined)", // '"'+(if (lidHandling == NoLid) "(Not defined)" else iSiteLid.toString)+'"',
				s""""$plateDestSite""""
			).mkString("Transfer_Rack(", ",", ");")
			//println(s"line: $line")
			val let = JsonUtils.makeSimpleObject(x.`object`+".location", JsString(plateDestName))
			val siteToNameAndModel_m = Map(
				CarrierNameGridSiteIndex(plateOrigCarrierName, plateOrigGrid, plateOrigSite) -> (plateOrigName, plateModelName),
				CarrierNameGridSiteIndex(plateDestCarrierName, plateDestGrid, plateDestSite) -> (plateDestName, plateModelName)
			)
			List(Token(line, let, siteToNameAndModel_m))
		}
	}
	
	private def createCommentLine(
		s: String
	): String = {
		List(
			'"'+s+'"'
		).mkString("Comment(", ",", ");")
	}
	
	private def createFactsLine(
		equipment: String,
		variableName: String,
		value: String
	): String = {
		List(
			'"'+equipment+'"',
			'"'+variableName+'"',
			'"'+value+'"',
			"\"0\"",
			"\"\""
		).mkString("FACTS(", ",", ");")
	}
	
	private def getLabwareInfo(
		objects: JsObject,
		labwareName: String
	): ResultC[LabwareInfo] = {
		for {
			labwareModelName0 <- lookupAs[String](objects, labwareName, "model")
			labwareModelName <- lookupAs[String](objects, labwareModelName0, "evowareName")
			siteName <- lookupAs[String](objects, labwareName, "location")
			carrierName <- lookupAs[String](objects, siteName, "evowareCarrier")
			gridIndex <- lookupAs[Int](objects, siteName, "evowareGrid")
			siteIndex <- lookupAs[Int](objects, siteName, "evowareSite")
		} yield {
			LabwareInfo(
				labwareName,
				labwareModelName0,
				labwareModelName,
				siteName,
				CarrierNameGridSiteIndex(carrierName, gridIndex, siteIndex)
			)
		}
	}
	
	private def getLabwareModelInfo(
		objects: JsObject,
		labwareModelName0: String
	): ResultC[LabwareModelInfo] = {
		for {
			rowCount <- lookupAs[Int](objects, labwareModelName0, "rows")
			colCount <- lookupAs[Int](objects, labwareModelName0, "columns")
			labwareModelName <- lookupAs[String](objects, labwareModelName0, "evowareName")
		} yield {
			LabwareModelInfo(
				labwareModelName0,
				labwareModelName,
				rowCount,
				colCount
			)
		}
	}
}
