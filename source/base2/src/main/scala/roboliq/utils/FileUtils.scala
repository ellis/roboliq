package roboliq.utils

import java.io.File
import roboliq.core.ResultC

object FileUtils {
	def writeToFile(fileName: String, data: String) {
		printToFile(new File(fileName))(p => p.print(data))
	}
	
	def writeBytesToFile(file: java.io.File, content: Array[Byte]) {
		// Save to file system
		val os = new java.io.FileOutputStream(file)
		try { os.write(content) }
		finally { os.close() }
	}

	/*
	def appendToFile(fileName:String, textData:String) =
		using (new FileWriter(fileName, true)){ 
		fileWriter => using (new PrintWriter(fileWriter)) {
			printWriter => printWriter.println(textData)
		}
	}
	*/
	def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
		val p = new java.io.PrintWriter(f)
		try { op(p) } finally { p.close() }
	}

	def findFile(
		filename: String,
		searchPath_l: List[File]
	): ResultC[File] = {
		val file0 = new File(filename)
		if (file0.exists)
			return ResultC.unit(file0)
		
		for (dir <- searchPath_l) {
			val file = new File(dir, filename)
			if (file.exists())
				return ResultC.unit(file)
		}
		
		ResultC.error(s"Could not find file: $filename")
	}
	
	def contentEquals(file1: java.io.File, file2: java.io.File): Boolean = {
		org.apache.commons.io.FileUtils.contentEquals(file1, file2)
	}
}