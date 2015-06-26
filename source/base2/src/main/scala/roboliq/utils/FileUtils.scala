package roboliq.utils

import java.io.File
import roboliq.core.RsResult
import roboliq.core.RsSuccess
import roboliq.core.RsError

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
	): RsResult[File] = {
		val file0 = new File(filename)
		if (file0.exists)
			return RsSuccess(file0)
		
		for (dir <- searchPath_l) {
			val file = new File(dir, filename)
			if (file.exists())
				return RsSuccess(file)
		}
		
		RsError(s"Could not find file: $filename")
	}
}