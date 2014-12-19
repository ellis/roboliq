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
import roboliq.core.ResultC
import roboliq.core.ResultCData

case class ResultEData(
	state: EvaluatorState = EvaluatorState(),
	context_r: List[String] = Nil,
	warning_r: List[String] = Nil,
	error_r: List[String] = Nil
) {
	def setState(state: EvaluatorState): ResultEData = {
		copy(state = state)
	}
	
	def setErrorsAndWarnings(error_r: List[String], warning_r: List[String]): ResultEData = {
		copy(error_r = error_r, warning_r = warning_r)
	}
	
	def logWarning(s: String): ResultEData = {
		copy(warning_r = prefixMessage(s) :: warning_r)
	}
	
	def logError(s: String): ResultEData = {
		//println(s"logError($s): "+(prefixMessage(s) :: error_r))
		copy(error_r = prefixMessage(s) :: error_r)
	}
	
	def log[A](res: RsResult[A]): ResultEData = {
		//println(s"log($res)")
		res match {
			case RsSuccess(_, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r)
			case RsError(error_l, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r, error_r = error_l.reverse.map(prefixMessage) ++ this.error_r)
		}
	}
	
	def pushLabel(label: String): ResultEData = {
		//println(s"pushLabel($label) = ${label :: context_r}")
		copy(context_r = label :: context_r)
	}
	
	def popLabel(): ResultEData = {
		//println(s"popLabel()")
		copy(context_r = context_r.tail)
	}

	def modifyState(fn: EvaluatorState => EvaluatorState): ResultEData = {
		val state1 = fn(state)
		copy(state = state1)
	}

	def pushScope(vars: RjsMap = RjsMap()): ResultEData = {
		modifyState(_.pushScope(vars))
	}
	
	def popScope(): ResultEData = {
		modifyState(_.popScope)
	}
	
	private def prefixMessage(s: String): String = {
		(s :: context_r).reverse.mkString(": ")
	}
}

trait ResultE[+A] {
	def run(data: ResultEData = ResultEData()): (ResultEData, Option[A])
	
