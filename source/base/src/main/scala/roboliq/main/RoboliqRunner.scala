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
import roboliq.input.ProtocolHandler
import roboliq.input.RjsNull
import roboliq.input.EvaluatorState
import roboliq.input.RjsProtocol
import roboliq.input.ProtocolDetails
import roboliq.input.RjsAbstractMap
import roboliq.evoware.config.EvowareAgentConfig
import roboliq.evoware.config.EvowareProtocolDataGenerator

case class RoboliqOpt(
	step_l: Vector[RoboliqOptStep] = Vector()
)

sealed trait RoboliqOptStep
case class RoboliqOptStep_File(file: File) extends RoboliqOptStep
//case class RoboliqOptStep_Stdin() extends RoboliqOptExpression
case class RoboliqOptStep_Json(json: String) extends RoboliqOptStep
case class RoboliqOptStep_Yaml(yaml: String) extends RoboliqOptStep
case class RoboliqOptStep_Check() extends RoboliqOptStep

private case class StepResult(
	protocol_? : Option[(RjsProtocol, EvaluatorState)] = None,
	data: RjsValue = RjsNull
)

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
		println(s"process($opt)")
		// Get directories of loaded files
		val searchPath_l = opt.step_l.flatMap {
			case RoboliqOptStep_File(file) => Some(file.getAbsoluteFile().getParentFile())
			case _ => None
		}
		for {
			// add directories of loaded files to ResultE search path
			_ <- ResultE.modify(state => {
				val l = (state.searchPath_l ++ searchPath_l).distinct
				state.copy(searchPath_l = l) 
			})
			result <- processSteps(StepResult(), opt.step_l)
		} yield result.data
	}
	
	private def processSteps(
		result0: StepResult,
		step_l: Vector[RoboliqOptStep]
	): ResultE[StepResult] = {
		println(s"processSteps(${step_l}):")
		step_l match {
			case step +: rest =>
				for {
					result1 <- processStep(result0, step)
					result2 <- processSteps(result1, rest)
				} yield result2
			case _ =>
				ResultE.unit(result0)
		}
	}
	
	private def processStep(
		result0: StepResult,
		step: RoboliqOptStep
	): ResultE[StepResult] = {
		println(s"processStep($step):")
		step match {
			case RoboliqOptStep_File(file) =>
				for {
					rjsval <- ResultE.loadRjsFromFile(file)
					result1 <- processStepRjs(result0, rjsval)
				} yield result1
			case RoboliqOptStep_Json(s) =>
				for {
					rjsval <- ResultE.from(RjsValue.fromJsonText(s))
					result1 <- processStepRjs(result0, rjsval)
				} yield result1
			case RoboliqOptStep_Yaml(s) =>
				for {
					rjsval <- ResultE.from(RjsValue.fromYamlText(s))
					result1 <- processStepRjs(result0, rjsval)
				} yield result1
			case RoboliqOptStep_Check() =>
				processStepCheck(result0)
		}
	}
	
	private def processStepRjs(
		result0: StepResult,
		rjsval: RjsValue
	): ResultE[StepResult] = {
		println(s"processStepRjs($rjsval):")
		for {
			res1 <- ResultE.evaluate(rjsval)
			result1 <- res1 match {
				case m0: RjsAbstractMap =>
					m0.typ_? match {
						case None =>
							for {
								data <- ResultE.from(RjsValue.merge(result0.data, res1))
							} yield result0.copy(data = data)
						case Some(typ) =>
							typ match {
								case "protocol" =>
									for {
										protocol <- RjsConverter.fromRjs[RjsProtocol](m0)
										state <- ResultE.get
									} yield result0.copy(protocol_? = Some((protocol, state)))
								case "EvowareAgent" =>
									processStepRjs_EvowareAgent(result0, m0)
							}
					}
				case x: RjsProtocol =>
					for {
						state <- ResultE.get
					} yield result0.copy(protocol_? = Some((x, state)))
				case _ =>
					for {
						data <- ResultE.from(RjsValue.merge(result0.data, res1))
					} yield result0.copy(data = data)
			}
		} yield result1
	}
	
	private def processStepRjs_EvowareAgent(
		result0: StepResult,
		m0: RjsValue with RjsAbstractMap
	): ResultE[StepResult] = {
		println(s"processStepRjs_EvowareAgent(${m0}):")
		for {
			evowareAgentConfig <- RjsConverter.fromRjs[EvowareAgentConfig](m0)
			searchPath_l <- ResultE.gets(_.searchPath_l)
			_ = println("B")
			details <- ResultE.from(EvowareProtocolDataGenerator.createProtocolData(
				agentIdent = "mario",
				agentConfig = evowareAgentConfig,
				table_l = List("mario.default"),
				searchPath_l = searchPath_l
			))
			_ = println("C")
			result <- ResultE.from(RjsValue.toBasicValue(details))
			_ = println("D")
			data <- ResultE.from(RjsValue.merge(result0.data, result))
			_ = println("E")
		} yield {
			result0.copy(data = data)
		}
	}
	
	private def processStepCheck(
		result: StepResult
	): ResultE[StepResult] = {
		println(s"processStepCheck($result):")
		val protocolHandler = new ProtocolHandler
		for {
			pair <- ResultE.from(result.protocol_?, s"You must supply a protocol")
			(protocol, state) = pair
			details0 <- ResultE.from(protocolHandler.extractDetails(protocol))
			details1 <- RjsConverter.fromRjs[ProtocolDetails](result.data)
			details2 <- ResultE.from(details0 merge details1)
			details3 <- new ProtocolHandler().expandCommands(details2)
			//_ = println("dataB.commandExpansions: "+dataB.commandExpansions)
			result <- ResultE.from(RjsValue.toBasicValue(details3))
		} yield {
			//println("check:")
			//println(dataB)
			StepResult(None, result)
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
