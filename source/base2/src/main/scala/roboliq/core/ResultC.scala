package roboliq.core

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

case class ResultCData(
	context_r: List[String] = Nil,
	warning_r: List[String] = Nil,
	error_r: List[String] = Nil
) {
	def setErrorsAndWarnings(error_r: List[String], warning_r: List[String]): ResultCData = {
		copy(error_r = error_r, warning_r = warning_r)
	}
	
	def logWarning(s: String): ResultCData = {
		copy(warning_r = prefixMessage(s) :: warning_r)
	}
	
	def logError(s: String): ResultCData = {
		//println(s"logError($s): "+(prefixMessage(s) :: error_r))
		copy(error_r = prefixMessage(s) :: error_r)
	}
	
	def log[A](res: RsResult[A]): ResultCData = {
		//println(s"log($res)")
		res match {
			case RsSuccess(_, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r)
			case RsError(error_l, warning_r) => copy(warning_r = warning_r.map(prefixMessage) ++ this.warning_r, error_r = error_l.reverse.map(prefixMessage) ++ this.error_r)
		}
	}
	
	def pushLabel(label: String): ResultCData = {
		//println(s"pushLabel($label) = ${label :: context_r}")
		copy(context_r = label :: context_r)
	}
	
	def popLabel(): ResultCData = {
		//println(s"popLabel()")
		copy(context_r = context_r.tail)
	}

	private def prefixMessage(s: String): String = {
		(s :: context_r).reverse.mkString(": ")
	}
}

trait ResultC[+A] {
	def run(data: ResultCData = ResultCData()): (ResultCData, Option[A])
	
	def map[B](f: A => B): ResultC[B] = {
		ResultC { data =>
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
	
	def flatMap[B](f: A => ResultC[B]): ResultC[B] = {
		ResultC { data =>
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

object ResultC {
	def apply[A](f: ResultCData => (ResultCData, Option[A])): ResultC[A] = {
		new ResultC[A] {
			def run(data: ResultCData) = f(data)
		}
	}
	
	def from[A](res: RsResult[A]): ResultC[A] = {
		ResultC { data =>
			val data1 = data.log(res)
			(data1, res.toOption)
		}
	}
	
	def from[A](opt: Option[A], error: => String): ResultC[A] = {
		ResultC { data =>
			val data1 = opt match {
				case None => data.logError(error)
				case _ => data
			}
			(data1, opt)
		}
	}
	
	def unit[A](a: A): ResultC[A] =
		ResultC { data => (data, Some(a)) }
	
	def putData(data: ResultCData): ResultC[Unit] =
		ResultC { _ => (data, Some(())) }
	
	def modifyData(f: ResultCData => ResultCData): ResultC[Unit] =
		ResultC { data => (f(data), Some(())) }
	
	def assert(condition: Boolean, msg: => String): ResultC[Unit] = {
		if (condition) unit(())
		else error(msg)
	}
	
	def error[A](s: String): ResultC[A] = {
		ResultC { data => (data.logError(s), None) }
	}
	
	def warning(s: String): ResultC[Unit] = {
		ResultC { data => (data.logWarning(s), None) }
	}
	
	/**
	 * Issues a warning if the condition is not met
	 */
	def check(condition: Boolean, s: => String): ResultC[Unit] = {
		if (condition) unit(())
		else warning(s)
	}
	
	//def getWellInfo(well: Well): ResultC[WellInfo] =
	//	getsResult[WellInfo](data => data.eb.wellToWellInfo(data.state, well))
	
	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	private def mapSub[A, B, C[_]](
		l: C[A],
		sequenceState: Boolean = false
	)(
		fn: A => ResultC[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultC[C[B]] = {
		ResultC { data0 =>
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
							else data0
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
		fn: A => ResultC[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultC[C[B]] = {
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
		fn: A => ResultC[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultC[C[B]] = {
		mapSub(l, true)(fn)
	}

	/**
	 * Map a function fn over the collection l.  Return either the all errors produced by fn, or a list of successes with accumulated warnings.
	 */
	def mapAll[A, B, C[_]](
		l: C[A]
	)(
		fn: A => ResultC[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): ResultC[C[B]] = {
		ResultC { data0 =>
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
		fn: A => ResultC[Any]
	)(implicit
		c2i: C[A] => Iterable[A]
	): ResultC[Unit] = {
		ResultC { data0 =>
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

	def or[B](f1: => ResultC[B], f2: => ResultC[B]): ResultC[B] = {
		ResultC { data =>
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
	
	def orElse[B](f1: => ResultC[B], f2: => ResultC[B]): ResultC[B] = {
		ResultC { data =>
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
	
	def toOption[B](ctx: ResultC[B]): ResultC[Option[B]] = {
		ResultC { data =>
			val (data1, opt1) = ctx.run(data)
			if (data1.error_r.isEmpty) {
				(data1, Some(opt1))
			}
			else {
				(data, None)
			}
		}
	}
	
	def context[B](label: String)(ctx: ResultC[B]): ResultC[B] = {
		ResultC { data =>
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
