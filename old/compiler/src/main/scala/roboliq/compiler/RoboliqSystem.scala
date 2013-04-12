package roboliq.compiler

import roboliq.common._


trait RoboliqSystem {
	val devices: Seq[Device]
	val processors: Seq[CommandCompiler]
}
