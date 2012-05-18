package roboliq.core

import scala.reflect.BeanProperty

class CmdNodeBean {
	var handler: CmdHandler = null
	var childCommands: java.util.List[CmdBean] = null
	var tokens: List[CmdToken] = null
	var states0: RobotState = null
	var states1: RobotState = null
	var lIndex: List[Int] = null

	@BeanProperty var index: String = null
	@BeanProperty var command: CmdBean = null
	/** Single line of plain text to document this command */
	@BeanProperty var doc: String = null
	/** Arbitrary length documentation in MarkDown format */
	@BeanProperty var docMarkDown: String = null
	@BeanProperty var errors: java.util.List[String] = null
	@BeanProperty var warnings: java.util.List[String] = null
	@BeanProperty var children: java.util.List[CmdNodeBean] = null
	@BeanProperty var events: java.util.List[EventBean] = null

	def getErrorCount: Int =
		if (errors == null) 0 else errors.size()

	def addError(s: String) {
		if (errors == null)
			errors = new java.util.ArrayList[String]()
		errors.add(s)
	}
	
	def addError(property: String, s: String) {
		addError("property `"+property+"`: "+s)
	}
	
	def addError(id: String, property: String, s: String) {
		addError("object `"+id+"` property `"+property+"`: "+s)
	}

	/**
	 * Make sure that the given value is non-null
	 * @param o object to check
	 * @param property property name
	 * @returns true if non-null, otherwise false
	 */
	def checkValueNonNull(o: Object, property: String): Boolean = {
		if (o == null) {
			addError(property, "must be assigned a value")
			false
		}
		else
			true
	}
	
	/**
	 * Make sure that the given value is non-null
	 * @param o object to check
	 * @param property property name
	 * @returns Some(true) if non-null, otherwise None
	 */
	def checkValueNonNull_?(o: Object, property: String): Option[Boolean] = {
		checkValueNonNull(o, property) match {
			case true => Some(true)
			case _ => None
		}
	}
	
	/**
	 * Make sure that all named properties are non-null
	 * @param bean bean object to check
	 * @param properties names of properties
	 * @returns true if all are non-null, otherwise false
	 */
	def checkPropertyNonNull(bean: Object, properties: String*): Boolean = {
		val nErrors0 = getErrorCount
		val clazz = bean.getClass()
		for (sProperty <- properties) {
			val sMethod = "get" + sProperty.take(1).toUpperCase() + sProperty.drop(1)
			val l = clazz.getDeclaredMethods().filter(_.getName == sMethod)
			if (l.isEmpty)
				addError(sProperty, "INTERNAL: property's 'get' method does not exist")
			else {
				val method = l(0)
				val ret = method.invoke(bean)
				checkValueNonNull(ret, sProperty)
			}
		}
		(nErrors0 == getErrorCount)
	}
	
	/**
	 * Make sure that all named properties are non-null
	 * @param bean bean object to check
	 * @param properties names of properties
	 * @returns Some(true) if all are non-null, otherwise None
	 */
	def checkPropertyNonNull_?(bean: Object, properties: String*): Option[Boolean] = {
		checkPropertyNonNull(bean, properties : _*) match {
			case true => Some(true)
			case _ => None
		}
	}
	
	def mustBeNonEmpty(bean: Object, properties: String*): Boolean = {
		val nErrors0 = getErrorCount
		val clazz = bean.getClass()
		for (sProperty <- properties) {
			val sMethod = "get" + sProperty.take(1).toUpperCase() + sProperty.drop(1)
			val l = clazz.getDeclaredMethods().filter(_.getName == sMethod)
			if (l.isEmpty)
				addError(sProperty, "INTERNAL: property's 'get' method does not exist")
			else {
				val method = l(0)
				val ret = method.invoke(bean)
				if (ret == null)
					addError(sProperty, "must be assigned a list")
				else {
					try {
						val lRet = ret.asInstanceOf[java.util.List[_]]
						if (lRet.isEmpty)
							addError(sProperty, "list must contain at least one element")
					} catch {
						case ex: Exception => 
							addError(sProperty, "must be assigned a list")
					}
				}
			}
		}
		(nErrors0 == getErrorCount)
	}
	
	def getValueNonNull_?[A <: Object](o: A, property: String): Option[A] = {
		checkValueNonNull_?(o, property).map(b => o)
	}
}
