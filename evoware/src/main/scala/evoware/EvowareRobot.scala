package evoware

import scala.collection.mutable.ArrayBuffer

import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._


abstract class EvowareRobot extends Robot {
	/*
	def powerSet[A](s: Set[A]) = s.foldLeft(Set(Set.empty[A])) {
		(set, element) =>
			set union (set map (_ + element))
	}
	
	val config = {
		val nTips = settings.kindOfTips.size
		val tips = (0 until nTips).map(new Tip(_))
		val tipKindsPowerset = powerSet(settings.tipKinds.toSet).filter(!_.isEmpty)
		val kindOfTipsIndexed = settings.kindOfTips.zipWithIndex
		val tipGroups = tipKindsPowerset.map(kinds => kindOfTipsIndexed.filter((kind, iTip) => kinds.contains(kind)).map(_._2).toArray).toArray
		new RobotConfig(tips, tipGroups)
	}
	*/
	//var state: RobotState = RobotState.empty
	
	/*def chooseWellsForTips(tips: Seq[Tip], wells: Seq[Well]): Seq[Well] = {
		val chosen = new ArrayBuffer[Well]
		chosen ++= wells.take(tips.size)
		chosen
	}*/
	
	def score(tokens: Seq[T1_Token]): Int = {
		tokens.size
	}

	def getTipKind(tip: Tip): EvowareTipKind // TODO: EvowareSetupState
	def getAspirateClass(tipState: TipState, wellState: WellState): Option[String]
	def getDispenseClass(tipState: TipState, wellState: WellState, nVolume: Double): Option[String]
	
	def getAspirateClass(state: IRobotState, twv: TipWellVolume): Option[String] = getAspirateClass(state.getTipState(twv.tip), state.getWellState(twv.well))
	def getDispenseClass(state: IRobotState, twv: TipWellVolume): Option[String] = getDispenseClass(state.getTipState(twv.tip), state.getWellState(twv.well), twv.nVolume)
	
	//def getGridIndex(part: Part): Option[Int] = evowareSetup.mapPartToGrid.get(part)
}
