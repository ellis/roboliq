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
		def removePlate(plate: PlateConfigL2) { cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(plate.state(ctx.states).location), None)) }
		
		val nPlates = cmd.args.plates.size
		val states = ctx.states
		for {
			_ <- Result.assert(device.nSlots > 0, "centrifuge must have a positive number of slots")
			_ <- Result.assert((device.nSlots % 2) == 0, "centrifuge must have an even number of slots")
			_ <- Result.assert(nPlates <= device.nSlots, "INTERNAL: cannot centrifuge more plates than there are slots available")
			_ <- Result.assert(nPlates <= 4, "INTERNAL: not yet able to handle centrifugation of more than 4 plates")
			program <- Result.get(cmd.args.idProgram_?, "must set centrifuge program")
		} yield {
			open()
			
			/*val deviceState = device.state(states)
			if (deviceState.iPosition_?.getOrElse(-1) != 1) {
				moveTo(1)
				open()
			}*/
			
			val plates2 = cmd.args.plates ++ {
				if ((nPlates % 2) != 0) {
					if (device.plate_balance == null)
						return Error("must set balance plate for centrifuge")
					val plate_balance = device.plate_balance.state(states).conf
					Seq(plate_balance)
				}
				else {
					Seq()
				}
			}
			val nPlates2 = plates2.size

			// Put the plates in
			val nSlots = device.nSlots
			val liPos = Seq(0, nSlots / 2, nSlots / 4, nSlots - nSlots / 4)
			for (iPlate <- 0 until nPlates2) {
				moveTo(liPos(iPlate))
				movePlate(plates2(iPlate))
			}
			
			close()
			
			cmds += CentrifugeRun.L3C(new CentrifugeRun.L3A(Some(device), program))
			
			open()
			
			// Take the plates out
			for (iPlate <- 0 until nPlates2) {
				moveTo(liPos(iPlate))
				removePlate(plates2(iPlate))
			}
			
			moveTo(0)
			close()
			
			cmds.toSeq
		}
	}
}
