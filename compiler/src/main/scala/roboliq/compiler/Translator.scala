package roboliq.compiler

import roboliq.common._


case class TranslatorStageSuccess(cmds: Seq[Command], log: Log = Log.empty) extends CompileStageResult {
	def print() {
		cmds.foreach(println)		
	}
}


trait Translator {
	def addKnowledge(kb: KnowledgeBase)

	def translate(cmd: CommandL1): Either[Seq[String], Seq[Command]]

	def translate(cmds: Seq[CommandL1]): Either[CompileStageError, TranslatorStageSuccess] = {
	//def translate(cmds: Seq[CommandL1]): Either[Seq[String], Seq[Command]] = {
		val cmds0 = cmds.flatMap(cmd => {
			translate(cmd) match {
				case Left(lsError) => return Left(CompileStageError(Log(lsError)))
				case Right(cmds0) => cmds0
			}
		})
		Right(TranslatorStageSuccess(cmds0))
	}

	def translateAndSave(cmds: Seq[CommandL1], sFilename: String): String
}
