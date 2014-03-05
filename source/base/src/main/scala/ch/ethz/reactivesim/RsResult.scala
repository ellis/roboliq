/*
Copyright 2013 Ellis Whitehead

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

import scala.language.implicitConversions

import scalaz.Monad


sealed trait RsResult[+A] {
	val warning_r: List[String]
	def map[B](f: A => B): RsResult[B]
	def flatMap[B](f: A => RsResult[B]): RsResult[B]
	def foreach(f: A => Unit): Unit
	//def append[B](that: RsResult[B]): RsResult[B] = this.flatMap(_ => that)
	def getWarnings: List[String] = warning_r.reverse
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

trait RsPimper {
	implicit def pimpedOption[A](opt: Option[A]) = new RsOptionW(opt)
	
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
