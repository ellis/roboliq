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

case class Opt(
	configFile: File = null,
	protocolFile: File = null
)

case class EvowareOpt(
	robotName: String = "",
	evowareDir: String = "",
	tableFile: String = "",
	robotConfigFile: String = ""
)

object Main extends App {
	val parser = new scopt.OptionParser[Opt]("roboliq") {
		head("roboliq", "0.1pre1")
		opt[File]("config") required() valueName("<file>") action { (x, o) =>
			o.copy(configFile = x) } text("configuration file")
		opt[File]("protocol") required() valueName("<file>") action { (x, o) =>
			o.copy(protocolFile = x) } text("protocol file")
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
			_ <- protocol.loadConfigBean(configBean)

			jsobj <- loadProtocolJson(opt.protocolFile)
			_ <- protocol.loadJson(jsobj)
			
			basename = FilenameUtils.getBaseName(opt.protocolFile.getPath())
			dirFile = opt.protocolFile.getParentFile()
			dateString = new SimpleDateFormat("ddMyyyy-hhmmss").format(new Date())
			dirOutput = new File(dirFile, s"roboliq--$basename--$dateString")
			_ = dirOutput.mkdir()
			pair <- protocol.createPlan()
			(planInfo_l, plan0) = pair
			filenameDomain = new File(dirOutput, "domain.pddl").getPath
			filenameProblem = new File(dirOutput, "problem.pddl").getPath
			filenamePlan0 = new File(dirOutput, "plan0.dot").getPath
			_ = roboliq.utils.FileUtils.writeToFile(filenameDomain, plan0.problem.domain.toStripsText)
			_ = roboliq.utils.FileUtils.writeToFile(filenameProblem, plan0.problem.toStripsText)
			_ = roboliq.utils.FileUtils.writeToFile(filenamePlan0, plan0.toDot)
			step0 = aiplan.strips2.PopState_SelectGoal(plan0, 0)
			plan2 <- aiplan.strips2.Pop.stepToEnd(step0).asRs
			_ = println("plan2:")
			_ = println(plan2.toDot)
			plan3 <- aiplan.strips2.Pop.groundPlan(plan2).asRs
			_ = println("plan3:")
			_ = println(plan3.toDot)
			//state <- JshopTranslator.translate(protocol, plan)
			//_ = println("result: " + result)
		} yield {
			roboliq.utils.FileUtils.writeToFile("test.dot", plan3.toDot)
			val actionOrig_l = planInfo_l zip plan3.action_l.drop(2)
			actionOrig_l.map(pair => {
				val (planInfo, action) = pair
				val planned = plan3.bindings.bind(action)
				val handler = cs.nameToActionHandler_m(planInfo.planAction.name)
				val op = handler.getOperator(planInfo, planned, protocol.eb)
				println("op:")
				println(op)
				op
			})
			// Additional actions added by the planner
			val actionAuto_l = plan3.action_l.toList.drop(2 + planInfo_l.size)
			actionAuto_l.map(action => {
				val planned = plan3.bindings.bind(action)
				val handler = cs.nameToAutoActionHandler_m(action.name)
				val op = handler.getInstruction(planned, protocol.eb)
				println("op:")
				println(op)
				op
			})

			/*
			val builder_l = protocol.agentToBuilder_m.values.toSet
			for (scriptBuilder <- builder_l) {
				val basename2 = new File(dir, basename + "_" + scriptBuilder.agentName).getPath
				println("basename: " + basename2)
				//println("scriptBuilder: " + scriptBuilder)
				scriptBuilder.saveScripts(basename2)
			}
			val l1 = state.well_aliquot_m.toList.map(pair => {
				val (well, aliquot) = pair
				val wellPosition = state.getWellPosition(well).toOption.get
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
			* 
			*/
		}

		val error_l = x.getErrors
		val warning_l = x.getWarnings
		if (!error_l.isEmpty || !warning_l.isEmpty) {
			println("Warnings and Errors:")
			error_l.foreach(println)
			warning_l.foreach(println)
			println()
		}
	}
	
	private def loadConfigBean(path: String): RsResult[ConfigBean] = {
		import org.yaml.snakeyaml._
		import org.yaml.snakeyaml.constructor.Constructor
		import roboliq.input._
		
		val text = scala.io.Source.fromFile(path).mkString
		
		val descriptionEvoware = new TypeDescription(classOf[EvowareAgentBean])
		descriptionEvoware.putMapPropertyType("tipModels", classOf[String], classOf[TipModelBean])
		descriptionEvoware.putListPropertyType("tips", classOf[TipBean])
		val constructorEvoware = new Constructor(classOf[EvowareAgentBean])
		constructorEvoware.addTypeDescription(descriptionEvoware);

		val descriptionConfig = new TypeDescription(classOf[ConfigBean])
		descriptionConfig.putMapPropertyType("evowareAgents", classOf[String], classOf[EvowareAgentBean])
		val constructorConfig = new Constructor(classOf[ConfigBean])
		constructorConfig.addTypeDescription(descriptionConfig)
		constructorConfig.addTypeDescription(descriptionEvoware)
		
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