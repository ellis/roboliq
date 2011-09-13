package roboliq.common

import scala.collection.mutable.ArrayBuffer

import roboliq.common._


trait RoboliqCommands {
	val cmds: ArrayBuffer[Command]
}

abstract class LiquidPropertiesFamily
object LiquidPropertiesFamily {
	case object Water extends LiquidPropertiesFamily
	case object Cells extends LiquidPropertiesFamily
	case object DMSO extends LiquidPropertiesFamily
	case object Decon extends LiquidPropertiesFamily
}

object WashIntensity extends Enumeration {
	val None, Light, Thorough, Decontaminate = Value
	
	def max(a: WashIntensity.Value, b: WashIntensity.Value): WashIntensity.Value = {
		if (a >= b) a else b
	}
}

sealed abstract class Result[+T] {
	def flatMap[B](f: T => Result[B]): Result[B]
	def map[B](f: T => B): Result[B]
	def isError: Boolean
	def isSuccess: Boolean
	
	final def >>=[B](f: T => Result[B]): Result[B] = flatMap(f)
}
case class Error[T](lsError: Seq[String]) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = Error[B](lsError)
	def map[B](f: T => B): Result[B] = Error[B](lsError)
	def isError: Boolean = true
	def isSuccess: Boolean = false
}
object Error {
	def apply(sError: String) = new Error(Seq(sError))
}
case class Success[T](value: T) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = f(value)
	def map[B](f: T => B): Result[B] = Success[B](f(value))
	def isError: Boolean = false
	def isSuccess: Boolean = true
}
object Result {
	def sequence[A](l: Seq[Result[A]]): Result[Seq[A]] = {
		val llsError = l.collect({ case Error(lsError) => lsError })
		if (!llsError.isEmpty)
			Error(llsError.flatten)
		else
			Success(l.map(_.asInstanceOf[Success[A]].value))
	}
	
	def map[A, B](l: Iterable[A], f: (A) => Result[B]): Result[Iterable[B]] = {
		Success(l.map(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def map[A, B](l: Seq[A], f: (A) => Result[B]): Result[Seq[B]] = {
		Success(l.map(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def mapOver[A, B](l: Seq[A])(f: A => Result[B]): Result[Seq[B]] = {
		Success(l.map(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
	
	def flatMap[A, B](l: Iterable[A], f: A => Result[Iterable[B]]): Result[Iterable[B]] = {
		Success(l.flatMap(a => f(a) match {
			case Error(lsError) => return Error(lsError)
			case Success(b) => b
		}))
	}
}