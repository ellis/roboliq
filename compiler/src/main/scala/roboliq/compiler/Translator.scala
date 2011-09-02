package roboliq.compiler

import roboliq.common._


trait Translator {
	def addKnowledge(kb: KnowledgeBase)

	def translate(cmd: CommandL1): Either[Seq[String], Seq[Command]]

	def translate(cmds: Seq[CommandL1]): Either[Seq[String], Seq[Command]] = {
		Right(cmds.flatMap(cmd => {
			translate(cmd) match {
				case Left(err) => return Left(err)
				case Right(cmds0) => cmds0
			}
		}))
	}

	def translateAndSave(cmds: Seq[CommandL1], sFilename: String): String
}
