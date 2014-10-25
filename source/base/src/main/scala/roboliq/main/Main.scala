package roboliq.main

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import com.google.gson.Gson
import roboliq.core._
import roboliq.entities.ClientScriptBuilder
import roboliq.evoware.translator.EvowareClientScriptBuilder
import roboliq.evoware.translator.EvowareConfig
import roboliq.input.ConfigBean
import roboliq.input.Protocol
import roboliq.plan.CallTree
import spray.json.JsObject
import spray.json.pimpString
import grizzled.slf4j.Logger
import roboliq.entities.WorldState
import roboliq.input.commands.PlanPath
import roboliq.entities.LiquidVolume
import roboliq.entities.AliquotFlat
import roboliq.plan.CommandSet
import roboliq.entities.WorldStateEvent
import roboliq.entities.AliquotFlat
import roboliq.plan.OperatorInfo
import roboliq.input.AgentInstruction
import spray.json.JsValue
import roboliq.hdf5.Hdf5
import roboliq.input.Context
import roboliq.input.ProtocolData
import roboliq.commands.PipetterDispense
import roboliq.input.WellDispenseEntry
import roboliq.entities.Distribution

case class Opt(
	configFile: File = null,
	protocolFile: File = null,
	outputDir: File = null
)

case class EvowareOpt(
	robotName: String = "",
	evowareDir: String = "",
	tableFile: String = "",
	robotConfigFile: String = ""
)

class Runner(args: Array[String]) {
	private val logger = Logger[this.type]

	val parser = new scopt.OptionParser[Opt]("roboliq") {
		head("roboliq", "0.1pre1")
		opt[File]("config") required() valueName("<file>") action { (x, o) =>
			o.copy(configFile = x) } text("configuration file")
		opt[File]("protocol") required() valueName("<file>") action { (x, o) =>
			o.copy(protocolFile = x) } text("protocol file")
		opt[File]("output") valueName("<dir>") action { (x, o) =>
			o.copy(outputDir = x) } text("output directory")
	}
	parser.parse(args, Opt()) map { opt =>
		run(opt)
	} getOrElse {
		// arguments are bad, usage message will have been displayed
	}
	
	private def getInputHash(opt: Opt): String = {
		val content1 = scala.io.Source.fromFile(opt.configFile).mkString
		val content2 = scala.io.Source.fromFile(opt.protocolFile).mkString
		
		roboliq.utils.MiscUtils.md5Hash(content1 ++ content2)
	}
	
	private def save(hdf5: Hdf5, scriptId: String, dir: File, filename: String, content: String) {
		// Save to HDF5
		hdf5.addFileText(scriptId, filename, content)
		// Save to file system
		val path = new File(dir, filename).getPath
		roboliq.utils.FileUtils.writeToFile(path, content)
	}
	
	private def saveBytes(hdf5: Hdf5, scriptId: String, dir: File, filename: String, content: Array[Byte]) {
		// Save to HDF5
		hdf5.addFileBytes(scriptId, filename, content)
		// Save to file system
		val path = new File(dir, filename).getPath
		val os = new java.io.FileOutputStream(path)
		os.write(content)
		os.close()
	}
	
