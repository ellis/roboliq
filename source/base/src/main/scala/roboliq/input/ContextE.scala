package roboliq.input

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsSuccess
import roboliq.entities.Aliquot
import roboliq.entities.Entity
import roboliq.entities.EntityBase
import roboliq.entities.Labware
import roboliq.entities.LabwareModel
import roboliq.entities.Well
import roboliq.entities.WorldState
import roboliq.entities.WorldStateBuilder
import scala.reflect.runtime.universe.TypeTag
import roboliq.entities.WellInfo
import roboliq.entities.Agent
import roboliq.entities.WorldStateEvent
import roboliq.entities.Amount
import java.io.File
import org.apache.commons.io.FilenameUtils
import com.google.gson.Gson
import spray.json.JsNull
import roboliq.utils.JsonUtils

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
	eb: EntityBase,
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

case class ContextEData(
	state: EvaluatorState,
	context_r: List[String] = Nil,
	warning_r: List[String] = Nil,
	error_r: List[String] = Nil
) {
	def setState(state: EvaluatorState): ContextEData = {
		copy(state = state)
	}
	
	def setErrorsAndWarnings(error_r: List[String], warning_r: List[String]): ContextEData = {
		copy(error_r = error_r, warning_r = warning_r)
	}
	
	def logWarning(s: String): ContextEData = {
		copy(warning_r = prefixMessage(s) :: warning_r)
	}
	
	def logError(s: String): ContextEData = {
		//println(s"logError($s): "+(prefixMessage(s) :: error_r))
		copy(error_r = prefixMessage(s) :: error_r)
	}
	
	def log[A](res: RsResult[A]): ContextEData = {
		//println(s"log($res)")
		res match {
			case RsSuccess(_, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r)
			case RsError(error_l, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r, error_r = error_l.reverse.map(prefixMessage) ++ this.error_r)
		}
	}
	
	def pushLabel(label: String): ContextEData = {
		//println(s"pushLabel($label) = ${label :: context_r}")
		copy(context_r = label :: context_r)
	}
	
	def popLabel(): ContextEData = {
		//println(s"popLabel()")
		copy(context_r = context_r.tail)
	}

	def modifyState(fn: EvaluatorState => EvaluatorState): ContextEData = {
		val state1 = fn(state)
		copy(state = state1)
	}

	def pushScope(vars: RjsMap = RjsMap()): ContextEData = {
		modifyState(_.pushScope(vars))
	}
	
	def popScope(): ContextEData = {
		modifyState(_.popScope)
	}
	
	private def prefixMessage(s: String): String = {
		(s :: context_r).reverse.mkString(": ")
	}
}

trait ContextE[+A] {
	def run(data: ContextEData): (ContextEData, Option[A])
	
	def map[B](f: A => B): ContextE[B] = {
		ContextE { data =>
			if (data.error_r.isEmpty) {
				val (data1, optA) = run(data)
				val optB = optA.map(f)
				(data1, optB)
			}
			else {
				(data, None)
			}
		}
	}
	
	def flatMap[B](f: A => ContextE[B]): ContextE[B] = {
		ContextE { data =>
			if (data.error_r.isEmpty) {
				val (data1, optA) = run(data)
				optA match {
					case None => (data1, None)
					case Some(a) => f(a).run(data1)
				}
			}
			else {
				(data, None)
			}
		}
	}
}

object ContextE {
	def apply[A](f: ContextEData => (ContextEData, Option[A])): ContextE[A] = {
		new ContextE[A] {
			def run(data: ContextEData) = f(data)
		}
	}
	
