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
import roboliq.input.ResultE
import roboliq.utils.JsonUtils
import roboliq.input.RjsValue

private case class RoboliqOpt(
	expression_l: Vector[RoboliqOptExpression] = Vector()
)

private sealed trait RoboliqOptExpression
private case class RoboliqOptExpression_File(file: File) extends RoboliqOptExpression
//private case class RoboliqOptExpression_Stdin() extends RoboliqOptExpression
private case class RoboliqOptExpression_Json(json: String) extends RoboliqOptExpression
private case class RoboliqOptExpression_Yaml(yaml: String) extends RoboliqOptExpression

class RoboliqRunner(args: Array[String]) {
	private val logger = Logger[this.type]

	private val parser = new scopt.OptionParser[RoboliqOpt]("roboliq") {
		head("roboliq", "0.1pre1")
		opt[File]("file")
			.valueName("<file>")
			.action { (x, o) =>
				o.copy(expression_l = o.expression_l :+ RoboliqOptExpression_File(x)) 
			}
			.text("expression file")
		opt[String]("json")
			.valueName("<JSON>")
			.action { (x, o) =>
				o.copy(expression_l = o.expression_l :+ RoboliqOptExpression_Json(x)) 
			}
			.text("JSON expression")
		opt[String]("yaml")
			.valueName("<YAML>")
			.action { (x, o) =>
				o.copy(expression_l = o.expression_l :+ RoboliqOptExpression_Yaml(x)) 
			}
			.text("YAML expression")
	}
	parser.parse(args, RoboliqOpt()) map { opt =>
		run(opt)
	} getOrElse {
		// arguments are bad, usage message will have been displayed
	}
	
	private def getInputHash(opt: RoboliqOpt): String = {
		// TODO: should probably use ResultE.findFile here, to take advantage of search paths
		val contentExpression_l = opt.expression_l.toList.map {
			case RoboliqOptExpression_File(file) =>
				scala.io.Source.fromFile(file).mkString
			case RoboliqOptExpression_Json(s) =>
				s
			case RoboliqOptExpression_Yaml(s) =>
				s
		}
		val content = contentExpression_l.mkString
		roboliq.utils.MiscUtils.md5Hash(content)
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
	
	def run(opt: RoboliqOpt) {
		import scala.sys.process._
		val protocol = new Protocol
		val time0 = System.currentTimeMillis()
		val searchPath_l = opt.expression_l.flatMap {
			case RoboliqOptExpression_File(file) => Some(file.getAbsoluteFile().getParentFile())
			case _ => None
		}
		
		val res = for {
			rjsval_l <- ResultE.map(opt.expression_l.toList) {
				case RoboliqOptExpression_File(file) =>
					ResultE.loadRjsFromFile(file)
				case RoboliqOptExpression_Json(s) =>
					val jsval = JsonUtils.textToJson(s)
					ResultE.from(RjsValue.fromJson(jsval))
				case RoboliqOptExpression_Yaml(s) =>
					val jsval = JsonUtils.yamlToJson(s)
					ResultE.from(RjsValue.fromJson(jsval))
			}
			result_l <- ResultE.map(rjsval_l) { rjsval =>
				ResultE.evaluate(rjsval)
			}
			res <- ResultE.from(RjsValue.merge(result_l))
		} yield res

		/*val error_l = x.getErrors
		val warning_l = x.getWarnings
		if (!error_l.isEmpty || !warning_l.isEmpty) {
			println("Warnings and Errors:")
			error_l.foreach(s => println("ERROR: "+s))
			warning_l.foreach(s => println("WARNING: "+s))
			println()
		}*/
		val time1 = System.currentTimeMillis()
		println(s"DONE (${((time1 - time0)/1000).asInstanceOf[Int]} seconds)")
	}
	
	private def getInstructions(
		cs: CommandSet,
		planInfo_l: List[OperatorInfo],
		originalActionCount: Int,
		indexToOperator_l: List[(Int, roboliq.ai.strips.Operator)]
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
		operator: roboliq.ai.strips.Operator
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
	
	private def loadCommandJson(file: File): RsResult[JsValue] = {
		for {
			_ <- RsResult.assert(file.exists, s"File not found: ${file.getPath}")
			bYaml <- FilenameUtils.getExtension(file.getPath).toLowerCase match {
				case "json" => RsSuccess(false)
				case "yaml" => RsSuccess(true)
				case ext => RsError(s"unrecognized command file extension `$ext`.  Expected either json or yaml.")
			}
			input0 = FileUtils.readFileToString(file)
			input <- if (bYaml) yamlToJson(input0) else RsSuccess(input0) 
		} yield input.parseJson
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
