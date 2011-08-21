package roboliq.devices.pipette

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L1P_Aspirate extends CommandCompilerL1 {
	type CmdType = L1C_Aspirate
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (twv <- cmd.twvs) {
			val liquid = twv.well.state(builder).liquid
			twv.tip.stateWriter(builder).aspirate(liquid, twv.nVolume)
			twv.well.stateWriter(builder).remove(twv.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class L1P_Dispense extends CommandCompilerL1 {
	type CmdType = L1C_Dispense
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (twv <- cmd.twvs) {
			val tipState = twv.tip.state(builder)
			val wellState = twv.well.state(builder)
			twv.tip.stateWriter(builder).dispense(twv.nVolume, wellState.liquid, twv.policy.pos)
			twv.well.stateWriter(builder).add(tipState.liquid, twv.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class L1P_Mix extends CommandCompilerL1 {
	type CmdType = L1C_Mix
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (twvpc <- cmd.twvpcs) {
			val wellState = twvpc.well.state(builder)
			twvpc.tip.stateWriter(builder).mix(wellState.liquid, twvpc.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class L1P_Wash extends CommandCompilerL1 {
	type CmdType = L1C_Wash
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (tip <- cmd.tips) {
			tip.stateWriter(builder).clean(cmd.degree)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 5
}

class L1P_SetTipStateClean extends CommandCompilerL1 {
	type CmdType = L1C_SetTipStateClean
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (tip <- cmd.tips) {
			tip.stateWriter(builder).clean(cmd.degree)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 10
}
