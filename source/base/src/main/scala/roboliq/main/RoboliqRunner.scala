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
import roboliq.input.RjsConverter
import roboliq.input.ProtocolDataA
import roboliq.input.ProtocolHandler
import roboliq.input.RjsNull

case class RoboliqOpt(
	step_l: Vector[RoboliqOptStep] = Vector()
)

sealed trait RoboliqOptStep
case class RoboliqOptStep_File(file: File) extends RoboliqOptStep
//case class RoboliqOptStep_Stdin() extends RoboliqOptExpression
case class RoboliqOptStep_Json(json: String) extends RoboliqOptStep
case class RoboliqOptStep_Yaml(yaml: String) extends RoboliqOptStep
case class RoboliqOptStep_Check() extends RoboliqOptStep


object RoboliqRunner {
	def getOptParser: scopt.OptionParser[RoboliqOpt] = new scopt.OptionParser[RoboliqOpt]("roboliq") {
		head("roboliq", "0.1pre1")
		opt[File]('f', "file")
			.valueName("<file>")
			.action { (x, o) =>
				o.copy(step_l = o.step_l :+ RoboliqOptStep_File(x)) 
			}
			.text("expression file")
		opt[String]('j', "json")
			.valueName("<JSON>")
			.action { (x, o) =>
				o.copy(step_l = o.step_l :+ RoboliqOptStep_Json(x)) 
			}
			.text("JSON expression")
		opt[String]('y', "yaml")
			.valueName("<YAML>")
			.action { (x, o) =>
				o.copy(step_l = o.step_l :+ RoboliqOptStep_Yaml(x)) 
			}
			.text("YAML expression")
		opt[Unit]("check")
			.action { (_, o) =>
				o.copy(step_l = o.step_l :+ RoboliqOptStep_Check())
			}
			.text("check the protocol data")
	}

	
	private def getInputHash(opt: RoboliqOpt): String = {
		// TODO: should probably use ResultE.findFile here, to take advantage of search paths
		val contentStep_l = opt.step_l.toList.flatMap {
			case RoboliqOptStep_File(file) =>
				Some(scala.io.Source.fromFile(file).mkString)
			case RoboliqOptStep_Json(s) =>
				Some(s)
			case RoboliqOptStep_Yaml(s) =>
				Some(s)
			case _ =>
				None
		}
		val content = contentStep_l.mkString
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
		val searchPath_l = opt.step_l.flatMap {
			case RoboliqOptStep_File(file) => Some(file.getAbsoluteFile().getParentFile())
			case _ => None
		}
		// FIXME: add search path to ResultE
		for {
			res <- processStep(RjsNull, opt.step_l)
		} yield res
	}
	
	private def processStep(
		res0: RjsValue,
		step_l: Vector[RoboliqOptStep]
	): ResultE[RjsValue] = {
		step_l match {
			case step +: rest =>
				for {
					res1 <- processStep(res0, step)
					res2 <- processStep(res1, rest)
				} yield res2
			case _ =>
				ResultE.unit(res0)
		}
	}
	
	private def processStep(
		res0: RjsValue,
		step: RoboliqOptStep
	): ResultE[RjsValue] = {
		step match {
			case RoboliqOptStep_File(file) =>
				for {
					rjsval <- ResultE.loadRjsFromFile(file)
					res1 <- ResultE.evaluate(rjsval)
					res2 <- ResultE.from(RjsValue.merge(res0, res1))
				} yield res2
			case RoboliqOptStep_Json(s) =>
				val jsval = JsonUtils.textToJson(s)
				for {
					rjsval <- ResultE.from(RjsValue.fromJson(jsval))
					res1 <- ResultE.evaluate(rjsval)
					res2 <- ResultE.from(RjsValue.merge(res0, res1))
				} yield res2
			case RoboliqOptStep_Yaml(s) =>
				val jsval = JsonUtils.yamlToJson(s)
				for {
					rjsval <- ResultE.from(RjsValue.fromJson(jsval))
					res1 <- ResultE.evaluate(rjsval)
					res2 <- ResultE.from(RjsValue.merge(res0, res1))
				} yield res2
			case RoboliqOptStep_Check() =>
				for {
					dataA <- RjsConverter.fromRjs[ProtocolDataA](res0)
					dataB <- new ProtocolHandler().stepB(dataA)
				} yield {
					println("check:")
					println(dataB)
					RjsNull
				} 
		}
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
