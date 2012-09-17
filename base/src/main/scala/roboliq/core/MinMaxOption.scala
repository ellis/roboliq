package roboliq.core

import scalaz._
import Scalaz._


case class MinMaxOption[A <: Ordered[A]](val option: Option[(A, A)]) {
	def +(a: A): MinMaxOption[A] = {
		val minMax = option match {
			case None => (a, a)
			case Some(mm @ (min, max)) =>
				if (a < min) (a, max)
				else if (a > max) (min, a)
				else mm
		}
		MinMaxOption(Some(minMax))
	}

	def +(mmo: MinMaxOption[A]): MinMaxOption[A] = {
		MinMaxOption((option, mmo.option) match {
			case (Some(mm1), Some(mm2)) => Some(List(mm1._1, mm2._1).min, List(mm1._2, mm2._2).max)
			case _ => option orElse mmo.option
		})
	}
}

class MinMaxOptionMonoid[A <: Ordered[A]] extends Monoid[MinMaxOption[A]] {
	val zero = MinMaxOption[A](None)
	def append(m1: MinMaxOption[A], m2: => MinMaxOption[A]): MinMaxOption[A] = {
		m1 + m2
	}
}
