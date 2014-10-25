package roboliq.main

object MainInEclipse extends App {
	def run(name: String) {
		new Runner(Array(
			"--config", "../tasks/autogen/roboliq.yaml",
			"--output", s"testoutput/$name/current",
			//"--protocol", "tasks/autogen/tania04_ph.prot"
			"--protocol", s"../tasks/autogen/$name.prot"
		))
	}
	
	def runTemp(name: String) {
		new Runner(Array(
			"--config", "../tasks/autogen/roboliq.yaml",
			"--output", s"temp/$name",
			//"--protocol", "tasks/autogen/tania04_ph.prot"
			"--protocol", s"../tasks/autogen/$name.prot"
		))
	}
	
	// Select first odd rows then even rows on a 384 well plate
	//println((for { col <- 1 to 24; row <- (0 until 16).toList.map(n => ('A' + n).asInstanceOf[Char]).grouped(2).toList.transpose.flatten } yield { f"$row$col%02d" }).mkString("+"))
	
	//run("test_single_carouselOpenSite_01")
	//run("test_single_carouselOpenSite_01")
	//run("test_single_evowareTimerSleep_01")
	//run("test_single_measureAbsorbance_01")
	//run("test_single_pipette_01")
	//run("test_single_pipette_02")
	//run("test_single_pipette_03")
	/*run("test_single_pipette_04")
	run("test_single_pipette_05")
	run("test_single_pipette_06")
	run("test_single_pipette_07")
	run("test_single_pipette_08")*/
	//run("test_single_sealPlate_01")
	//run("test_single_sealPlate_02")
	//run("test_single_sealPlate_03")
	//run("test_single_sealPlate_04")
	//run("test_script_centrifuge_01")
	//run("test_script_wellGroup_01")
	//run("test_script_wellGroup_02")
	//run("test_tubes_01")
	//run("test_tipDistance_01")
	//run("test_tipDistance_02")
	//run("test_bug_01")

	//runTemp("tania04_ph")
	//runTemp("tania06_qc_ph")
	//runTemp("tania07_qc_ph")
	//runTemp("tania08_urea")
	//runTemp("tania08_urea_1_balancePlate")
	//runTemp("tania08_urea_2_pipette")
	runTemp("tania08_urea_3_measure")
	//runTemp("tania09_urea_test")
	//runTemp("tania09_urea_test_3_measure")
}