package roboliq.input

import roboliq.core.ResultC

// Maybe this should be a type class instead of a trait
trait Mergable[A] {
	def merge(that: A): ResultC[A]
}