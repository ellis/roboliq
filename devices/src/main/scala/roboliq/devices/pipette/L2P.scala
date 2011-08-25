/*
package roboliq.devices.pipette

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L2P_Aspirate extends CommandCompilerL2 {
	type CmdType = L2C_Aspirate
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (item <- cmd.items) {
			val wellState = item.well.obj.state(builder)
			item.tip.obj.stateWriter(builder).aspirate(wellState.liquid, item.nVolume)
			item.well.obj.stateWriter(builder).remove(item.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class L2P_Dispense extends CommandCompilerL2 {
	type CmdType = L2C_Dispense
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (item <- cmd.items) {
			val tipState = item.tip.obj.state(builder)
			val wellState = item.well.obj.state(builder)
			item.tip.obj.stateWriter(builder).dispense(item.nVolume, wellState.liquid, item.policy.pos)
			item.well.obj.stateWriter(builder).add(tipState.liquid, item.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class L2P_Mix extends CommandCompilerL2 {
	type CmdType = L2C_Mix
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (item <- cmd.items) {
			val wellState = item.well.obj.state(builder)
			item.tip.obj.stateWriter(builder).mix(wellState.liquid, item.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class L2P_Wash extends CommandCompilerL2 {
	type CmdType = L2C_Wash
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (tip <- cmd.tips) {
			tip.obj.stateWriter(builder).clean(cmd.degree)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 5
}

class L2P_SetTipStateClean extends CommandCompilerL2 {
	type CmdType = L2C_SetTipStateClean
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (tip <- cmd.tips) {
			tip.obj.stateWriter(builder).clean(cmd.degree)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 10
}
*/