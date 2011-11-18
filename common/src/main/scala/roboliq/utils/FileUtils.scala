package roboliq.utils

object FileUtils {
	/*
	def writeToFile(fileName:String, data:String) = 
		using (new FileWriter(fileName)) {
		fileWriter => fileWriter.write(data)
	}
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
}