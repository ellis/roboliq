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

case class ContextXData[State](
	state: State,
	context_r: List[String] = Nil,
	warning_r: List[String] = Nil,
	error_r: List[String] = Nil
) {
	def setState(state: State): ContextXData[State] = {
		copy(state = state)
	}
	
	def setErrorsAndWarnings(error_r: List[String], warning_r: List[String]): ContextXData[State] = {
		copy(error_r = error_r, warning_r = warning_r)
	}
	
	def logWarning(s: String): ContextXData[State] = {
		copy(warning_r = prefixMessage(s) :: warning_r)
	}
	
	def logError(s: String): ContextXData[State] = {
		//println(s"logError($s): "+(prefixMessage(s) :: error_r))
		copy(error_r = prefixMessage(s) :: error_r)
	}
	
	def log[A](res: RsResult[A]): ContextXData[State] = {
		//println(s"log($res)")
		res match {
			case RsSuccess(_, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r)
			case RsError(error_l, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r, error_r = error_l.reverse.map(prefixMessage) ++ this.error_r)
		}
	}
	
	def pushLabel(label: String): ContextXData[State] = {
		//println(s"pushLabel($label) = ${label :: context_r}")
		copy(context_r = label :: context_r)
	}
	
	def popLabel(): ContextXData[State] = {
		//println(s"popLabel()")
		copy(context_r = context_r.tail)
	}

	def prefixMessage(s: String): String = {
		(s :: context_r).reverse.mkString(": ")
	}
}

/*
sealed trait ContextX[+A] {
	def run(data: ContextXData[State]): (ContextXData[State], A)
	
	def map[B](f: A => B): ContextX[State, B] = {
		ContextX { data =>
			val (data1, a) = run(data)
			(data1, f(a))
		}
	}
	
	def flatMap[B](f: A => ContextX[State, B]): ContextX[State, B] = {
		ContextX { data =>
			val (data1, a) = run(data)
			f(a).run(data1)
		}
	}
}
*/
trait ContextX[State, +A] {
	def run(data: ContextXData[State]): (ContextXData[State], Option[A])
	
	def map[B](f: A => B): ContextX[State, B] = {
		ContextX { data =>
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
	
	def flatMap[B](f: A => ContextX[State, B]): ContextX[State, B] = {
		ContextX { data =>
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
	
	/*private def pair[A](data: ContextXData[State], res: RsResult[A]): (ContextXData[State], RsResult[A]) = {
		(data.log(res), res)
	}*/
}

class ContextXFunctions {
	def apply[State, A](f: ContextXData[State] => (ContextXData[State], Option[A])): ContextX[State, A] = {
		new ContextX[State, A] {
			def run(data: ContextXData[State]) = f(data)
		}
	}
	
	def from[State, A](res: RsResult[A]): ContextX[State, A] = {
		ContextX { data =>
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def from[State, A](opt: Option[A], error: => String): ContextX[State, A] = {
		ContextX { data =>
			val data1 = opt match {
				case None => data.logError(error)
				case _ => data
			}
			(data1, opt)
		}
	}
	
	def unit[State, A](a: A): ContextX[State, A] =
		ContextX { data => (data, Some(a)) }
	
	def get[State]: ContextX[State, ContextXData[State]] =
		ContextX { data => (data, Some(data)) }
	
	def gets[State, A](f: ContextXData[State] => A): ContextX[State, A] =
		ContextX { data => (data, Some(f(data))) }
	
	def getsResult[State, A](f: ContextXData[State] => RsResult[A]): ContextX[State, A] = {
		ContextX { data =>
			val res = f(data)
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def getsOption[State, A](f: ContextXData[State] => Option[A], error: => String): ContextX[State, A] = {
		ContextX { data =>
			f(data) match {
				case None => (data.logError(error), None)
				case Some(a) => (data, Some(a))
			}
		}
	}
	
	def put[State](data: ContextXData[State]): ContextX[State, Unit] =
		ContextX { _ => (data, Some(())) }
	
	def modify[State](f: ContextXData[State] => ContextXData[State]): ContextX[State, Unit] =
		ContextX { data => (f(data), Some(())) }
	
	def assert[State](condition: Boolean, msg: => String): ContextX[State, Unit] = {
		if (condition) unit(())
		else error(msg)
	}
	
	def error[State, A](s: String): ContextX[State, A] = {
		ContextX { data => (data.logError(s), None) }
	}
	
	//def getWellInfo(well: Well): ContextX[WellInfo] =
	//	getsResult[WellInfo](data => data.eb.wellToWellInfo(data.state, well))
	
	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	def map[State, A, B, C[_]](
		l: C[A]
	)(
		fn: A => ContextX[State, B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ContextX[State, C[B]] = {
		ContextX { data0 =>
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
	def mapAll[State, A, B, C[_]](
		l: C[A]
	)(
		fn: A => ContextX[State, B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ContextX[State, C[B]] = {
		ContextX { data0 =>
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
	def foreach[State, A, C[_]](
		l: C[A]
	)(
		fn: A => ContextX[State, Any]
	)(implicit
		c2i: C[A] => Iterable[A]
	): ContextX[State, Unit] = {
		ContextX { data0 =>
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
	
	def or[State, B](f1: => ContextX[State, B], f2: => ContextX[State, B]): ContextX[State, B] = {
		ContextX { data =>
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
	
	def orElse[State, B](f1: => ContextX[State, B], f2: => ContextX[State, B]): ContextX[State, B] = {
		ContextX { data =>
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
	
	def toOption[State, B](ctx: ContextX[State, B]): ContextX[State, Option[B]] = {
		ContextX { data =>
			val (data1, opt1) = ctx.run(data)
			if (data1.error_r.isEmpty) {
				(data1, Some(opt1))
			}
			else {
				(data, None)
			}
		}
	}
	
	def context[State, B](label: String)(ctx: ContextX[State, B]): ContextX[State, B] = {
		ContextX { data =>
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
}

object ContextX extends ContextXFunctions