	def run(opt: Opt) {
		import scala.sys.process._
		val protocol = new Protocol
		val x = for {
			configBean <- loadConfigBean(opt.configFile.getPath())
			//_ = println("opt.configFile.getPath(): "+opt.configFile.getPath())
			cs <- protocol.loadCommandSet()
			_ <- protocol.loadConfigBean(configBean, Nil)

			jsobj <- loadProtocolJson(opt.protocolFile)
			_ <- protocol.loadJson(jsobj)
			
			scriptId = getInputHash(opt)
			
			basename = FilenameUtils.getBaseName(opt.protocolFile.getPath())
			dirFile = opt.protocolFile.getParentFile()
			dateString = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
			dirOutput = if (opt.outputDir != null) opt.outputDir else new File(dirFile, s"roboliq--$basename--$dateString")
			_ = dirOutput.mkdirs()
			pair <- protocol.createPlan()
			(planInfo_l, plan0) = pair
			filenameHdf5 = new File(dirOutput, "data.h5").getPath
			hdf5 = new Hdf5(filenameHdf5)
			_ = hdf5.copyFile(scriptId, "config.yaml", opt.configFile)
			_ = hdf5.copyFile(scriptId, "protocol.prot", opt.protocolFile)
			_ = save(hdf5, scriptId, dirOutput, "domain.pddl", plan0.problem.domain.toStripsText)
			_ = save(hdf5, scriptId, dirOutput, "problem.pddl", plan0.problem.toStripsText)
			_ = save(hdf5, scriptId, dirOutput, "plan0.dot", plan0.toDot(showInitialState=true))
			_ = save(hdf5, scriptId, dirOutput, "actions0.lst", plan0.action_l.drop(2).mkString("\n"))
			_ = hdf5.saveSubstances(scriptId, protocol.nameToSubstance_m.toList.sortBy(_._1).map(_._2))
			_ = hdf5.saveSourceMixtures(scriptId, protocol.eb.sourceToMixture_m.toList.sortBy(_._1))
			step0 = aiplan.strips2.PopState_SelectGoal(plan0, 0)
			plan2 <- RsResult.from(aiplan.strips2.Pop.stepToEnd(step0))
			_ = save(hdf5, scriptId, dirOutput, "plan1.dot", plan2.toDot(showInitialState=true))
			//_ = println("plan2:")
			//_ = println(plan2.toDot(showInitialState=false))
			//_ = println("orderings: "+plan2.orderings.getMinimalMap)
			plan3 <- RsResult.from(aiplan.strips2.Pop.groundPlan(plan2))
			_ = save(hdf5, scriptId, dirOutput, "plan.dot", plan3.toDot(showInitialState=true))
			_ = save(hdf5, scriptId, dirOutput, "actions.lst", (0 until plan3.action_l.size).map(i => plan3.getActionText(i)).mkString("\n"))
			//_ = println("plan3:")
			//_ = println(plan3.toDot(showInitialState=false))
			// List of action indexes in the ordered they've been planned (0 = initial state action, 1 = final goal action)
			ordering_l <- RsResult.from(plan3.orderings.getSequence).map(_.filter(_ >= 2))
			originalActionCount = planInfo_l.size
		} yield {
			val searchPath_l = List[File](opt.protocolFile.getAbsoluteFile().getParentFile(), opt.configFile.getAbsoluteFile().getParentFile())
			val data0 = ProtocolData(
				protocol,
				protocol.eb,
				protocol.state0.toImmutable,
				searchPath_l
			)
			val indexToOperator_l = ordering_l.map(action_i => (action_i, plan3.bindings.bind(plan3.action_l(action_i))))
			val ctx0 = for {
				// Instructions
				_ <- getInstructions(cs, planInfo_l, originalActionCount, indexToOperator_l)
				//_ = println("instructions:")
				ai_l <- Context.gets(_.instruction_l.toList)
				//_ = ai_l.foreach(x => { println(x._1) })
			} yield {
				hdf5.saveInstructions(scriptId, ai_l.map(_._1))
				protocol.agentToBuilder_m.values.foreach(_.end())
				val builder_l = protocol.agentToBuilder_m.values.toSet
				for {
					filenameToData_ll <- RsResult.mapAll(builder_l) { scriptBuilder =>
						//val basename2 = new File(dirOutput, basename + "_" + scriptBuilder.agentName).getPath
						val basename2 = basename + "_" + scriptBuilder.agentName
						scriptBuilder.generateScripts(basename2)
					}
				} {
					for ((filename, byte_l) <- filenameToData_ll.flatten) {
						saveBytes(hdf5, scriptId, dirOutput, filename, byte_l)
					}
				}
			}
			val (data1, _) = ctx0.run(data0)
			
			hdf5.saveInstructionData(scriptId, data1.instruction_l.map(_._1).toList)

			// Print final aliquots in all wells
			val l1 = data1.state.well_aliquot_m.toList.map(pair => {
				val (well, aliquot) = pair
				val wellPosition = data1.state.getWellPosition(well).toOption.get
				val labwareIdent = protocol.eb.getIdent(wellPosition.parent).getOrElse("ERROR")
				val wellIdent = wellPosition.toString(protocol.eb)
				(labwareIdent, wellPosition.col, wellPosition.row) -> (wellIdent, aliquot)
			})
			val l2 = l1.sortBy(_._1)
			l2.foreach(pair => {
				val (wellIdent, aliquot) = pair._2
				val amount = {
					if (aliquot.distribution.bestGuess.units == roboliq.entities.SubstanceUnits.Liter)
						LiquidVolume.l(aliquot.distribution.bestGuess.amount).toString
					else
						aliquot.distribution.bestGuess.toString
				}
				println(s"$wellIdent: ${AliquotFlat(aliquot).toMixtureString} ${amount}")
			})
			
			// Consider creating an HDF5 table for the final well aliquots:
			//println("well_aliquot_m:")
			//data1.state.well_aliquot_m.foreach(println)
			
			hdf5.close()
			
			data1.error_r.reverse.foreach(println)
			data1.warning_r.reverse.foreach(println)
		}

		val error_l = x.getErrors
		val warning_l = x.getWarnings
		if (!error_l.isEmpty || !warning_l.isEmpty) {
			println("Warnings and Errors:")
			error_l.foreach(s => println("ERROR: "+s))
			warning_l.foreach(s => println("WARNING: "+s))
			println()
		}
		println("DONE")
	}
	
