package roboliq.core

import scala.collection.mutable.ArrayBuffer

import scalaz.Semigroup

trait Cmd {
	// REFACTOR: The `cmd` should be turned into a class attribute instead, and probably renamed to `kind`
	def cmd: String
}

trait CmdToken
