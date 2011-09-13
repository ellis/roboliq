package roboliq.compiler

import roboliq.common._


trait CompilerConfig {
	val devices: Seq[Device]
	val processors: Seq[CommandCompiler]
	
	def createTranslator: Translator
}