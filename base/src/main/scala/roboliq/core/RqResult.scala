package roboliq.core

import scala.language.implicitConversions

import scalaz.Monad


sealed trait RqResult[+A] {
	val warning_r: List[String]
	def map[B](f: A => B): RqResult[B]
	def flatMap[B](f: A => RqResult[B]): RqResult[B]
	def foreach(f: A => Unit): Unit
	//def append[B](that: RqResult[B]): RqResult[B] = this.flatMap(_ => that)
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
	@inline final def orElse[B >: A](alternative: => RqResult[B]): RqResult[B] =
		if (isEmpty) alternative else this
	
	def flatten[B](implicit ev: A <:< RqResult[B]): RqResult[B]
}

object RqResult {
	def zero: RqResult[Unit] = RqSuccess[Unit](())
	def unit[A](a: A): RqResult[A] = RqSuccess[A](a)
	
	def toResultOfList[B](l: List[RqResult[B]]): RqResult[List[B]] = {
		l.foldRight(unit(List[B]()))((r, acc) => {
			acc.flatMap(l => r.map(_ :: l))
		})
	}
	
	def toResultOfTuple[A, B](tuple: (RqResult[A], RqResult[B])): RqResult[(A, B)] = {
		tuple match {
			case (RqSuccess(a, wa), RqSuccess(b, wb)) => RqSuccess((a, b), wa ++ wb)
			case (a, b) => RqError(a.getErrors ++ b.getErrors, a.getWarnings ++ b.getWarnings)
		}
	}
}

sealed case class RqSuccess[+A](res: A, warning_r: List[String] = Nil) extends RqResult[A] {
	def map[B](f: A => B): RqResult[B] = RqSuccess(f(res), warning_r)
	def flatMap[B](f: A => RqResult[B]): RqResult[B] = {
		f(res) match {
			case RqSuccess(res2, warning_r2) => RqSuccess(res2, warning_r2 ++ warning_r)
			case RqError(error_l2, warning_r2) => RqError(error_l2, warning_r2 ++ warning_r)
		}
	} 
	def foreach(f: A => Unit): Unit = f(res)

	def getErrors: List[String] = Nil

	def isError = false

	def getOrElse[B >: A](default: => B): B = res

	def flatten[B](implicit ev: A <:< RqResult[B]): RqResult[B] =
    	ev(res)
}

sealed case class RqError[+A](error_l: List[String], warning_r: List[String] = Nil) extends RqResult[A] {
	def map[B](f: A => B): RqResult[B] = RqError[B](error_l, warning_r)
	def flatMap[B](f: A => RqResult[B]): RqResult[B] = RqError[B](error_l, warning_r)
	def foreach(f: A => Unit): Unit = ()

	def getErrors: List[String] = error_l

	def isError = true

	def getOrElse[B >: A](default: => B): B = default

	def flatten[B](implicit ev: A <:< RqResult[B]): RqResult[B] =
    	RqError(error_l, warning_r)
}

object RqError {
	def apply[A](error: String): RqError[A] = RqError(List(error))
}

class RqOptionW[A](opt: Option[A]) {
	def asRq(error: String): RqResult[A] = opt match {
		case Some(x) => RqSuccess(x)
		case None => RqError(error)
	}
}

trait RqPimper {
	implicit def pimpedOption[A](opt: Option[A]) = new RqOptionW(opt)
	
	implicit def resultMonad: Monad[RqResult] = new Monad[RqResult] {
		def bind[A, B](fa: RqResult[A])(f: (A) => RqResult[B]) = fa.flatMap(f)
		def point[A](a: => A) = new RqSuccess[A](a)
	}
	
	implicit def resultResultToResult[A](o: RqResult[RqResult[A]]): RqResult[A] =
		o.flatten
	
	implicit def tryRqResultToRqResult[A](o: scala.util.Try[RqResult[A]]): RqResult[A] = {
		o match {
			case scala.util.Success(x) => x
			case scala.util.Failure(x) => RqError(x.getMessage())
		}
	}
	
	implicit def tryToRqResult[A](o: scala.util.Try[A]): RqResult[A] = {
		o match {
			case scala.util.Success(x) => RqSuccess(x)
			case scala.util.Failure(x) => RqError(x.getMessage())
		}
	}
}
