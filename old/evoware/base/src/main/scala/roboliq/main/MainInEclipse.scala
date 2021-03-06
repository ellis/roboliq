package roboliq.main

import java.io.File

object MainInEclipse extends App {
	def run(name: String, tableName_l: List[String] = Nil) {
		new Runner(Array(
			"--config", "../tasks/autogen/roboliq.yaml",
			"--output", s"testoutput/$name/current",
			"--protocol", s"../tasks/autogen/$name.prot"
		) ++ tableName_l.flatMap(List("--table", _)))
	}
	
	def runTemp(name: String, tableName_l: List[String] = Nil) {
		val filename1 = s"../tasks/autogen/$name.prot"
		val filename2 = s"../tasks/autogen/$name.json"
		val filename = if (new File(filename1).exists) filename1 else filename2
		new Runner(Array(
			"--config", "../tasks/autogen/roboliq.yaml",
			"--output", s"temp/$name",
			"--protocol", filename
		) ++ tableName_l.flatMap(List("--table", _)))
	}
	
	// Select first odd rows then even rows on a 384 well plate
	//println((for { col <- 1 to 24; row <- (0 until 16).toList.map(n => ('A' + n).asInstanceOf[Char]).grouped(2).toList.transpose.flatten } yield { f"$row$col%02d" }).mkString("+"))
	
	//run("test_single_carouselOpenSite_01")
	//run("test_single_evowareTimerSleep_01")
	//run("test_single_measureAbsorbance_01")
	//run("test_single_openDeviceSite_02")
	//run("test_single_pipette_01")
	//run("test_single_pipette_02")
	//run("test_single_pipette_03")
	/*run("test_single_pipette_04")
	run("test_single_pipette_05")
	run("test_single_pipette_06")
	run("test_single_pipette_07")
	run("test_single_pipette_08")*/
	//run("test_single_runDevice_01")
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
	//runTemp("tania08_urea_3_measure")
	//runTemp("tania09_urea_test")
	//runTemp("tania09_urea_test_3_measure")
	//runTemp("tania10_renaturation")
	//runTemp("tania10_renaturation")
	//runTemp("tania10_renaturation_1_pipette", List("mario_withDownholder"))
	//runTemp("tania10_renaturation_2", List("mario_withDownholder"))
	//runTemp("tania11_renaturation_test_2_extract", List("mario_withDownholder"))
	//runTemp("tania10_renaturation_3_measure_hack", List("mario_withDownholder"))
	/*runTemp("tania10_renaturation_3a_measure_hack", List("mario_withDownholder"))
	runTemp("tania10_renaturation_3b_measure_hack", List("mario_withDownholder"))
	runTemp("tania10_renaturation_3c_measure_hack", List("mario_withDownholder"))
	runTemp("tania10_renaturation_3d_measure_hack", List("mario_withDownholder"))
	runTemp("tania10_renaturation_3e_measure_hack", List("mario_withDownholder"))
	runTemp("tania10_renaturation_3f_measure_hack", List("mario_withDownholder"))
	runTemp("tania10_renaturation_3g_measure_hack", List("mario_withDownholder"))
	runTemp("tania10_renaturation_3h_measure_hack", List("mario_withDownholder"))*/
	//runTemp("tania12_denaturation_1_balancePlate")
	//runTemp("tania12_denaturation_2_pipette")
	//runTemp("tania12_denaturation_4_pipette_and_measure")
	//runTemp("tania14_renaturation_A01")
	//runTemp("tania14_renaturation_B01")
	/*runTemp("tania14_renaturation_111")
	runTemp("tania14_renaturation_112")
	runTemp("tania14_renaturation_113")
	runTemp("tania14_renaturation_114")
	runTemp("tania14_renaturation_115")
	runTemp("tania14_renaturation_131")
	runTemp("tania14_renaturation_132")
	runTemp("tania14_renaturation_133")
	runTemp("tania14_renaturation_134")
	runTemp("tania14_renaturation_135")
	runTemp("tania14_renaturation_151")
	runTemp("tania14_renaturation_152")
	runTemp("tania14_renaturation_153")
	runTemp("tania14_renaturation_154")
	runTemp("tania14_renaturation_155")
	runTemp("tania14_renaturation_221")
	runTemp("tania14_renaturation_222")
	runTemp("tania14_renaturation_223")
	runTemp("tania14_renaturation_224")
	runTemp("tania14_renaturation_225")
	runTemp("tania14_renaturation_241")
	runTemp("tania14_renaturation_242")
	runTemp("tania14_renaturation_243")
	runTemp("tania14_renaturation_244")
	runTemp("tania14_renaturation_245")
	runTemp("tania14_renaturation_261")
	runTemp("tania14_renaturation_262")
	runTemp("tania14_renaturation_263")
	runTemp("tania14_renaturation_264")
	runTemp("tania14_renaturation_265")*/
	/*runTemp("tania15_renaturation_11")
	runTemp("tania15_renaturation_12")
	runTemp("tania15_renaturation_13")
	runTemp("tania15_renaturation_14")
	runTemp("tania15_renaturation_15")
	runTemp("tania15_renaturation_21")
	runTemp("tania15_renaturation_22")
	runTemp("tania15_renaturation_23")
	runTemp("tania15_renaturation_24")
	runTemp("tania15_renaturation_25")
	runTemp("tania15_renaturation_31")
	runTemp("tania15_renaturation_32")
	runTemp("tania15_renaturation_33")
	runTemp("tania15_renaturation_34")
	runTemp("tania15_renaturation_35")
	runTemp("tania15_renaturation_41")
	runTemp("tania15_renaturation_42")
	runTemp("tania15_renaturation_43")
	runTemp("tania15_renaturation_44")
	runTemp("tania15_renaturation_45")
	runTemp("tania15_renaturation_51")
	runTemp("tania15_renaturation_52")
	runTemp("tania15_renaturation_53")
	runTemp("tania15_renaturation_54")
	runTemp("tania15_renaturation_55")*/
	//runTemp("tania13_ph_1_balancePlate")
	//runTemp("tania13_ph_2_pipette")
	//runTemp("tania13_ph_3_measure")
	//runTemp("chao01_dye")
	//runTemp("chao04_2nd_dispense_A")
	//runTemp("chao04_2nd_dispense_B")
	//runTemp("chao05_adjusted_d")
	//runTemp("chao06_2nd_dispense")
	//runTemp("chao07_tripple_A")
	//runTemp("chao07_tripple_B")
	//runTemp("chao07_tripple_C")
	//runTemp("qa01_dye_titration")
	//runTemp("qa02_surface_A")
	//runTemp("qa02_surface_B")
	//runTemp("qa03_weight_1")
	//runTemp("qa03_weight_2")
	//runTemp("qa03_weight_3")
	//runTemp("qa03_weight_4")
	//runTemp("qa05_dye_vol_range")
	//runTemp("tania16_ph_2_pipette")
	//runTemp("tania16_ph_3_pipette2")
	runTemp("tania17_denaturation_2_pipette")
	//runTemp("tania17_denaturation_4_pipette_and_measure")
}