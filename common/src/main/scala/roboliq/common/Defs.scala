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

sealed abstract class Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B]
	def map[B](f: T => B): Result[B]
}
case class Error[T](lsError: Seq[String]) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = Error[B](lsError)
	def map[B](f: T => B): Result[B] = Error[B](lsError)
}
case class Success[T](value: T) extends Result[T] {
	def flatMap[B](f: T => Result[B]): Result[B] = f(value)
	def map[B](f: T => B): Result[B] = Success[B](f(value))
}
