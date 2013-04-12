package roboliq.compiler

import roboliq.common._


case class TranslatorStageSuccess(internal: Object, cmds: Seq[Command], log: Log = Log.empty) extends CompileStageResult {
	def print() {
		cmds.foreach(println)
	}
}

trait TranslationBuilder


trait Translator {
	def addKnowledge(kb: KnowledgeBase)

	def translate(cmds: Seq[CommandL1]): Either[CompileStageError, TranslatorStageSuccess]
}
