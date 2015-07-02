package roboliq.evoware.translator

import java.io.File
import roboliq.core.ResultC
import roboliq.evoware.parser.EvowareCarrierData
import roboliq.utils.FileUtils
import roboliq.utils.JsonUtils

object EvowareCompilerMain extends App {
	val configFilename0 = args(0)
	val tableFilename0 = args(1)
	val protocolFilename0 = args(2)
	
	val compiler = new EvowareCompiler("ourlab.mario.evoware", false)
	val table_l: List[String] = List(/*"mario.default"*/)
	val searchPath_l: List[File] = Nil
	
	
	val result_? = for {
		configFile <- ResultC.from(FileUtils.findFile(configFilename0, searchPath_l))
		tableFile <- ResultC.from(FileUtils.findFile(tableFilename0, searchPath_l))
		protocolFile <- ResultC.from(FileUtils.findFile(protocolFilename0, searchPath_l))
		// Load carrier file
		carrierData <- ResultC.from(EvowareCarrierData.loadFile(configFile.getPath))
		filename <- ResultC.from(FileUtils.findFile(tableFile.getPath, searchPath_l))
		tableData <- ResultC.from(roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, filename.getPath))
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
			assert(FileUtils.contentEquals(file, new java.io.File(dir, "output.esc")))
		}
		()
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