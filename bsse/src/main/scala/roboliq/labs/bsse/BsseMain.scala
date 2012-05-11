package roboliq.labs.bsse

import scala.collection.JavaConversions._
import org.apache.commons.io.FilenameUtils
import roboliq.core._
import roboliq.robots.evoware._
import roboliq.utils.FileUtils
import station1._
import java.io.File
import scala.collection.mutable.Stack

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
		FileUtils.printToFile(new File(sBasename+".html")) {html =>
			val tags = new Stack[String]
			def docPrintln(o: Any) {
				val sIndent = "\t" * tags.size
				html.println(sIndent+o)
			}
			def tagPush(tag: String, attributes: Map[String, String] = Map()) {
				val s0 = "<"+tag;
				val s1 = {
					if (attributes.isEmpty) ""
					else " "+attributes.map(pair => pair._1+"=\""+pair._2+"\"").mkString(" ")
				}
				docPrintln(s0+s1+">")
				tags.push(tag)
			}
			def tagPop() {
				if (!tags.isEmpty) {
					docPrintln("</"+tags.pop()+">")
				}
			}
			def tagMake(tag: String, attributes: Map[String, String] = Map(), contents: String = null) {
				val s0 = "<"+tag;
				val s1 = {
					if (attributes.isEmpty) ""
					else " "+attributes.map(pair => pair._1+"=\""+pair._2+"\"").mkString(" ")
				}
				val s2 = {
					if (contents == null) "/>"
					else ">"+contents+"</"+tag+">";
				}
				docPrintln(s0+s1+s2)
			}
			def docHeader(sTitle: String, iLevel: Int) {
				tagMake("h"+iLevel, Map(), sTitle)
			}
			
			tagPush("html", Map())
			tagPush("body", Map())
			
			// Title
			docHeader("Documentation for file "+sProtocolFilename, 1)
			
			// Date
			tagMake("p", Map(), "Date: "+(new java.util.Date))

			// Input command docs
			docHeader("Protocol", 2)
			nodes.foreach(node => {
				docHeader("Step "+node.lIndex.last, 3);
				docPrintln(node.doc)
			})
			
			//println(roboliq.yaml.RoboliqYaml.yamlOut.dump(seqAsJavaList(nodes)))
			println(res.locationTracker.map)
			res.lNode.flatMap(getErrorMessage) match {
				case Nil =>
					translator.translate(res) match {
						case Error(ls) =>
							docHeader("Translation Errors", 2)
							ls.foreach(s => docPrintln("#. "+s))
							ls.foreach(println)
						case Success(evowareScript) =>
							// print to screen
							//evowareScript.cmds.foreach(println)
							// save to file
							val sScriptFilename = sBasename+".esc"
							translator.saveWithHeader(evowareScript, sScriptFilename)
							
							docHeader("Initial Bench Setup", 2)

							// Print plate locations again
							tagPush("table", Map("border" -> "1"))
							tagPush("tr")
								tagMake("th", Map("colspan" -> "2"), "Plates")
							tagPop()
							tagPush("tr")
								tagMake("th", Map(), "ID")
								tagMake("th", Map(), "Location")
							tagPop()
							res.locationTracker.map.foreach(item => {
								tagPush("tr")
								tagMake("td", Map(), item._1)
								if (!item._2.isEmpty)
									tagMake("td", Map(), item._2.head._2)
								tagPop()
							})
							
							// Print tube locations
							val mapTubeLoc = res.ob.m_mapWell2.filter(pair => pair._2.isInstanceOf[WellPosition])
							if (!mapTubeLoc.isEmpty) {
								tagPush("tr")
									tagMake("th", Map("colspan" -> "2"), "Tubes")
								tagPop()
								tagPush("tr")
									tagMake("th", Map(), "ID")
									tagMake("th", Map(), "Location")
								tagPop()
								mapTubeLoc.foreach(pair => pair._2 match {
									case pos: WellPosition =>
										tagPush("tr")
										tagMake("td", Map(), pair._1)
										tagMake("td", Map(), pos.idPlate+" row "+(pos.iRow + 1)+" col "+(pos.iCol + 1))
										tagPop()
									case _ =>
								})
							}
							tagPop()
					}
				case ls =>
					ls.foreach(println)
			}
			
			docHeader("Robot Commands", 2)
			def printDoc(lNode: java.util.List[CmdNodeBean], nIndent: Int) {
				if (lNode.exists(_.doc != null)) {
					tagPush("ul")
					val sIndent = " " * (nIndent * 2)
					lNode.foreach(node => {
						val sDoc = {
							if (node.doc != null && node.doc.size > 50)
								node.doc.take(50)+"..."
							else
								node.doc
						}
						if (node.children != null) {
							tagPush("li")
							docPrintln(sDoc)
							printDoc(node.children, nIndent + 1)
							tagPop()
						}
						else {
							tagMake("li", Map(), sDoc)
						}
					})
					tagPop()
				}
			}
			printDoc(nodes, 0)
			
			tagPop()
			tagPop()
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
