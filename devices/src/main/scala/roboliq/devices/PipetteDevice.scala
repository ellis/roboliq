package roboliq.devices

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._

import roboliq.devices.pipette._


class PipetteDevice {
	/*def translate2(robot: Robot, state0: RobotState, tok: T2_Token): Seq[T1_TokenState] = {
		tok match {
			case t2 @ T2_Pipette(_) =>
				val comp = new T2_Pipette_Compiler(robot, state0, t2)
				comp.tokens
			case t2 @ T2_PipetteLiquid(_, _) =>
				val comp = new T2_PipetteLiquid_Compiler(robot, state0, t2)
				comp.tokens
			case t2 @ T2_PipettePlate(_, _, _) =>
				val comp = new T2_PipettePlate_Compiler(robot, state0, t2)
				comp.tokens
			case t2 @ T2_PipetteMix(_, _, _) =>
				val comp = new T2_PipetteMix_Compiler(robot, state0, t2)
				comp.tokens
		}
	}
	*/
	def getDispensePolicy(tipState: TipState, wellState: WellState, nVolume: Double): Option[Tuple2[PipettePolicy, String]] = {
		val tipKind = getTipKind(tipState.tip)
		val liquid = tipState.liquid
		
		val bLarge = (tipKind.sName == "large")
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (liquid.bCells)
			if (bLarge) Some("Comp cells free dispense") else None
		else if (liquid.sName.contains("DMSO"))
			if (bLarge) Some("DMSO free dispense") else None
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some("D-BSSE Decon")
		// If our volume is high enough that we don't need to worry about accuracy
		else if (nVolume >= nFreeDispenseVolumeThreshold)
			if (bLarge) Some("Water free dispense") else None
		else if (wellState.nVolume == 0)
			if (bLarge) Some("Water dry contact") else Some("D-BSSE Te-PS Dry Contact")
		else
			if (bLarge) Some("Water wet contact") else Some("D-BSSE Te-PS Wet Contact")
	}

	def getDispenseClass(tipState: TipState, wellState: WellState, nVolume: Double): Option[String] = {
		val tipKind = getTipKind(tipState.tip)
		val liquid = tipState.liquid
		
		val bLarge = (tipKind.sName == "large")
		// If our volume is high enough that we don't need to worry about accuracy,
		// or if we're pipetting competent cells,
		// then perform a free dispense.
		if (liquid.bCells)
			if (bLarge) Some("Comp cells free dispense") else None
		else if (liquid.sName.contains("DMSO"))
			if (bLarge) Some("DMSO free dispense") else None
		else if (liquid.sName.contains("D-BSSE Decon"))
			Some("D-BSSE Decon")
		// If our volume is high enough that we don't need to worry about accuracy
		else if (nVolume >= nFreeDispenseVolumeThreshold)
			if (bLarge) Some("Water free dispense") else None
		else if (wellState.nVolume == 0)
			if (bLarge) Some("Water dry contact") else Some("D-BSSE Te-PS Dry Contact")
		else
			if (bLarge) Some("Water wet contact") else Some("D-BSSE Te-PS Wet Contact")
	}
}

object PipetteDeviceUtil {
	def updateState(state0: RobotState, tok: T1_Token): RobotState = {
		val builder = new RobotStateBuilder(state0)
		tok match {
			case T1_Aspirate(twvs) => twvs.foreach(builder.aspirate)
			case T1_Dispense(twvds) => twvds.foreach(builder.dispense)
			case t1 @ T1_Clean(_, _) => t1.tips.foreach(tip => builder.cleanTip(tip, t1.degree))
		}
		builder.toImmutable
	}
	
	def getTokenStates(state0: RobotState, toks: Seq[T1_Token]): Seq[T1_TokenState] = {
		var state = state0
		for (tok <- toks) yield {
			state = updateState(state, tok)
			new T1_TokenState(tok, state)
		}
	}
}
