/*package roboliq.test.bsse

import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import roboliq.core._
import scala.collection.immutable.BitSet
import roboliq.utils.FileUtils



class PipettePlannerSpec extends FunSpec with ShouldMatchers with BeforeAndAfter {
/*
  val script01 = """
plates:
  P1:
    model: D-BSSE 96 Well PCR Plate

events:
- !add { obj: P1(A01), src: water, volume: 100e-6 }

commands:
- !!roboliq.commands.pipette.PipetteCmdBean { src: P1(A01), dest: P1(A02 d H), volume: [5e-6] }
"""
*/
  describe("YamlTest2") {
    val file_l = List(
    	"test002-vol=0000_1.yaml",
    	"test002-vol=0001.yaml",
    	"test002-vol=0002_5.yaml",
    	"test002-vol=0003.yaml",
    	"test002-vol=0050.yaml"
    )
    for (file <- file_l) {
      it(s"should compile: $file") {
        val yamltest2 = new YamlTest2(List(file))
        yamltest2.run
      }
    }
  }
}

class YamlTest2(args: List[String]) {
	import org.yaml.snakeyaml._
	import roboliq.yaml.RoboliqYaml
	import scala.collection.JavaConversions._
	import org.apache.commons.io.FilenameUtils
	import roboliq.core._
	import roboliq.robots.evoware._
	import roboliq.utils.FileUtils
	import roboliq.labs.bsse._
	import station1._
	import java.io.File
	import scala.collection.mutable.Stack
	import java.io.PrintWriter

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

		val doc = new EvowareDoc
		doc.sProtocolFilename = sProtocolFilename
		doc.lNode = nodes
		doc.processorResult = res
		
		//println(roboliq.yaml.RoboliqYaml.yamlOut.dump(seqAsJavaList(nodes)))
		println(res.locationTracker.map)
		res.lNode.flatMap(getErrorMessage) match {
			case Nil =>
				translator.translate(res) match {
					case Error(ls) =>
						doc.lsTranslatorError = ls.toList
						ls.foreach(println)
					case Success(evowareScript) =>
						doc.evowareScript = evowareScript
						// save to file
						val sScriptFilename = sBasename+".esc"
						translator.saveWithHeader(evowareScript, sScriptFilename)
				}
			case ls =>
				doc.lsProcessorError = ls
				ls.foreach(println)
		}
		
		doc.printToFile(sBasename+".html")
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