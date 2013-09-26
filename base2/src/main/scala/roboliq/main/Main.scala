package roboliq.main

import java.io.File
import org.apache.commons.io.FileUtils
import com.google.gson.Gson
import spray.json.JsObject
import spray.json.pimpString
import roboliq.core._
import roboliq.input.Protocol
import roboliq.input.ConfigBean
import roboliq.evoware.translator.EvowareConfigData
import roboliq.evoware.translator.EvowareConfig
import roboliq.evoware.translator.EvowareClientScriptBuilder
import roboliq.entities.ClientScriptBuilder
import roboliq.translator.jshop.JshopTranslator

case class Opt(
	configFile: File = new File("."),
	protocolFile: File = new File(".")
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
		
	} getOrElse {
		// arguments are bad, usage message will have been displayed
	}
	val protocol = new Protocol
	
	def runWeizmann(protocolName: String) {
		import org.yaml.snakeyaml.Yaml
		import org.yaml.snakeyaml.constructor.Constructor
		import scala.sys.process._
		val x = for {
			carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile("./testdata/wis-pcrobot/config/carrier.cfg")
			tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, "./testdata/wis-pcrobot/config/table-01.esc")
			configBean <- loadConfigBean("tasks/wisauto/config.yaml")

			_ = protocol.loadConfigBean(configBean)
			_ = protocol.loadEvoware("r1", carrierData, tableData, configBean)
			jsobj <- loadProtocolJson(s"tasks/wisauto/$protocolName")
			_ = protocol.loadJson(jsobj)
			
			_ = protocol.saveProblem(s"tasks/wisauto/$protocolName.lisp", "")
			_ = Seq("bash", "-c", s"source tasks/classpath.sh; make -C tasks/wisauto/ $protocolName.plan").!!
			planOutput = scala.io.Source.fromFile(s"tasks/wisauto/$protocolName.plan").getLines.toList
			_ <- RsResult.assert(planOutput.size > 4, "JSON planner did not find a plan")
			plan_l = planOutput.drop(2).reverse.drop(2).reverse
			plan = plan_l.mkString("\n")
			
			configData = EvowareConfigData(Map("G009S1" -> "pipette2hi"))
			config = new EvowareConfig(carrierData, tableData, configData)
			scriptBuilder = new EvowareClientScriptBuilder(config, s"tasks/wisauto/$protocolName")
			agentToBuilder_m = Map[String, ClientScriptBuilder](
				"user" -> scriptBuilder,
				"r1" -> scriptBuilder
			)
			result <- JshopTranslator.translate(protocol, plan, agentToBuilder_m)
		} yield {
			for (script <- scriptBuilder.script_l) {
				scriptBuilder.saveWithHeader(script, script.filename)
			}
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
		
		val text = scala.io.Source.fromFile("tasks/wisauto/config.yaml").mkString
		
		val descriptionEvoware = new TypeDescription(classOf[EvowareAgentBean])
		descriptionEvoware.putMapPropertyType("tipModels", classOf[String], classOf[TipModelBean])
		descriptionEvoware.putListPropertyType("tips", classOf[TipBean])
		val constructorEvoware = new Constructor(classOf[EvowareAgentBean])
		constructorEvoware.addTypeDescription(descriptionEvoware);

		val descriptionConfig = new TypeDescription(classOf[ConfigBean])
		descriptionConfig.putListPropertyType("evowareAgents", classOf[EvowareAgentBean])
		val constructorConfig = new Constructor(classOf[ConfigBean])
		constructorConfig.addTypeDescription(descriptionConfig);
		
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
		println("gson: " + s_~)
		RsSuccess(s_~)
	}
	
	private def loadProtocolJson(base: String): RsResult[JsObject] = {
		val prot = new File(s"$base.prot")
		val json = new File(s"$base.json")
		val yaml = new File(s"$base.yaml")
		
		val file = if (prot.exists) prot else if (json.exists) json else yaml
		for {
			_ <- RsResult.assert(file.exists, s"File not found: $base with extension prot, json, or yaml")
			input0 = FileUtils.readFileToString(file)
			input <- if (file eq json) RsSuccess(input0) else yamlToJson(input0)
		} yield input.asJson.asJsObject
	}

}