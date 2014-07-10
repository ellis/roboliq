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

object Main extends App {
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
			
			basename = FilenameUtils.getBaseName(opt.protocolFile.getPath())
			dirFile = opt.protocolFile.getParentFile()
			dateString = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
			dirOutput = if (opt.outputDir != null) opt.outputDir else new File(dirFile, s"roboliq--$basename--$dateString")
			_ = dirOutput.mkdirs()
			pair <- protocol.createPlan()
			(planInfo_l, plan0) = pair
			filenameDomain = new File(dirOutput, "domain.pddl").getPath
			filenameProblem = new File(dirOutput, "problem.pddl").getPath
			filenamePlan0 = new File(dirOutput, "plan0.dot").getPath
			filenameActions0 = new File(dirOutput, "actions0.lst").getPath
			filenamePlan1 = new File(dirOutput, "plan1.dot").getPath
			filenamePlan = new File(dirOutput, "plan.dot").getPath
			filenameHdf5 = new File(dirOutput, "data.hdf5").getPath
			hdf5 = new Hdf5(filenameHdf5)
			_ = roboliq.utils.FileUtils.writeToFile(filenameDomain, plan0.problem.domain.toStripsText)
			_ = roboliq.utils.FileUtils.writeToFile(filenameProblem, plan0.problem.toStripsText)
			_ = roboliq.utils.FileUtils.writeToFile(filenamePlan0, plan0.toDot(showInitialState=true))
			_ = roboliq.utils.FileUtils.writeToFile(filenameActions0, plan0.action_l.mkString("\n"))
			step0 = aiplan.strips2.PopState_SelectGoal(plan0, 0)
			plan2 <- RsResult.from(aiplan.strips2.Pop.stepToEnd(step0))
			_ = roboliq.utils.FileUtils.writeToFile(filenamePlan1, plan2.toDot(showInitialState=true))
			//_ = println("plan2:")
			//_ = println(plan2.toDot(showInitialState=false))
			//_ = println("orderings: "+plan2.orderings.getMinimalMap)
			plan3 <- RsResult.from(aiplan.strips2.Pop.groundPlan(plan2))
			_ = roboliq.utils.FileUtils.writeToFile(filenamePlan, plan3.toDot(showInitialState=true))
			//_ = println("plan3:")
			//_ = println(plan3.toDot(showInitialState=false))
			// List of action indexes in the ordered they've been planned (0 = initial state action, 1 = final goal action)
			ordering_l <- RsResult.from(plan3.orderings.getSequence).map(_.filter(_ >= 2))
			originalActionCount = planInfo_l.size
		} yield {
			val data0 = ProtocolData(
				protocol,
				protocol.eb,
				protocol.state0.toImmutable
			)
			val indexToOperator_l = ordering_l.map(action_i => (action_i, plan3.bindings.bind(plan3.action_l(action_i))))
			val ctx0 = for {
				// Instructions
				_ <- getInstructions(cs, planInfo_l, originalActionCount, indexToOperator_l)
				_ = println("instructions:")
				ai_l <- Context.gets(_.instruction_l.toList)
				_ = ai_l.foreach(x => { println(x._1) })
				_ = hdf5.addInstructions(opt.protocolFile.getName(), ai_l.map(_._1))
			} yield {
				protocol.agentToBuilder_m.values.foreach(_.end())
				val builder_l = protocol.agentToBuilder_m.values.toSet
				for (scriptBuilder <- builder_l) {
					val basename2 = new File(dirOutput, basename + "_" + scriptBuilder.agentName).getPath
					println("basename: " + basename2)
					//println("scriptBuilder: " + scriptBuilder)
					scriptBuilder.saveScripts(basename2)
				}
			}
			val (data1, _) = ctx0.run(data0)

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
		} yield input.asJson.asJsObject
	}
}