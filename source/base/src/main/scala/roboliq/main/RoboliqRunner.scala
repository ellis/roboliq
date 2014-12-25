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

case class RoboliqOpt(
	expression_l: Vector[RoboliqOptExpression] = Vector()
)

sealed trait RoboliqOptExpression
case class RoboliqOptExpression_File(file: File) extends RoboliqOptExpression
//case class RoboliqOptExpression_Stdin() extends RoboliqOptExpression
case class RoboliqOptExpression_Json(json: String) extends RoboliqOptExpression
case class RoboliqOptExpression_Yaml(yaml: String) extends RoboliqOptExpression

object RoboliqRunner {
	def getOptParser: scopt.OptionParser[RoboliqOpt] = new scopt.OptionParser[RoboliqOpt]("roboliq") {
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
		val time0 = System.currentTimeMillis()
		
		val res = process(opt)

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
	
	def process(opt: RoboliqOpt): ResultE[RjsValue] = {
		val searchPath_l = opt.expression_l.flatMap {
			case RoboliqOptExpression_File(file) => Some(file.getAbsoluteFile().getParentFile())
			case _ => None
		}
		
		for {
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
	}
}

class RoboliqRunner(args: Array[String]) {
	private val logger = Logger[this.type]

	val parser = RoboliqRunner.getOptParser
	
	parser.parse(args, RoboliqOpt()) map { opt =>
		RoboliqRunner.run(opt)
	} getOrElse {
		// arguments are bad, usage message will have been displayed
	}
}
