package roboliq.core

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe.Type


trait Cmd {
	// REFACTOR: The `cmd` should be turned into a class attribute instead, and probably renamed to `kind`
	def cmd: String
	def typ: Type
}

trait CmdToken // REFACTOR: rename to Token
