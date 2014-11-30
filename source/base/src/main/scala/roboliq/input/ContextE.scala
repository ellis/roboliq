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
import spray.json.JsValue
import scala.reflect.runtime.universe.TypeTag
import roboliq.entities.WellInfo
import roboliq.entities.Agent
import roboliq.entities.WorldStateEvent
import roboliq.entities.Amount
import java.io.File
import spray.json.JsObject

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

	def prefixMessage(s: String): String = {
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
	
	def get: ContextE[ContextEData] =
		ContextE { data => (data, Some(data)) }
	
	def gets[A](f: ContextEData => A): ContextE[A] =
		ContextE { data => (data, Some(f(data))) }
	
	def getsResult[A](f: ContextEData => RsResult[A]): ContextE[A] = {
		ContextE { data =>
			val res = f(data)
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def getsOption[A](f: ContextEData => Option[A], error: => String): ContextE[A] = {
		ContextE { data =>
			f(data) match {
				case None => (data.logError(error), None)
				case Some(a) => (data, Some(a))
			}
		}
	}
	
	def put(data: ContextEData): ContextE[Unit] =
		ContextE { _ => (data, Some(())) }
	
	def modify(f: ContextEData => ContextEData): ContextE[Unit] =
		ContextE { data => (f(data), Some(())) }
	
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
	def map[A, B, C[_]](
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
					if (data.error_r.isEmpty) {
						val ctx1 = fn(x)
						val (data1, opt) = ctx1.run(data)
						if (data1.error_r.isEmpty && opt.isDefined) {
							builder += opt.get
						}
						data = data1
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
						val (data1, opt) = ctx1.run(data)
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
	
	def getScope: ContextE[Map[String, JsObject]] = {
		ContextE { data =>
			(data, Some(data.state.scope))
		}
	}
	
	def addToScope(scope: Map[String, JsObject]): ContextE[Unit] = {
		ContextE { data =>
			val scope0 = data.state.scope
			val scope1 = scope0 ++ scope
			val state1 = data.state.copy(scope = scope1)
			val data1 = data.copy(state = state1)
			(data1, Some(()))
		}
	}
	
	def fromJson[A: TypeTag](jsval: JsValue): ContextE[A] = {
		Converter2.fromJson[A](jsval)
	}
	
	def fromScope[A: TypeTag](): ContextE[A] = {
		for {
			scope <- ContextE.getScope
			x <- Converter2.fromJson[A](JsObject(scope))
		} yield x
	}
	
	def evaluate(jsobj: JsObject): ContextE[JsObject] = {
		val evaluator = new Evaluator()
		evaluator.evaluate(jsobj)
	}
}
