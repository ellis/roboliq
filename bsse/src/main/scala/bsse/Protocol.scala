package bsse

import roboliq.common._
//import roboliq.commands.pipette._
import roboliq.protocol.CommonProtocol
import roboliq.devices.pipette._

trait Protocol extends CommonProtocol with PipetteCommands {
}
