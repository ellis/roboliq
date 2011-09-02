package roboliq.compiler

import roboliq.common._


class Robot(
	val devices: Seq[Device],
	val processors: Seq[CommandCompiler]
) {
}
