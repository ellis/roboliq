package meta

import concrete.PipettingRule

sealed class CleanPolicy(val before: Int = 0, val during: Int = 0, val after: Int = 0)

sealed class MetaToken
case class CopyPlate(source: Plate, dest: Plate, volume: Double, rule: PipettingRule, clean: CleanPolicy) extends MetaToken
