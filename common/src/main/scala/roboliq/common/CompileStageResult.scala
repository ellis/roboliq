package roboliq.common

import scala.collection.mutable.ArrayBuffer


case class LogItem(val obj_? : Option[Object], val lsError: Seq[String]) {
	def this(sError: String) = this(None, Seq(sError))
	def this(lsError: Seq[String]) = this(None, lsError)
	def this(obj: Object, sError: String) = this(Some(obj), Seq(sError))
	def this(obj: Object, lsError: Seq[String]) = this(Some(obj), lsError)
	
	def getString: String = {
		obj_? match {
			case Some(cmd: Command) => cmd.toDebugString+":\n"+lsError.mkString("\n")
			case Some(obj) => obj+":\n"+lsError.mkString("\n")
			case None => lsError.mkString("\n")
		}
	}
}

class LogItemBuffer(val obj_? : Option[Object]) extends ArrayBuffer[LogItem] {
	def +=(s: String) = append(new LogItem(obj_?, s))
}

class LogBuilder(val obj_? : Option[Object] = None) {
	val errors = new LogItemBuffer(obj_?)
	val warnings = new LogItemBuffer(obj_?)
	val logs = new LogItemBuffer(obj_?)
	
	def toImmutable() = new Log(errors.toSeq, warnings.toSeq, logs.toSeq)
}

class Log(val errors: Seq[LogItem], val warnings: Seq[LogItem], val logs: Seq[LogItem]) {
	def print() {
		if (!errors.isEmpty) {
			println("Errors:")
			errors.foreach(item => println(item.getString))
			println()
		}
		if (!warnings.isEmpty) {
			println("Warnings:")
			warnings.foreach(item => println(item.getString))
			println()
		}
		if (!logs.isEmpty) {
			println("Logs:")
			logs.foreach(item => println(item.getString))
			println()
		}
	}
}

object Log {
	val empty = new Log(Seq(), Seq(), Seq())
	//def apply() = empty
	def apply(sError: String): Log = new Log(Seq(new LogItem(sError)), Seq(), Seq())
	def apply(lsError: Seq[String]): Log = new Log(Seq(new LogItem(lsError)), Seq(), Seq())
}


trait CompileStageResult {
	//val parent_? : Option[CompileStageResult]
	val log: Log
	def print()
}

case class CompileStageError(log: Log) extends CompileStageResult {
	def this(sError: String) = this(Log(sError))
	def this(lsError: List[String]) = this(Log(lsError))
	
	def print() {
		log.print()
	}
}

case class KnowledgeStageSuccess(mapper: ObjMapper, states0: RobotState, log: Log = Log.empty) extends CompileStageResult {
	def print() {}
}
