package roboliq.input

import java.io.File

class EvaluatorScope(
	val map: RjsMap = RjsMap(),
	val parent_? : Option[EvaluatorScope] = None
) {
	/** First try lookup in own map, then in parent's map */
	def get(name: String): Option[RjsValue] = {
		map.get(name).orElse(parent_?.flatMap(_.get(name)))
	}
	
	/**
	 * First try lookup in own map, then in parent's map.
	 * Return both the object and the scope in which it was found.
	 * This is required for lambdas, which should be called with their containing scope.
	 */
	def getWithScope(name: String): Option[(RjsValue, EvaluatorScope)] = {
		map.get(name) match {
			case None => parent_?.flatMap(_.getWithScope(name))
			case Some(jsval) => Some((jsval, this))
		}
	}
	
	def add(name: String, jsobj: RjsValue): EvaluatorScope = {
		new EvaluatorScope(map = map.add(name, jsobj), parent_?)
	}
	
	def add(vars: Map[String, RjsValue]): EvaluatorScope = {
		new EvaluatorScope(map = this.map.add(vars), parent_?)
	}
	
	def add(map: RjsMap): EvaluatorScope = {
		new EvaluatorScope(map = this.map.add(map), parent_?)
	}
	
	def createChild(vars: RjsMap = RjsMap()): EvaluatorScope =
		new EvaluatorScope(map.add(vars), Some(this))
	
	def toMap: Map[String, RjsValue] = {
		parent_?.map(_.toMap).getOrElse(Map()) ++ map.map
	}
}

case class EvaluatorState(
	scope: EvaluatorScope = new EvaluatorScope(),
	searchPath_l: List[File] = Nil,
	inputFile_l: List[File] = Nil
) {
	def pushScope(vars: RjsMap = RjsMap()): EvaluatorState = {
		copy(scope = scope.createChild(vars))
	}
	
	def popScope(): EvaluatorState = {
		copy(scope = scope.parent_?.get)
	}
	
	def addToScope(name: String, value: RjsValue): EvaluatorState = {
		copy(scope = scope.add(name, value))
	}
	
	def addToScope(map: Map[String, RjsValue]): EvaluatorState = {
		copy(scope = scope.add(map))
	}
}
