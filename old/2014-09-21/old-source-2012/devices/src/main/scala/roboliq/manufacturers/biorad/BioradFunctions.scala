package roboliq.manufacturers.biorad

import java.io.File

import roboliq.utils.FileUtils
//import roboliq.common._


object BioradFunctions {
	def makeBioradPlateFile(
		dir: File,
		sNamePlate: String,
		lsNameSample: Array[String],
		rowcol0: Tuple2[Int, Int]
	) {
		val nRows = 8
		val nCols = 12
		val iWell0 = rowcol0._2 * nRows + rowcol0._1
		// Create 96 lines enumerated first across columns (0 = A1, 2 = A2, 12 = B1, etc)
		val lsLine = List.tabulate(nCols, nRows)((iCol, iRow) => {
			val i = iCol * nRows + iRow - iWell0
			if (i >= 0 && i < lsNameSample.size) lsNameSample(i) else ""
		}).flatMap(identity)
		FileUtils.printToFile(new File(dir, sNamePlate+".biorad.csv"))(p => lsLine.foreach(p.println))
	}
}