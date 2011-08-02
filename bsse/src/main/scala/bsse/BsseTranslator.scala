package bsse

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._

import evoware._


//sealed class BsseTip(val roboTip: Tip)

class BsseTranslator(robot: BsseRobot) extends EvowareTranslator(robot) {
	def clean(state0: RobotState, tok: T1_Clean): List[T0_Token] = {
		val tips = tok.tips
		val degree = tok.degree
		if (tips.isEmpty || degree == CleanDegree.None)
			return Nil
		
		// Make sure we only try to clean same-sized tips
		val tipKind = robot.getTipKind(tips.head)
		assert(tips.forall(tip => robot.getTipKind(tip) == tipKind))
		
		val tipStates = tips.map(tip => state0.getTipState(tip))
		// Calculate an overall tip state for maximum contamination
		val tipStateAcc = tipStates.foldRight(TipState(tips.head)) { (tipState, acc) =>
				acc.aspirate(tipState.liquid, tipState.nContamInsideVolume)
				   .dispenseFree(tipState.nContamInsideVolume)
		}
		val nContamInsideVolume = tipStateAcc.nContamInsideVolume

		degree match {
			case CleanDegree.None => Nil
			case CleanDegree.Light => cleanGroupLight(state0, tips, nContamInsideVolume)
			case CleanDegree.Thorough => cleanGroupLight(state0, tips, nContamInsideVolume)
			case CleanDegree.Decontaminate => cleanGroupDecontam(state0, tips, nContamInsideVolume).toList
		}
	}
	
	/*
	private def cleanGroup(tips: Seq[Tip], degreeMin: CleanDegree.Value): List[T0_Token] = {
		val tipStates = tips.map(tip => state.getTipState(tip))
		// Calculate an overall tip state for maximum contamination
		val tipStateAcc = tipState.foldRight(TipState(tips.head)) { (tipState, acc) =>
				acc.aspirate(tipState.liquid, tipState.nContamInsideVolume)
				   .dispense(tipState.nContamInsideVolume)
		}
		val bRequireDecontam = tipStateAcc.contamInside.contaminated || tipStateAcc.contamOutside.contaminated
		val degree = {
			if (bRequireDecontam) CleanDegree.Decontaminate
			else degreeMin
		}
		
		degreeMin match {
			case None => Nil
			case Light => cleanGroupLight(tips)
			case Thorough => cleanGroupLight(tips)
			case Decontaminate => cleanGroupLight(tips).toList
		}
	}
	*/
	
	private def cleanGroupLight(state0: RobotState, tips: Seq[Tip], nContamInsideVolume: Double): List[T0_Token] = {
		val tipKind = robot.getTipKind(tips.head)
		val mTips = encodeTips(tips)
		// TODO: Set these values depending on the tip kind
		val nWasteDelay = 500
		val nCleanerVolume = 10.0
		val nCleanerDelay = 500
		val nAirgapVolume = 10
		val nAirgapSpeed = 70
		val nRetractSpeed = 30
		
		assert(tipKind.nWashVolumeExtra >= 0)
		
		List(
			T0_Wash(
				mTips,
				iWasteGrid = 2, iWasteSite = 1,
				iCleanerGrid = 2, iCleanerSite = 2,
				nWasteVolume = nContamInsideVolume + tipKind.nWashVolumeExtra,
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 4, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = true
			),
			T0_Wash(
				mTips,
				iWasteGrid = 1, iWasteSite = 1,
				iCleanerGrid = 1, iCleanerSite = 2,
				nWasteVolume = 4, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nWasteDelay = nWasteDelay,
				nCleanerVolume = 1, // FIXME: how should this be calculated? -- ellis, 2011-06-16
				nCleanerDelay = nCleanerDelay,
				nAirgapVolume = nAirgapVolume,
				nAirgapSpeed = nAirgapSpeed,
				nRetractSpeed = nRetractSpeed,
				bFastWash = false
			)
		)
	}
	
	private def cleanGroupDecontam(state0: RobotState, tips: Seq[Tip], nContamInsideVolume: Double): Seq[T0_Token] = {
		val tipKind = robot.getTipKind(tips.head)
		val mTips = encodeTips(tips)
		// TODO: Set these values depending on the tip kind
		val nWasteDelay = 500
		val nCleanerVolume = 10.0
		val nCleanerDelay = 500
		val nAirgapVolume = 10
		val nAirgapSpeed = 70
		val nRetractSpeed = 30
		
		assert(tipKind.nWashVolumeExtra >= 0)
		assert(tips.size <= robot.plateDecon2.wells.size)
		
		val nVolume = nContamInsideVolume + tipKind.nWashVolumeExtra
		val iWell0 = (robot.plateDecon2.wells.size - tips.size) / 2
		val wells2 = robot.plateDecon2.wells.drop(iWell0)
		val wells3 = robot.plateDecon3.wells.drop(iWell0)
		val tws2 = tips zip wells2
		val tws3 = tips zip wells3
		val twvsAspirate = tws2.map{case (tip, well) => new TipWellVolumePolicy(tip, well, nVolume, PipettePolicy(PipettePosition.Free))}
		val twvdsDispense = tws3.map{case (tip, well) => new TipWellVolumePolicy(tip, well, nVolume, PipettePolicy(PipettePosition.Free))}
		
		Seq(T0_Wash(
			mTips,
			iWasteGrid = 2, iWasteSite = 1,
			iCleanerGrid = 2, iCleanerSite = 2,
			nWasteVolume = 15, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nWasteDelay = nWasteDelay,
			nCleanerVolume = 3, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nCleanerDelay = nCleanerDelay,
			nAirgapVolume = nAirgapVolume,
			nAirgapSpeed = nAirgapSpeed,
			nRetractSpeed = nRetractSpeed,
			bFastWash = true
		)) ++ translate(state0, T1_Aspirate(twvsAspirate)) ++
		translate(state0, T1_Dispense(twvdsDispense)) ++
		Seq(T0_Wash(
			mTips,
			iWasteGrid = 2, iWasteSite = 1,
			iCleanerGrid = 2, iCleanerSite = 2,
			nWasteVolume = 2, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nWasteDelay = nWasteDelay,
			nCleanerVolume = 5, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nCleanerDelay = nCleanerDelay,
			nAirgapVolume = nAirgapVolume,
			nAirgapSpeed = nAirgapSpeed,
			nRetractSpeed = nRetractSpeed,
			bFastWash = true
		),
		T0_Wash(
			mTips,
			iWasteGrid = 1, iWasteSite = 1,
			iCleanerGrid = 1, iCleanerSite = 2,
			nWasteVolume = 2, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nWasteDelay = nWasteDelay,
			nCleanerVolume = 5, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nCleanerDelay = nCleanerDelay,
			nAirgapVolume = nAirgapVolume,
			nAirgapSpeed = nAirgapSpeed,
			nRetractSpeed = nRetractSpeed,
			bFastWash = true
		))
	}
}
