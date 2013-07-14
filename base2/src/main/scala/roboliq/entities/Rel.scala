package roboliq.entities

case class Rel(name: String, args: List[String], comment: String = null) {
	override def toString(): String = s"($name " + args.mkString("", " ", ")")
	def toStringWithComment(): String = s"($name ${args.mkString(" ")})" + (if (comment == null) "" else " ; "+comment)
}
