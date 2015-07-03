package roboliq.evoware.translator

import java.io.File
import roboliq.core.ResultC
import roboliq.evoware.parser.EvowareCarrierData
import roboliq.utils.FileUtils
import roboliq.utils.JsonUtils

case class EvowareCompilerOpt(
	carrierFile_? : Option[File] = None,
	tableFile_? : Option[File] = None,
	protocolFile_? : Option[File] = None
)

object EvowareCompilerMain extends App {
	def getOptParser(): scopt.OptionParser[EvowareCompilerOpt] = new scopt.OptionParser[EvowareCompilerOpt]("roboliq evoware compiler") {
		head("roboliq evoware compiler", "0.1pre1")
		arg[File]("<carrier>")
			.action { (x, o) =>
				o.copy(carrierFile_? = Some(x)) 
			}
			.text("path to Evoware Carrier.cfg")
		arg[File]("<table>").optional()
			.action { (x, o) =>
				o.copy(tableFile_? = Some(x)) 
			}
			.text("path to Evoware table file (.ewt)")
		arg[File]("<protocol>").optional()
			.action { (x, o) =>
				o.copy(protocolFile_? = Some(x)) 
			}
			.text("path to roboliq protocol")
	}

	def run(opt: EvowareCompilerOpt) {
		println("run")
		val compiler = new EvowareCompiler("ourlab.mario.evoware", false)
		val table_l: List[String] = List(/*"mario.default"*/)
		val searchPath_l: List[File] = Nil
		
		val result_? = (opt.carrierFile_?, opt.tableFile_?, opt.protocolFile_?) match {
			case (Some(carrierFile), None, None) =>
				for {
					carrierData <- ResultC.from(EvowareCarrierData.loadFile(carrierFile.getPath))
				} yield {
					carrierData.printCarriersById
					()
				}
			case (Some(carrierFile), Some(tableFile), None) =>
				for {
					carrierData <- ResultC.from(EvowareCarrierData.loadFile(carrierFile.getPath))
					tableData <- ResultC.from(roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile.getPath))
				} yield {
					tableData.print()
				}
			case (Some(carrierFile), Some(tableFile), Some(protocolFile)) =>
				for {
					carrierData <- ResultC.from(EvowareCarrierData.loadFile(carrierFile.getPath))
					tableData <- ResultC.from(roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile.getPath))
					protocolText = org.apache.commons.io.FileUtils.readFileToString(protocolFile, "UTF-8")
					protocol = JsonUtils.textToJson(protocolText).asJsObject
					token_l <- compiler.buildTokens(protocol)
					script_l = compiler.buildScripts(token_l)
					content_l <- compiler.generateScriptContents(tableData, "test", script_l)
				} yield {
					// Save scripts
					val dir = new java.io.File(".")
					dir.mkdirs()
					for ((filename, bytes) <- content_l) {
						val file = new java.io.File(dir, filename)
						FileUtils.writeBytesToFile(file, bytes)
					}
					()
				}
			case _ => ResultC.unit(())
		}

		val (resultData, _) = result_?.run()
		if (!resultData.error_r.isEmpty) {
			System.err.println("ERRORS:")
			resultData.error_r.reverse.foreach(s => System.err.println("ERROR: "+s))
		}
		if (!resultData.warning_r.isEmpty) {
			System.err.println("WARNINGS:")
			resultData.error_r.reverse.foreach(s => System.err.println("WARNING: "+s))
		}
	}
	
	val parser = getOptParser()
	parser.parse(args, EvowareCompilerOpt()) map { opt =>
		run(opt)
	} getOrElse {
		// arguments are bad, usage message will have been displayed
	}
}
