package roboliq.commands.arm

object LidHandling extends Enumeration {
        val NoLid, CoverAtSource, RemoveAtSource = Value
}

sealed abstract class LidHandlingSpec
case class LidCoverSpec(location: String) extends LidHandlingSpec
case class LidRemoveSpec(location: String) extends LidHandlingSpec
