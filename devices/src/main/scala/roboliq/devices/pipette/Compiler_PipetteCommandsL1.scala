package roboliq.devices.pipette

import roboliq.common._
import roboliq.level3._


class Compiler_AspirateL1 extends CommandCompilerL1 {
	type CmdType = L1_Aspirate
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (twv <- cmd.twvs) {
			val liquid = twv.well.obj.state(builder).liquid
			twv.tip.stateWriter(builder).aspirate(liquid, twv.nVolume)
			twv.well.obj.stateWriter(builder).remove(twv.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class Compiler_DispenseL1 extends CommandCompilerL1 {
	type CmdType = L1_Dispense
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (twv <- cmd.twvs) {
			val tipState = twv.tip.state(builder)
			val wellState = twv.well.obj.state(builder)
			twv.tip.stateWriter(builder).dispense(twv.nVolume, wellState.liquid, twv.policy.pos)
			twv.well.obj.stateWriter(builder).add(tipState.liquid, twv.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class Compiler_L1_Mix extends CommandCompilerL1 {
	type CmdType = L1_Mix
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (twvpc <- cmd.twvpcs) {
			val wellState = twvpc.well.obj.state(builder)
			twvpc.tip.stateWriter(builder).mix(wellState.liquid, twvpc.nVolume)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 1
}

class Compiler_SetTipStateCleanL1 extends CommandCompilerL1 {
	type CmdType = L1_SetTipStateClean
	val cmdType = classOf[CmdType]
	
	def updateState(builder: StateBuilder, cmd: CmdType) {
		for (tip <- cmd.tips) {
			tip.stateWriter(builder).clean(cmd.degree)
		}
	}
	
	def score(state0: RobotState, state1: RobotState, cmd: CmdType): Int = 10
}