	def from[A](res: RsResult[A]): ContextE[A] = {
		ContextE { data =>
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def from[A](opt: Option[A], error: => String): ContextE[A] = {
		ContextE { data =>
			val data1 = opt match {
				case None => data.logError(error)
				case _ => data
			}
			(data1, opt)
		}
	}
	
	def unit[A](a: A): ContextE[A] =
		ContextE { data => (data, Some(a)) }
	
	def get: ContextE[EvaluatorState] =
		ContextE { data => (data, Some(data.state)) }
	
	def gets[A](f: EvaluatorState => A): ContextE[A] =
		ContextE { data => (data, Some(f(data.state))) }
	
	def getsResult[A](f: EvaluatorState => RsResult[A]): ContextE[A] = {
		ContextE { data =>
			val res = f(data.state)
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def getsOption[A](f: EvaluatorState => Option[A], error: => String): ContextE[A] = {
		ContextE { data =>
			f(data.state) match {
				case None => (data.logError(error), None)
				case Some(a) => (data, Some(a))
			}
		}
	}
	
	def putData(data: ContextEData): ContextE[Unit] =
		ContextE { _ => (data, Some(())) }
	
	def modifyData(f: ContextEData => ContextEData): ContextE[Unit] =
		ContextE { data => (f(data), Some(())) }
	
	def put(state: EvaluatorState): ContextE[Unit] =
		ContextE { data => (data.copy(state = state), Some(())) }
	
	def modify(f: EvaluatorState => EvaluatorState): ContextE[Unit] =
		ContextE { data => (data.copy(state = f(data.state)), Some(())) }
	
	def assert(condition: Boolean, msg: => String): ContextE[Unit] = {
		if (condition) unit(())
		else error(msg)
	}
	
	def error[A](s: String): ContextE[A] = {
		ContextE { data => (data.logError(s), None) }
	}
	
	//def getWellInfo(well: Well): ContextE[WellInfo] =
	//	getsResult[WellInfo](data => data.eb.wellToWellInfo(data.state, well))
	
	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	private def mapSub[A, B, C[_]](
		l: C[A],
		sequenceState: Boolean = false
	)(
		fn: A => ContextE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ContextE[C[B]] = {
		ContextE { data0 =>
			if (data0.error_r.isEmpty) {
				var data = data0
				val builder = cbf()
				for (x <- c2i(l)) {
					if (data.error_r.isEmpty) {
						val ctx1 = fn(x)
						val (data1, opt) = ctx1.run(data)
						if (data1.error_r.isEmpty && opt.isDefined) {
							builder += opt.get
						}
						data = {
							// If state should not be sequenced, copy the original state back into the new context data
							if (sequenceState) data1
							else data1.copy(state = data0.state)
						}
					}
				}
				if (data.error_r.isEmpty) (data, Some(builder.result()))
				else (data, None)
			}
			else {
				(data0, None)
			}
		}
	}
	
	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	def map[A, B, C[_]](
		l: C[A]
	)(
		fn: A => ContextE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ContextE[C[B]] = {
		mapSub(l, false)(fn)
	}
	
	/**
	 * Map a function fn over the collection l.
	 * The context gets updated after each item.
	 * @return Return either the first error produced by fn, or a list of successes with accumulated warnings.  The result is wrapped in the final context.
	 */
	def mapSequential[A, B, C[_]](
		l: C[A]
	)(
		fn: A => ContextE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ContextE[C[B]] = {
		mapSub(l, true)(fn)
	}

	/**
	 * Map a function fn over the collection l.  Return either the all errors produced by fn, or a list of successes with accumulated warnings.
	 */
	def mapAll[A, B, C[_]](
		l: C[A]
	)(
		fn: A => ContextE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ContextE[C[B]] = {
		ContextE { data0 =>
			if (data0.error_r.isEmpty) {
				var data = data0
				val builder = cbf()
				for (x <- c2i(l)) {
					val ctx1 = fn(x)
					val (data1, opt) = ctx1.run(data)
					if (data1.error_r.isEmpty && opt.isDefined) {
						builder += opt.get
					}
					data = data1
				}
				if (data.error_r.isEmpty) (data, Some(builder.result()))
				else (data, None)
			}
			else {
				(data0, None)
			}
		}
	}

	/**
	 * Run a function fn over the collection l.  Abort on the first error produced by fn.
	 * The context gets updated after each item.
	 * @return The final context
	 */
	def foreach[A, C[_]](
		l: C[A]
	)(
		fn: A => ContextE[Any]
	)(implicit
		c2i: C[A] => Iterable[A]
	): ContextE[Unit] = {
		ContextE { data0 =>
			if (data0.error_r.isEmpty) {
				var data = data0
				for (x <- c2i(l)) {
					if (data.error_r.isEmpty) {
						val ctx1 = fn(x)
						val (data1, _) = ctx1.run(data)
						data = data1
					}
				}
				if (data.error_r.isEmpty) (data, Some(()))
				else (data, None)
			}
			else {
				(data0, None)
			}
		}
	}

	def or[B](f1: => ContextE[B], f2: => ContextE[B]): ContextE[B] = {
		ContextE { data =>
			if (data.error_r.isEmpty) {
				val (data1, opt1) = f1.run(data)
				if (data1.error_r.isEmpty) {
					(data1, opt1)
				}
				else {
					val (data2, opt2) = f2.run(data)
					if (data2.error_r.isEmpty) {
						(data2, opt2)
					}
					else {
						// TODO: should probably prefix the different warnings to distinguish between the alternatives that were tried
						val error_r = data2.error_r ++ data1.error_r
						// TODO: this will duplicate all the warnings that were already in data -- avoid that
						val warning_r = data2.warning_r ++ data1.warning_r
						val data3 = data.setErrorsAndWarnings(error_r, warning_r)
						(data3, None)
					}
				}
			}
			else {
				(data, None)
			}
		}
	}
	
	def orElse[B](f1: => ContextE[B], f2: => ContextE[B]): ContextE[B] = {
		ContextE { data =>
			if (data.error_r.isEmpty) {
				val (data1, opt1) = f1.run(data)
				if (data1.error_r.isEmpty) {
					(data1, opt1)
				}
				else {
					val (data2, opt2) = f2.run(data)
					if (data2.error_r.isEmpty) {
						(data2, opt2)
					}
					else {
						(data2, None)
					}
				}
			}
			else {
				(data, None)
			}
		}
	}
	
	def toOption[B](ctx: ContextE[B]): ContextE[Option[B]] = {
		ContextE { data =>
			val (data1, opt1) = ctx.run(data)
			if (data1.error_r.isEmpty) {
				(data1, Some(opt1))
			}
			else {
				(data, None)
			}
		}
	}
	
	def context[B](label: String)(ctx: ContextE[B]): ContextE[B] = {
		ContextE { data =>
			val data0 = data.pushLabel(label)
			val (data1, opt1) = ctx.run(data0)
			val data2 = data1.popLabel()
			if (data2.error_r.isEmpty) {
				(data2, opt1)
			}
			else {
				(data2, None)
			}
		}
	}
	
	def getScope: ContextE[EvaluatorScope] = {
		ContextE.gets(_.scope)
	}
	
	/**
	 * 
	 */
	def scope[B](ctx: ContextE[B]): ContextE[B] = {
		ContextE { data =>
			val data0 = data.pushScope()
			val (data1, opt1) = ctx.run(data0)
			val data2 = data1.popScope()
			if (data2.error_r.isEmpty) {
				(data2, opt1)
			}
			else {
				(data2, None)
			}
		}
	}
	
	def addToScope(name: String, value: RjsValue): ContextE[Unit] = {
		ContextE.modify(_.addToScope(name, value))
	}
	
	def addToScope(map: Map[String, RjsValue]): ContextE[Unit] = {
		ContextE.modify(_.addToScope(map))
	}
	
	def addToScope(map: RjsMap): ContextE[Unit] = {
		ContextE.modify(_.addToScope(map.map))
	}
	
	def withScope[B](scope: EvaluatorScope)(ctx: ContextE[B]): ContextE[B] = {
		ContextE { data =>
			val state0 = data.state
			val scope0 = state0.scope
			// Run ctx using the given scope
			val data1 = data.copy(state = data.state.copy(scope = scope))
			val (data2, opt2) = ctx.run(data1)
			// Swap the original scope back in
			val data3 = data2.copy(state = state0)
			(data3, if (data3.error_r.isEmpty) opt2 else None)
		}
	}
	
	def fromRjs[A: TypeTag](rjsval: RjsValue): ContextE[A] = {
		Converter3.fromRjs[A](rjsval)
	}
	
	def fromRjs[A: TypeTag](map: RjsMap, field: String): ContextE[A] =
		fromRjs[A](map.map, field)

	def fromRjs[A: TypeTag](map: Map[String, RjsValue], field: String): ContextE[A] = {
		ContextE.context(field) {
			map.get(field) match {
				case Some(jsval) => Converter3.fromRjs[A](jsval)
				case None =>
					ContextE.orElse(
						Converter3.fromRjs[A](RjsNull),
						ContextE.error("value required")
					)
			}
		}
	}

	def getScopeValue(name: String): ContextE[RjsValue] = {
		for {
			scope <- ContextE.getScope
			rjsval <- ContextE.from(scope.get(name), s"unknown variable `$name`")
		} yield rjsval
	}
	
	def fromScope[A: TypeTag](): ContextE[A] = {
		for {
			scope <- ContextE.getScope
			x <- Converter3.fromRjs[A](RjsMap(scope.toMap))
		} yield x
	}

	def fromScope[A: TypeTag](name: String): ContextE[A] = {
		for {
			jsobj <- getScopeValue(name)
			x <- Converter3.fromRjs[A](jsobj)
		} yield x
	}
	
	def evaluate(rjsval: RjsValue): ContextE[RjsValue] = {
		val evaluator = new Evaluator()
		evaluator.evaluate(rjsval)
	}
	
	/*
	/**
	 * Evaluate jsval using a different scope
	 */
	def evaluate(jsval: RjsValue, scope: EvaluatorScope): ContextE[RjsMap] = {
		// Temporarily set the given scope, evaluate jsval, then switch back to the current scope
		for {
			scope0 <- ContextE.getScope
			_ <- ContextE.modify(_.copy(scope = scope))
			res <- evaluate(jsval)
			_ <- ContextE.modify(_.copy(scope = scope0))
		} yield res
	}*/
	
	def findFile(filename: String): ContextE[File] = {
		for {
			searchPath_l <- ContextE.gets(_.searchPath_l)
			file <- ContextE.from(roboliq.utils.FileUtils.findFile(filename, searchPath_l))
			_ <- ContextE.modify(state => state.copy(inputFile_l = file :: state.inputFile_l))
		} yield file
	}

	def loadJsonFromFile(file: File): ContextE[RjsValue] = {
		import spray.json._
		for {
			_ <- ContextE.assert(file.exists, s"File not found: ${file.getPath}")
			_ <- ContextE.modify(state => state.copy(inputFile_l = file :: state.inputFile_l))
			bYaml <- FilenameUtils.getExtension(file.getPath).toLowerCase match {
				case "json" => ContextE.unit(false)
				case "yaml" => ContextE.unit(true)
				case ext => ContextE.error(s"Unrecognized file extension `$ext`.  Expected either json or yaml.")
			}
			input0 = org.apache.commons.io.FileUtils.readFileToString(file)
			input = if (bYaml) JsonUtils.yamlToJsonText(input0) else input0
			jsval = input.parseJson
			res <- RjsValue.fromJson(jsval)
		} yield res
	}
}
