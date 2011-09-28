package roboliq.robots.evoware.devices.centrifuge

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.centrifuge._
import roboliq.commands.move._
import roboliq.compiler._


class L3P_Centrifuge(device: CentrifugeDevice) extends CommandCompilerL3 {
	type CmdType = L3C_Centrifuge
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val cmds = new ArrayBuffer[Command]
		
		def open() { cmds += CentrifugeOpen.L3C(new CentrifugeOpen.L3A(Some(device), ())) }
		def close() { cmds += CentrifugeClose.L3C(new CentrifugeClose.L3A(Some(device), ())) }
		def moveTo(iPosition: Int) { cmds += CentrifugeMoveTo.L3C(new CentrifugeMoveTo.L3A(Some(device), iPosition)) }
		def movePlate(plate: PlateConfigL2) { cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(device.location), None)) }
		
		val plates = cmd.args.plates
		val nPlates = plates.size
		val states = ctx.states
		for {
			_ <- Result.assert(device.nSlots > 0, "centrifuge must have a positive number of slots")
			_ <- Result.assert((device.nSlots % 2) == 0, "centrifuge must have an even number of slots")
			_ <- Result.assert(nPlates <= device.nSlots, "INTERNAL: cannot centrifuge more plates than there are slots available")
			_ <- Result.assert(nPlates > 4, "INTERNAL: not yet able to handle centrifugation of more than 4 plates")
			_ <- Result.assert(true, "")
		} yield {
			open()
			
			/*val deviceState = device.state(states)
			if (deviceState.iPosition_?.getOrElse(-1) != 1) {
				moveTo(1)
				open()
			}*/
			
			if (nPlates >= 1) {
				moveTo(0)
				movePlate(plates(0))
			}
			if (nPlates >= 2) {
				val i = device.nSlots / 2
				moveTo(i)
				movePlate(plates(1))
			}
			if (nPlates >= 3) {
				val i = device.nSlots / 4
				moveTo(i)
				movePlate(plates(2))
			}
			if (nPlates >= 4) {
				val i = device.nSlots - (device.nSlots / 4)
				moveTo(i)
				movePlate(plates(3))
			}
			
			if ((nPlates % 2) != 0) {
				if (device.setup.plate_balance == null)
					return Error("must set balance plate for centrifuge")
				val plate_balance = device.setup.plate_balance.state(states).conf
				val i = device.nSlots / 2 + 1
				moveTo(i)
				movePlate(plate_balance)
			}
			
			close()
			
			cmds += CentrifugeRun.L3C(new CentrifugeRun.L3A(Some(device), "(program)"))
			
			cmds.toSeq
		}
	}
}
