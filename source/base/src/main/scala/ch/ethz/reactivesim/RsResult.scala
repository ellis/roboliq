/*
Copyright 2013,2014 Ellis Whitehead

This file is part of reactive-sim.

reactive-sim is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

reactive-sim is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with reactive-sim.  If not, see <http://www.gnu.org/licenses/>
*/

package ch.ethz.reactivesim

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scala.language.implicitConversions
import scalaz.Monad
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer


sealed trait RsResult[+A] {
	val warning_r: List[String]
	def map[B](f: A => B): RsResult[B]
	def flatMap[B](f: A => RsResult[B]): RsResult[B]
	def foreach(f: A => Unit): Unit
	//def append[B](that: RsResult[B]): RsResult[B] = this.flatMap(_ => that)
	def getWarnings: List[String] = warning_r.reverse
	/** Drop the warnings from a success (but not from an error) */
	def dropWarnings: RsResult[A]
	def getErrors: List[String]
	
	def isError: Boolean
	final def isSuccess: Boolean = !isError
	private def isEmpty = isError
	

	/** Returns the option's value if the option is nonempty, otherwise
	  * return the result of evaluating `default`.
	  *
	  * @param default  the default expression.
	  */
	def getOrElse[B >: A](default: => B): B
	
	/** Returns this $option if it is nonempty,
	  * otherwise return the result of evaluating `alternative`.
	  * @param alternative the alternative expression.
	  */
	@inline final def orElse[B >: A](alternative: => RsResult[B]): RsResult[B] =
		if (isEmpty) alternative else this
	
	def flatten[B](implicit ev: A <:< RsResult[B]): RsResult[B]
	
	def toOption: Option[A]
}

object RsResult {
	def apply[A](opt: Option[A], error: => String): RsResult[A] = opt match {
		case Some(x) => RsSuccess(x)
		case None => RsError(error)
	}
	
	def apply[A](a: A, error: => String): RsResult[A] = {
		if (a == null) RsError(error)
		else RsSuccess(a)
	}
	
	def zero: RsResult[Unit] = RsSuccess[Unit](())
	def unit[A](a: A): RsResult[A] = RsSuccess[A](a)
	
	def asInstanceOf[A : Manifest](that: AnyRef): RsResult[A] =
		that match {
			case a : A => RsSuccess(a)
			case _ => RsError(s"expected instance of ${manifest[A].erasure.getName()}: $that")
		}
	
	def assert(b: Boolean, error: => String): RsResult[Unit] =
		if (b) zero else RsError(error)
	
	def toResultOfList[B](l: List[RsResult[B]]): RsResult[List[B]] = {
		l.foldRight(unit(List[B]()))((r, acc) => {
			acc.flatMap(l => r.map(_ :: l))
		})
	}
	
	def toResultOfTuple[A, B](tuple: (RsResult[A], RsResult[B])): RsResult[(A, B)] = {
		tuple match {
			case (RsSuccess(a, wa), RsSuccess(b, wb)) => RsSuccess((a, b), wa ++ wb)
			case (a, b) => RsError(a.getErrors ++ b.getErrors, a.getWarnings ++ b.getWarnings)
		}
	}
	
	def from[A](opt: Option[A], error: => String): RsResult[A] = opt match {
		case Some(x) => RsSuccess(x)
		case None => RsError(error)
	}
	
	def fromOrElse[A](opt: Option[A], default: => A): RsResult[A] = opt match {
		case Some(x) => RsSuccess(x)
		case None => RsSuccess(default)
	}
	
	def fromWithWarning[A](opt: Option[A], default: => A, warning: => String): RsResult[A] = opt match {
		case Some(x) => RsSuccess(x)
		case None => RsSuccess(default, List(warning))
	}
	
	/**
	 * Flatten a sequence of RsResults by just keeping the success values
	 */
	def flatten[A, C[_]](
		l: C[RsResult[A]]
	)(implicit
		c2i: C[RsResult[A]] => Iterable[RsResult[A]],
		cbf: CanBuildFrom[C[RsResult[A]], A, C[A]]
	): C[A] = {
		val builder = cbf()
		for (x <- c2i(l)) {
			x match {
				case RsSuccess(a, _) => builder += a
				case _ =>
			} 
		}
		builder.result()
	}
	
