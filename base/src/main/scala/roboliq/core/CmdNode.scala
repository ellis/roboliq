package roboliq.core

import scala.reflect.BeanProperty

/**
 * Data about a processed command.
 * 
 * Command nodes are produces when a [[roboliq.core.CmdHandler]] expands a [[roboliq.core.CmdBean]].
 * This class is set up for YAML output.
 */
class CmdNodeBean {
	/** The handler which created this node. */
	var handler: CmdHandler = null
	/** The commands which were created when `command` was expanded by `handler`. */
	var childCommands: java.util.List[CmdBean] = null
	/** The final tokens which were created when `command` was expanded by `handler`. */
	var tokens: List[CmdToken] = null
	/** Initial state when entering this `command`. */
	var states0: RobotState = null
	/** Final state after this `command` was finished. */
	var states1: RobotState = null
	/** List of integers representing the index of this node in the command node tree. */
	var lIndex: List[Int] = null

	/** Index of this node in the command node tree. */
	@BeanProperty var index: String = null
	/** The command of this node */
	@BeanProperty var command: CmdBean = null
	/** Single line of plain text to document this command */
	@BeanProperty var doc: String = null
	/** Arbitrary length documentation in MarkDown format */
	@BeanProperty var docMarkDown: String = null
	/** List of errors when processing this command */
	@BeanProperty var errors: java.util.List[String] = null
	/** List of warnings when processing this command */
	@BeanProperty var warnings: java.util.List[String] = null
	/** List of this node's child nodes. */
	@BeanProperty var children: java.util.List[CmdNodeBean] = null
	/** List of robot events which are expected to occur during this command. */
	@BeanProperty var events: java.util.List[EventBean] = null

	/** Number of errors while processing this `command`. */
	def getErrorCount: Int =
		if (errors == null) 0 else errors.size()

	/** Add an error to the `errors` list */
	def addError(s: String) {
		if (errors == null)
			errors = new java.util.ArrayList[String]()
		errors.add(s)
	}
	
	/** Add an error to the `errors` list */
	def addError(property: String, s: String) {
		addError("property `"+property+"`: "+s)
	}
	
	/** Add an error to the `errors` list */
	def addError(id: String, property: String, s: String) {
		addError("object `"+id+"` property `"+property+"`: "+s)
	}

	/**
	 * Make sure that the given value is non-null
	 * @param o object to check
	 * @param property property name
	 * @return true if non-null, otherwise false
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
	 * @return Some(true) if non-null, otherwise None
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
	 * @return true if all are non-null, otherwise false
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
	 * @return Some(true) if all are non-null, otherwise None
	 */
	def checkPropertyNonNull_?(bean: Object, properties: String*): Option[Boolean] = {
		checkPropertyNonNull(bean, properties : _*) match {
			case true => Some(true)
			case _ => None
		}
	}
	
	/**
	 * Check that all `properties` are non-empty on the given `bean`.
	 * Add error messages to `errors` for any missing property values.
	 * 
	 * @param bean JavaBean of interest.
	 * @param properties list of property names to check.
	 * @return true if the givens `properties` are all non-empty for the given `bean`. 
	 */
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
	
	/**
	 * Try to get the value of the property with name `property` from object `o`.
	 * On success, return Some value, otherwise None. 
	 */
	def getValueNonNull_?[A <: Object](o: A, property: String): Option[A] = {
		checkValueNonNull_?(o, property).map(b => o)
	}
}
