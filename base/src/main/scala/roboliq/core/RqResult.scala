package roboliq.core

import scala.language.implicitConversions

import scalaz.Monad


sealed trait RqResult[+A] {
	val warning_r: List[String]
	def map[B](f: A => B): RqResult[B]
	def flatMap[B](f: A => RqResult[B]): RqResult[B]
	def foreach(f: A => Unit): Unit
	
	def getWarnings: List[String] = warning_r.reverse
	def getErrors: List[String]
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
}

sealed case class RqError[+A](error_l: List[String], warning_r: List[String] = Nil) extends RqResult[A] {
	def map[B](f: A => B): RqResult[B] = RqError[B](error_l, warning_r)
	def flatMap[B](f: A => RqResult[B]): RqResult[B] = RqError[B](error_l, warning_r)
	def foreach(f: A => Unit): Unit = ()

	def getErrors: List[String] = error_l
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

object RqPimper {
	implicit def pimpedOption[A](opt: Option[A]) = new RqOptionW(opt)
	
	implicit def resultMonad: Monad[RqResult] = new Monad[RqResult] {
		def bind[A, B](fa: RqResult[A])(f: (A) => RqResult[B]) = fa.flatMap(f)
		def point[A](a: => A) = new RqSuccess[A](a)
	}
}
