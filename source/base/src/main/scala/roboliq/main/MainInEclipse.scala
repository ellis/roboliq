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
			"--output", s"temp",
			//"--protocol", "tasks/autogen/tania04_ph.prot"
			"--protocol", s"../tasks/autogen/$name.prot"
		))
	}
	
	//run("test_single_pipette_01")
	//run("test_single_pipette_02")
	//run("test_single_pipette_03")
	/*run("test_single_pipette_04")
	run("test_single_pipette_05")
	run("test_single_pipette_06")
	run("test_single_pipette_07")
	run("test_single_pipette_08")*/
	//run("test_script_wellGroup_01")
	//run("test_script_wellGroup_02")
	//run("test_tubes_01")
	//runTemp("tania04_ph")
	//runTemp("tania06_qc_ph")
	runTemp("tania07_qc_ph")
	//run("test_tipDistance_01")
	//run("test_tipDistance_02")
}