	private def getInstructions(
		cs: CommandSet,
		planInfo_l: List[OperatorInfo],
		originalActionCount: Int,
		indexToOperator_l: List[(Int, aiplan.strips2.Strips.Operator)]
	): Context[Unit] = {
		Context.foreachFirst(indexToOperator_l.zipWithIndex) { case ((action_i, operator), instructionIdx) =>
			for {
				_ <- Context.modify(_.setCommand(List(action_i)))
				_ <- getInstructionStep(cs, planInfo_l, originalActionCount, action_i, operator)
			} yield ()
		}
	}
	
	private def getInstructionStep(
		cs: CommandSet,
		operatorInfo_l: List[OperatorInfo],
		originalActionCount: Int,
		action_i: Int,
		operator: aiplan.strips2.Strips.Operator
	): Context[Unit] = {
		//println("action: "+operator)
		val instructionParam_m: Map[String, JsValue] = if (action_i - 2 < originalActionCount) operatorInfo_l(action_i - 2).instructionParam_m else Map()
		for {
			handler <- Context.from(cs.nameToOperatorHandler_m.get(operator.name), s"getInstructionStep: unknown operator `${operator.name}`")
			_ <- handler.getInstruction(operator, instructionParam_m)
		} yield ()
	}
	
	private def loadConfigBean(path: String): RsResult[ConfigBean] = {
		import org.yaml.snakeyaml._
		import org.yaml.snakeyaml.constructor.Constructor
		import roboliq.input._
		
		val text = scala.io.Source.fromFile(path).mkString
		
		val descriptionTableSetup = new TypeDescription(classOf[TableSetupBean])
		descriptionTableSetup.putMapPropertyType("sites", classOf[String], classOf[SiteBean])
		
		val descriptionEvoware = new TypeDescription(classOf[EvowareAgentBean])
		descriptionEvoware.putListPropertyType("labwareModels", classOf[LabwareModelBean])
		descriptionEvoware.putMapPropertyType("tipModels", classOf[String], classOf[TipModelBean])
		descriptionEvoware.putListPropertyType("tips", classOf[TipBean])
		descriptionEvoware.putMapPropertyType("tableSetups", classOf[String], classOf[TableSetupBean])
		//val constructorEvoware = new Constructor(classOf[EvowareAgentBean])
		//constructorEvoware.addTypeDescription(descriptionEvoware);

		val descriptionConfig = new TypeDescription(classOf[ConfigBean])
		descriptionConfig.putMapPropertyType("evowareAgents", classOf[String], classOf[EvowareAgentBean])
		val constructorConfig = new Constructor(classOf[ConfigBean])
		constructorConfig.addTypeDescription(descriptionConfig)
		constructorConfig.addTypeDescription(descriptionEvoware)
		constructorConfig.addTypeDescription(descriptionTableSetup)
		
		val yaml = new Yaml(constructorConfig)
		val configBean = yaml.load(text).asInstanceOf[ConfigBean]
		RsSuccess(configBean)
	}
	
	private def yamlToJson(s: String): RsResult[String] = {
		import org.yaml.snakeyaml._
		val yaml = new Yaml()
		val o = yaml.load(s).asInstanceOf[java.util.Map[String, Object]]
		val gson = new Gson
		val s_~ = gson.toJson(o)
		//println("gson: " + s_~)
		RsSuccess(s_~)
	}
	
	private def loadProtocolJson(file: File): RsResult[JsObject] = {
		for {
			_ <- RsResult.assert(file.exists, s"File not found: ${file.getPath}")
			bYaml <- FilenameUtils.getExtension(file.getPath).toLowerCase match {
				case "json" => RsSuccess(false)
				case "yaml" => RsSuccess(true)
				case "prot" => RsSuccess(true)
				case ext => RsError(s"unrecognized protocol file extension `$ext`")
			}
			input0 = FileUtils.readFileToString(file)
			input <- if (bYaml) yamlToJson(input0) else RsSuccess(input0) 
		} yield input.parseJson.asJsObject
	}
}

object Main extends App {
	args
	new Runner(args)
}