package roboliq.labs.bsse

import scala.collection.JavaConversions._
import org.apache.commons.io.FilenameUtils
import roboliq.core._, roboliq.entity._, roboliq.processor._
import roboliq.commands._
import roboliq.robots.evoware._
import roboliq.utils.FileUtils
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
			"config/robot.json",
			"config/database-01.json"
		)
	
		val sProtocolFilename = pathbase + args(0)
		val sBasename = FilenameUtils.removeExtension(sProtocolFilename)
		
		val message = for {
			// Load carrier.cfg
			carrierData <- EvowareCarrierData.loadFile(pathbase+"config/carrier.cfg")
			// Load .esc file for table template
			tableData <- EvowareTableData.loadFile(carrierData, pathbase+"config/table-01.esc")
			// Load user-defined table config
			configData <- EvowareConfigData.loadFile(pathbase+"config/table-01.yaml")
			// Load liquid classes
			defaultLcs <- EvowareLiquidClassParser.parseFile(pathbase+"config/DefaultLCs.XML")
			customLcs <- EvowareLiquidClassParser.parseFile(pathbase+"config/CustomLCs.XML")
			// Load evoware entities into processor
			entityData <- EvowareEntityData.createEntities(carrierData, tableData, configData, defaultLcs ++ customLcs)
			_ <- RqResult.toResultOfList(entityData.pipettePolicy_l.map(processor.loadEntity[PipettePolicy]))
			_ <- RqResult.toResultOfList(entityData.plateModel_l.map(processor.loadEntity[PlateModel]))
			_ <- RqResult.toResultOfList(entityData.plateLocation_l.map(processor.loadEntity[PlateLocation]))
			_ <- RqResult.toResultOfList(entityData.plate_l.map(processor.loadEntity[Plate]))
			_ <- RqResult.toResultOfList(entityData.plateState_l.map(processor.loadEntity[PlateState]))

			// Load entities from files
			_ <- RqResult.toResultOfList(databaseFiles.map(s => processor.loadJsonData(new java.io.File(pathbase + s))))
			// Load entities and commands from file passed on the command line
			_ <- processor.loadJsonData(new java.io.File(pathbase + args(0)))

			// Try to run the commands, returning a processor graph
			graph = processor.run()
			// write graph as HTML for debugging
			_ = org.apache.commons.io.FileUtils.writeStringToFile(new java.io.File("lastrun.html"), graph.toHtmlTable)

			pathToToken_l = processor.getTokenList
			token_l = pathToToken_l.map(_._2)

			//val yamlOut = roboliq.yaml.RoboliqYaml.yamlOut
			//FileUtils.writeToFile(sBasename+".cmd", yamlOut.dump(seqAsJavaList(cmds)))
			_ = FileUtils.writeToFile(sBasename+".out", token_l.mkString("", "\n", "\n"))
	
			/*val doc = new EvowareDoc
			doc.sProtocolFilename = sProtocolFilename
			doc.lNode = nodes
			doc.processorResult = res
			*/
			
			config = new EvowareConfig(carrierData, tableData, configData)
			translator = new EvowareTranslator(config)
			script <- translator.translate(token_l)
		} yield {
			val sScriptFilename = sBasename+".esc"
			translator.saveWithHeader(script, sScriptFilename)
			()
		}
		
		val l0 = processor.getMessages
		if (!l0.isEmpty) {
			println("Proccessor Messages")
			l0.foreach(println)
		}
		
		message match {
			case RqError(e, w) =>
				println("Errors:")
				e.foreach(println)
				w.foreach(println)
			case RqSuccess(_, w) =>
				if (!w.isEmpty) {
					println("Warnings:")
					w.foreach(println)
				}
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
