package roboliq.labs.bsse

import scala.collection.JavaConversions._
import org.apache.commons.io.FilenameUtils
import roboliq.core._, roboliq.processor._
import roboliq.commands._
import roboliq.robots.evoware._
import roboliq.utils.FileUtils
import station1._
import java.io.File
import scala.collection.mutable.Stack
import java.io.PrintWriter


object JsonTest {
	import spray.json.JsonParser
	import roboliq.processor.ProcessorData
	
	def run(args: List[String]) {
		if (args.isEmpty) {
			println("Please pass a json file containing commands")
		}
		
		// TODO: should load the handlers via a yaml config file
		val processor = new ProcessorData(List(
			new arm.MovePlateHandler,
			new pipette.TipsHandler_Fixed,
			new pipette.TransferHandler,
			new pipette.low.AspirateHandler,
			new pipette.low.DispenseHandler,
			new pipette.low.MixHandler,
			new pipette.low.WashTipsHandler
		))
	
		val pathbase = "testdata/bsse-robot1/"
		val databaseFiles = List(
			"config/bench-01.json",
			"config/database-01.json"
		)
		databaseFiles.map(s => processor.loadJsonData(new java.io.File(pathbase + s)))
		
		processor.loadJsonData(new java.io.File(pathbase + args(0)))
		processor.run()
	
		val pathToToken_l = processor.getTokenList
		val token_l = pathToToken_l.map(_._2)
	
		val evowareConfigFile = new EvowareConfigFile(pathbase+"config/carrier.cfg")
		val evowareTable = new StationConfig(evowareConfigFile, pathbase+"config/bench-01.esc")
		val config = new EvowareConfig(evowareTable.tableFile, evowareTable.mapLabelToSite)
		val translator = new EvowareTranslator(config)

		val sProtocolFilename = pathbase + args(0)
		val sBasename = FilenameUtils.removeExtension(sProtocolFilename)
		//val yamlOut = roboliq.yaml.RoboliqYaml.yamlOut
		//FileUtils.writeToFile(sBasename+".cmd", yamlOut.dump(seqAsJavaList(cmds)))
		FileUtils.writeToFile(sBasename+".out", token_l.mkString("\n"))

		/*val doc = new EvowareDoc
		doc.sProtocolFilename = sProtocolFilename
		doc.lNode = nodes
		doc.processorResult = res
		*/
		
		//println(roboliq.yaml.RoboliqYaml.yamlOut.dump(seqAsJavaList(nodes)))
		translator.translate(token_l) match {
			case RqError(e, w) =>
				//doc.lsTranslatorError = ls.toList
				e.foreach(println)
				w.foreach(println)
			case RqSuccess(evowareScript, w) =>
				//doc.evowareScript = evowareScript
				// save to file
				val sScriptFilename = sBasename+".esc"
				translator.saveWithHeader(evowareScript, sScriptFilename)
		}
		
		//doc.printToFile(sBasename+".html")
	}
}