	/**
	 * Transform a list of results to a result of the successful values, dropping any errors but accumulating warnings
	 */
	def sequenceDrop[A, C[_]](
		l: C[RsResult[A]]
	)(implicit
		c2i: C[RsResult[A]] => Iterable[RsResult[A]],
		cbf: CanBuildFrom[C[RsResult[A]], A, C[A]]
	): RsResult[C[A]] = {
		val w_l = new ArrayBuffer[String]
		val builder = cbf()
		for (x <- c2i(l)) {
			x match {
				case RsSuccess(a, w) =>
					w_l ++= w
					builder += a
				case _ =>
			}
		}
		RsSuccess(builder.result(), w_l.toList)
	}
	
	/**
	 * Transform a list of results to a result of the successful values while accumulating success warnings, but return first error if any
	 */
	def sequenceFirst[A, C[_]](
		l: C[RsResult[A]]
	)(implicit
		c2i: C[RsResult[A]] => Iterable[RsResult[A]],
		cbf: CanBuildFrom[C[RsResult[A]], A, C[A]]
	): RsResult[C[A]] = {
		val w_l = new ArrayBuffer[String]
		val builder = cbf()
		for (x <- c2i(l)) {
			x match {
				case RsSuccess(a, w) =>
					w_l ++= w
					builder += a
				case RsError(e, w) =>
					return RsError(e, w)
			}
		}
		RsSuccess(builder.result(), w_l.toList)
	}
	
	/**
	 * Transform a list of results to a result of the successful values while accumulating success warnings,
	 * but if there are any errors, return them all instead.
	 */
	def sequenceAll[A, C[_]](
		l: C[RsResult[A]]
	)(implicit
		c2i: C[RsResult[A]] => Iterable[RsResult[A]],
		cbf: CanBuildFrom[C[RsResult[A]], A, C[A]]
	): RsResult[C[A]] = {
		val w_l = new ArrayBuffer[String]
		val builder = cbf()
		for (x <- c2i(l)) {
			x match {
				case RsSuccess(a, w) =>
					w_l ++= w
					builder += a
				case RsError(e, w) =>
					return sequenceToError(l)
			}
		}
		RsSuccess(builder.result(), w_l.toList)
	}
	
	private def sequenceToError[A, C[_]](
		l: Iterable[RsResult[A]]
	): RsResult[C[A]] = {
		val e_l = new ArrayBuffer[String]
		val w_l = new ArrayBuffer[String]
		for (x <- l) {
			x match {
				case RsError(e, w) =>
					e_l ++= e
					w_l ++= w
				case _ =>
			}
		}
		RsError(e_l.toList, w_l.toList)
	}

