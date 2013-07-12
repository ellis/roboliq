package roboliq.entities

case class Rel(name: String, args: List[String]) {
	override def toString(): String = s"($name " + args.mkString("", " ", ")")
}