/*
class YamlTest2(args: List[String]) {
	import org.yaml.snakeyaml._
	import roboliq.yaml.RoboliqYaml

	val sHome = System.getProperty("user.home")
	val pathbase = sHome+"/src/roboliq/testdata/"

	val filesDefault = List(
		"classes-bsse1-20120408.yaml",
		"robot-bsse1-20120408.yaml",
		"database-001-20120408.yaml",
		"protocol-001-20120408.yaml"
	//"protocol-002-20120409.yaml"
	)
	val files = {
		if (args.isEmpty) filesDefault
		// Use default file list, but replace the protocol file
		else if (args.tail.isEmpty) filesDefault.init ++ args
		// Only use files passed by user
		else args
	}

	val beans = files.map(s => RoboliqYaml.loadFile(pathbase + s))

	val bb = new BeanBase
	beans.foreach(bb.addBean)
	val ob = new ObjBase(bb)

	val builder = new StateBuilder(ob)
	val processor = Processor(bb, builder.toImmutable)
	val cmds = beans.last.commands.toList
	val res = processor.process(cmds)
	val nodes = res.lNode

	val evowareConfigFile = new EvowareConfigFile(pathbase+"tecan-bsse1-20120408/carrier.cfg")
	//val evowareTableFile = EvowareTableParser.parseFile(evowareConfigFile, pathbase+"tecan-bsse1-20120408/bench1.esc")
	val evowareTable = new StationConfig(evowareConfigFile, pathbase+"tecan-bsse1-20120408/bench1.esc")
	val config = new EvowareConfig(evowareTable.tableFile, evowareTable.mapLabelToSite)
	val translator = new EvowareTranslator(config)

	def run {
		val sProtocolFilename = files.last
		val sBasename = pathbase + FilenameUtils.removeExtension(sProtocolFilename)
		val yamlOut = roboliq.yaml.RoboliqYaml.yamlOut
		FileUtils.writeToFile(sBasename+".cmd", yamlOut.dump(seqAsJavaList(cmds)))
		FileUtils.writeToFile(sBasename+".out", yamlOut.dump(seqAsJavaList(nodes)))

		/*val doc = new EvowareDoc
		doc.sProtocolFilename = sProtocolFilename
		doc.lNode = nodes
		doc.processorResult = res*/
		
		//println(roboliq.yaml.RoboliqYaml.yamlOut.dump(seqAsJavaList(nodes)))
		println(res.locationTracker.map)
		res.lNode.flatMap(getErrorMessage) match {
			case Nil =>
				translator.translate(res) match {
					case Error(ls) =>
						//doc.lsTranslatorError = ls.toList
						ls.foreach(println)
					case Success(evowareScript) =>
						//doc.evowareScript = evowareScript
						// save to file
						val sScriptFilename = sBasename+".esc"
						translator.saveWithHeader(evowareScript, sScriptFilename)
				}
			case ls =>
				//doc.lsProcessorError = ls
				ls.foreach(println)
		}
		
		//doc.printToFile(sBasename+".html")
	}
	
	private def getErrorMessage(node: CmdNodeBean): List[String] = {
		if (node.errors != null) {
			node.errors.toList.map(node.index+": "+_)
		}
		else if (node.children != null) {
			node.children.flatMap(getErrorMessage).toList
		}
		else
			Nil
	}
	//def getErrorMessages(lNode: List[CmdNodeBean]): List[String] =
		
}
*/

object BsseMain {
	def main(args: Array[String]) {
		//new roboliq.labs.bsse.examples.PrimerTest1().run()
		//new YamlTest2(args.toList).run
		JsonTest.run(args.toList)
	}

	/*
	def runProtocol(lItem: List[roboliq.protocol.Item], db: ItemDatabase, sFilename: String) {
		val sHome = System.getProperty("user.home")
		val configFile = new EvowareConfigFile(sHome+"/tmp/tecan/carrier.cfg")
		val station = new StationConfig(configFile, sHome+"/src/roboliq/ellis_pcr1_corrected.esc")
		val toolchain = new BsseToolchain(station)
		val (kb, cmds) = ExampleRunner.run(lItem, db)
		toolchain.compile(kb, cmds, true) match {
			case Left(err) => err.print()
			case Right(succT: TranslatorStageSuccess) =>
				val script = succT.internal.asInstanceOf[EvowareScriptBuilder]
				val translator = new EvowareTranslator(toolchain.evowareConfig)
				val s = translator.saveWithHeader(script.cmds.toSeq, station.tableFile, script.mapCmdToLabwareInfo.toMap, sFilename)
				println(s)
			case Right(succ) => succ.print()
		}
	}
	
	//runProtocol(new PcrExample4().l, PcrExample4Database.db, "ellis_pcr4.esc")
	runProtocol(new PcrExample5().l, PcrExample5Database.db, "ellis_pcr5.esc")
	//new examples.PrimerTest1().run()
	*/

	/*
	// Attempt at touchdown PCR for phusion hotstart 
	FileUtils.writeToFile("test.tpb", TRobotProgram.generateTouchdown(
		0,
		6,
		"touchdn",
		nBoilingTemp = 98,
		nExtensionTemp = 72,
		nExtensionTime = 30,
		nAnnealingTemp1 = 68,
		nAnnealingTemp2 = 58,
		nAnnealingTime = 20,
		nFinalReps = 16
	))
	*/
	/*
	// Attempt at touchdown PCR for taq
	FileUtils.writeToFile("tchdntaq.tpb", TRobotProgram.generateTouchdown(
		0,
		7,
		"tchdntaq",
		nBoilingTemp = 95,
		nExtensionTemp = 72,
		nExtensionTime = 90,
		nAnnealingTemp1 = 65,
		nAnnealingTemp2 = 55,
		nAnnealingTime = 20,
		nFinalReps = 15
	))
	*/
}