	/**
	 * Map a function fn over the collection l.  Return either the first error produced by fn, or a list of successes with accumulated warnings.
	 */
	def mapFirst[A, B, C[_]](
		l: C[A]
	)(
		fn: A => RsResult[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): RsResult[C[B]] = {
		val w_l = new ArrayBuffer[String]
		val builder = cbf()
		for (x <- c2i(l)) {
			fn(x) match {
				case RsSuccess(a, w) =>
					w_l ++= w
					builder += a
				case RsError(e, w) =>
					return RsError(e, w)
			}
		}
		RsSuccess(builder.result(), w_l.toList)
	}
	
	/**
	 * Transform a list of results to a result of the successful values while accumulating success warnings,
	 * but if there are any errors, return them all instead.
	 */
	def mapAll[A, B, C[_]](
		l: C[A]
	)(
		fn: A => RsResult[B]
	)(implicit
		c2i: C[A] => Iterable[A],
		cbf: CanBuildFrom[C[A], B, C[B]]
	): RsResult[C[B]] = {
		val e_l = new ArrayBuffer[String]
		val ew_l = new ArrayBuffer[String]
		val w_l = new ArrayBuffer[String]
		val builder = cbf()
		for (a <- c2i(l)) {
			fn(a) match {
				case RsSuccess(b, w) =>
					w_l ++= w
					builder += b
				case RsError(e, w) =>
					e_l ++= e
					ew_l ++= w
			}
		}
		
		if (e_l.isEmpty && ew_l.isEmpty)
			RsSuccess(builder.result(), w_l.toList)
		else
			RsError(e_l.toList, ew_l.toList)
	}

	/**
	 * Fold over l and return either the successful result with accumulated warnings, or else the first error
	 */
	def fold[A, B, C[_]](
		zero: B,
		l: C[A]
	)(
		fn: (B, A) => RsResult[B]
	)(implicit
		c2i: C[A] => Iterable[A]
	): RsResult[B] = {
		var b = zero
		val w_l = new ArrayBuffer[String]
		for (a <- c2i(l)) {
			fn(b, a) match {
				case RsSuccess(b2, w) =>
					w_l ++= w
					b = b2
				case err =>
					return err
			}
		}
		RsSuccess(b, w_l.toList)
	}
}

sealed case class RsSuccess[+A](res: A, warning_r: List[String] = Nil) extends RsResult[A] {
	def map[B](f: A => B): RsResult[B] = RsSuccess(f(res), warning_r)
	def flatMap[B](f: A => RsResult[B]): RsResult[B] = {
		f(res) match {
			case RsSuccess(res2, warning_r2) => RsSuccess(res2, warning_r2 ++ warning_r)
			case RsError(error_l2, warning_r2) => RsError(error_l2, warning_r2 ++ warning_r)
		}
	} 
	def foreach(f: A => Unit): Unit = f(res)

	def dropWarnings: RsResult[A] = RsSuccess(res)

	def getErrors: List[String] = Nil

	def isError = false

	def getOrElse[B >: A](default: => B): B = res

	def flatten[B](implicit ev: A <:< RsResult[B]): RsResult[B] =
    	ev(res)
    
    def toOption: Option[A] = Some(res)
}

sealed case class RsError[+A](error_l: List[String], warning_r: List[String] = Nil) extends RsResult[A] {
	def map[B](f: A => B): RsResult[B] = RsError[B](error_l, warning_r)
	def flatMap[B](f: A => RsResult[B]): RsResult[B] = RsError[B](error_l, warning_r)
	def foreach(f: A => Unit): Unit = ()

	def dropWarnings: RsResult[A] = this

	def getErrors: List[String] = error_l

	def isError = true

	def getOrElse[B >: A](default: => B): B = default

	def flatten[B](implicit ev: A <:< RsResult[B]): RsResult[B] =
    	RsError(error_l, warning_r)

    def toOption: Option[A] = None
}

object RsError {
	def apply[A](error: String): RsError[A] = RsError(List(error))
}

class RsOptionW[A](opt: Option[A]) {
	def asRs(error: => String): RsResult[A] = opt match {
		case Some(x) => RsSuccess(x)
		case None => RsError(error)
	}
}

class RsEither1W[A](either: Either[String, A]) {
	def asRs: RsResult[A] = either match {
		case Right(x) => RsSuccess(x)
		case Left(msg) => RsError(msg)
	}
}

class RsEitherNW[A](either: Either[List[String], A]) {
	def asRs: RsResult[A] = either match {
		case Right(x) => RsSuccess(x)
		case Left(msg_l) => RsError(msg_l)
	}
}

trait RsPimper {
	implicit def pimpedOption[A](opt: Option[A]) = new RsOptionW(opt)
	implicit def pimpedEither1[A](either: Either[String, A]) = new RsEither1W(either)
	implicit def pimpedEitherN[A](either: Either[List[String], A]) = new RsEitherNW(either)
	
	implicit def resultMonad: Monad[RsResult] = new Monad[RsResult] {
		def bind[A, B](fa: RsResult[A])(f: (A) => RsResult[B]) = fa.flatMap(f)
		def point[A](a: => A) = new RsSuccess[A](a)
	}
	
	implicit def resultResultToResult[A](o: RsResult[RsResult[A]]): RsResult[A] =
		o.flatten
	
	implicit def tryRsResultToRsResult[A](o: scala.util.Try[RsResult[A]]): RsResult[A] = {
		o match {
			case scala.util.Success(x) => x
			case scala.util.Failure(x) => RsError(x.getMessage())
		}
	}
	
	implicit def tryToRsResult[A](o: scala.util.Try[A]): RsResult[A] = {
		o match {
			case scala.util.Success(x) => RsSuccess(x)
			case scala.util.Failure(x) => RsError(x.getMessage())
		}
	}
}
