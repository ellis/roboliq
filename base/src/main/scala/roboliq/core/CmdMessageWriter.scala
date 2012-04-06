package roboliq.core

class CmdMessageWriter(node: CmdNodeBean) {
	def ok: Boolean = (node.getErrorCount == 0)
	def hasErrors: Boolean = {
		node.getErrorCount > 0
	}
	
	def addError(s: String) =
		node.addError(s)
	def addError(param: String, s: String) =
		node.addError(param, s)
	def addError(id: String, property: String, s: String) =
		node.addError(id, property, s)
	
	def paramMustBeNonNull(name: String): Boolean = {
		getPropertyValue(node.command, name) match {
			case None =>
				addError(name, "must be assigned a value")
				false
			case Some(value) =>
				true
		}
	}
	
	def paramMustBeNonNull(name: String, o: Object): Boolean = {
		if (o == null) {
			addError(name, "must be assigned a value")
			false
		}
		else {
				true
		}
	}
	
	def paramMustBeNonEmpty(name: String): Boolean = {
		getPropertyValue(node.command, name) match {
			case None =>
				addError(name, "must be assigned a value")
				false
			case Some(value) =>
				val l = value.asInstanceOf[java.util.List[_]]
				if (l.isEmpty) {
					addError(name, "list must contain one or more elements")
					false
				}
				else {
					true
				}
		}
	}
	
	private def getPropertyValue(o: Object, sProperty: String): Option[Object] = {
		val clazz = node.command.getClass()
		val sMethod = "get" + sProperty.take(1).toUpperCase() + sProperty.drop(1)
		val l = clazz.getDeclaredMethods().filter(_.getName == sMethod)
		if (l.isEmpty) {
			None
		}
		else {
			val method = l(0)
			val ret = method.invoke(o)
			Some(ret)
		}
	}
}