	def map[B](f: A => B): ResultE[B] = {
		ResultE { data =>
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
	
	def flatMap[B](f: A => ResultE[B]): ResultE[B] = {
		ResultE { data =>
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

object ResultE {
	def apply[A](f: ResultEData => (ResultEData, Option[A])): ResultE[A] = {
		new ResultE[A] {
			def run(data: ResultEData) = f(data)
		}
	}
	
	def from[A](res: RsResult[A]): ResultE[A] = {
		ResultE { data =>
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def from[A](opt: Option[A], error: => String): ResultE[A] = {
		ResultE { data =>
			val data1 = opt match {
				case None => data.logError(error)
				case _ => data
			}
			(data1, opt)
		}
	}
	
	def from[A](res: ResultC[A]): ResultE[A] = {
		ResultE { data =>
			if (data.error_r.isEmpty) {
				val dataC0 = ResultCData(data.context_r, data.warning_r, data.error_r)
				val (dataC1, ret_?) = res.run(dataC0)
				val data1 = ResultEData(data.state, dataC1.context_r, dataC1.warning_r, dataC1.error_r)
				(data1, ret_?)
			}
			else {
				(data, None)
			}
		}
	}
	
	def unit[A](a: A): ResultE[A] =
		ResultE { data => (data, Some(a)) }
	
	def get: ResultE[EvaluatorState] =
		ResultE { data => (data, Some(data.state)) }
	
	def gets[A](f: EvaluatorState => A): ResultE[A] =
		ResultE { data => (data, Some(f(data.state))) }
	
	def getsResult[A](f: EvaluatorState => RsResult[A]): ResultE[A] = {
		ResultE { data =>
			val res = f(data.state)
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def getsOption[A](f: EvaluatorState => Option[A], error: => String): ResultE[A] = {
		ResultE { data =>
			f(data.state) match {
				case None => (data.logError(error), None)
				case Some(a) => (data, Some(a))
			}
		}
	}
	
	def putData(data: ResultEData): ResultE[Unit] =
		ResultE { _ => (data, Some(())) }
	
	def modifyData(f: ResultEData => ResultEData): ResultE[Unit] =
		ResultE { data => (f(data), Some(())) }
	
	def put(state: EvaluatorState): ResultE[Unit] =
		ResultE { data => (data.copy(state = state), Some(())) }
	
	def modify(f: EvaluatorState => EvaluatorState): ResultE[Unit] =
		ResultE { data => (data.copy(state = f(data.state)), Some(())) }
	
	def assert(condition: Boolean, msg: => String): ResultE[Unit] = {
		if (condition) unit(())
		else error(msg)
	}
	
	def error[A](s: String): ResultE[A] = {
		ResultE { data => (data.logError(s), None) }
	}
	
	def warning[A](s: String): ResultE[A] = {
		ResultE { data => (data.logWarning(s), None) }
	}
	
	//def getWellInfo(well: Well): ResultE[WellInfo] =
	//	getsResult[WellInfo](data => data.eb.wellToWellInfo(data.state, well))
	
	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	private def mapSub[A, B, C[_]](
		l: C[A],
		sequenceState: Boolean = false
	)(
		fn: A => ResultE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultE[C[B]] = {
		ResultE { data0 =>
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
		fn: A => ResultE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultE[C[B]] = {
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
		fn: A => ResultE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultE[C[B]] = {
		mapSub(l, true)(fn)
	}

	/**
	 * Map a function fn over the collection l.  Return either the all errors produced by fn, or a list of successes with accumulated warnings.
	 */
	def mapAll[A, B, C[_]](
		l: C[A]
	)(
		fn: A => ResultE[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultE[C[B]] = {
		ResultE { data0 =>
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
		fn: A => ResultE[Any]
	)(implicit
		c2i: C[A] => Iterable[A]
	): ResultE[Unit] = {
		ResultE { data0 =>
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

	def or[B](f1: => ResultE[B], f2: => ResultE[B]): ResultE[B] = {
		ResultE { data =>
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
	
	def orElse[B](f1: => ResultE[B], f2: => ResultE[B]): ResultE[B] = {
		ResultE { data =>
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
	
	def toOption[B](ctx: ResultE[B]): ResultE[Option[B]] = {
		ResultE { data =>
			val (data1, opt1) = ctx.run(data)
			if (data1.error_r.isEmpty) {
				(data1, Some(opt1))
			}
			else {
				(data, None)
			}
		}
	}
	
	def context[B](label: String)(ctx: ResultE[B]): ResultE[B] = {
		ResultE { data =>
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
	
	def getScope: ResultE[EvaluatorScope] = {
		ResultE.gets(_.scope)
	}
	
	/**
	 * 
	 */
	def scope[B](ctx: ResultE[B]): ResultE[B] = {
		ResultE { data =>
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
	
	def addToScope(name: String, value: RjsValue): ResultE[Unit] = {
		ResultE.modify(_.addToScope(name, value))
	}
	
	def addToScope(map: Map[String, RjsValue]): ResultE[Unit] = {
		ResultE.modify(_.addToScope(map))
	}
	
	def addToScope(map: RjsMap): ResultE[Unit] = {
		ResultE.modify(_.addToScope(map.map))
	}
	
	def withScope[B](scope: EvaluatorScope)(ctx: ResultE[B]): ResultE[B] = {
		ResultE { data =>
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
	
	def getScopeValue(name: String): ResultE[RjsValue] = {
		for {
			scope <- ResultE.getScope
			rjsval <- ResultE.from(scope.get(name), s"unknown variable `$name`")
		} yield rjsval
	}
	
	def fromScope[A: TypeTag](): ResultE[A] = {
		for {
			scope <- ResultE.getScope
			x <- RjsConverter.fromRjs[A](RjsMap(scope.toMap))
		} yield x
	}

	def fromScope[A: TypeTag](name: String): ResultE[A] = {
		for {
			rjsval <- getScopeValue(name)
			x <- RjsConverter.fromRjs[A](rjsval)
		} yield x
	}
	
	def evaluate(rjsval: RjsValue): ResultE[RjsValue] = {
		val evaluator = new Evaluator()
		evaluator.evaluate(rjsval)
	}
	
	/*
	/**
	 * Evaluate jsval using a different scope
	 */
	def evaluate(jsval: RjsValue, scope: EvaluatorScope): ResultE[RjsMap] = {
		// Temporarily set the given scope, evaluate jsval, then switch back to the current scope
		for {
			scope0 <- ResultE.getScope
			_ <- ResultE.modify(_.copy(scope = scope))
			res <- evaluate(jsval)
			_ <- ResultE.modify(_.copy(scope = scope0))
		} yield res
	}*/
	
	def findFile(filename: String): ResultE[File] = {
		for {
			searchPath_l <- ResultE.gets(_.searchPath_l)
			file <- ResultE.from(roboliq.utils.FileUtils.findFile(filename, searchPath_l))
			_ <- ResultE.modify(state => state.copy(inputFile_l = file :: state.inputFile_l))
		} yield file
	}

	def loadJsonFromFile(file: File): ResultE[RjsBasicValue] = {
		import spray.json._
		for {
			_ <- ResultE.assert(file.exists, s"File not found: ${file.getPath}")
			_ <- ResultE.modify(state => state.copy(inputFile_l = file :: state.inputFile_l))
			bYaml <- FilenameUtils.getExtension(file.getPath).toLowerCase match {
				case "json" => ResultE.unit(false)
				case "yaml" => ResultE.unit(true)
				case ext => ResultE.error(s"Unrecognized file extension `$ext`.  Expected either json or yaml.")
			}
			input0 = org.apache.commons.io.FileUtils.readFileToString(file)
			input = if (bYaml) JsonUtils.yamlToJsonText(input0) else input0
			jsval = input.parseJson
			res <- from(RjsValue.fromJson(jsval))
		} yield res
	}
}
