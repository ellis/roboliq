package roboliq.labs.bsse

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.protocol.ItemDatabase
import roboliq.devices.pipette._
import roboliq.robots.evoware._
import roboliq.labs.bsse.examples._
import roboliq.utils.FileUtils

import station1._


object BsseMain extends App {
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
	//runProtocol(new PcrExample5().l, PcrExample5Database.db, "ellis_pcr5.esc")
	//new examples.PrimerTest1().run()

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
}
