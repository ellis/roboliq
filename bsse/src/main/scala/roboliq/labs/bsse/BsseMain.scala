package roboliq.labs.bsse

import scala.collection.JavaConversions._

import org.apache.commons.io.FilenameUtils

import roboliq.core._
import roboliq.robots.evoware._
import roboliq.utils.FileUtils

import station1._

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
		println(roboliq.yaml.RoboliqYaml.yamlOut.dump(seqAsJavaList(nodes)))
		println(res.locationTracker.map)
		res.lNode.flatMap(getErrorMessage) match {
			case Nil =>
				translator.translate(res) match {
					case Error(ls) => ls.foreach(println)
					case Success(evowareScript) =>
						// print to screen
						evowareScript.cmds.foreach(println)
						// save to file
						val sProtocolFilename = files.last
						val sScriptFilename = pathbase + FilenameUtils.removeExtension(sProtocolFilename) + ".esc"
						translator.saveWithHeader(evowareScript, sScriptFilename)
						
						// Print plate locations again
						println()
						println("Plate locations:")
						res.locationTracker.map.foreach(println)
						
						// Print tube locations
						println()
						println("Tube locations")
						res.ob.m_mapWell2.foreach(pair => pair._2 match {
							case pos: WellPosition =>
								println(pair._1+": "+pos.idPlate+" row "+(pos.iRow + 1)+" col "+(pos.iCol + 1))
							case _ =>
						})
				}
			case ls =>
				ls.foreach(println)
		}
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

object BsseMain {
	def main(args: Array[String]) {
		//new roboliq.labs.bsse.examples.PrimerTest1().run()
		new YamlTest2(args.toList).run
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
