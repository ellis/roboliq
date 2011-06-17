package bsse

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._

import evoware._


//sealed class BsseTip(val roboTip: Tip)

class BsseTranslator(robot: BsseRobot) extends EvowareTranslator(robot) {
	def clean(tips: Seq[Tip], degree: CleanDegree.Value): List[T0_Token] = {
		if (tips.isEmpty || degree == CleanDegree.None)
			return Nil
		
		// Make sure we only try to clean same-sized tips
		val tipKind = robot.getTipKind(tips.head)
		assert(tips.forall(tip => robot.getTipKind(tip) == tipKind))
		
		val tipStates = tips.map(tip => state.getTipState(tip))
		// Calculate an overall tip state for maximum contamination
		val tipStateAcc = tipState.foldRight(TipState(tips.head)) { (tipState, acc) =>
				acc.aspirate(tipState.liquid, tipState.nContamInsideVolume)
				   .dispense(tipState.nContamInsideVolume)
		}
		val nContamInsideVolume = tipStateAcc.nContamInsideVolume

		degreeMin match {
			case None => Nil
			case Light => cleanGroupLight(tips, nContamInsideVolume)
			case Thorough => cleanGroupLight(tips, nContamInsideVolume)
			case Decontaminate => cleanGroupDecontam(tips, nContamInsideVolume)
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
			case Decontaminate => cleanGroupLight(tips)
		}
	}
	*/
	
	private def cleanGroupLight(tips: Seq[Tip], nContamInsideVolume: Double): List[T0_Token] = {
		val tipKind = getTipKind(tips.head)
		val mTips = EvowareTranslator.encodeTips(tips)
		// TODO: Set these values depending on the tip kind
		val nWasteDelay = 500
		val nCleanerVolume = 10.0
		val nCleanerDelay = 500
		val nAirgapVolume = 10
		val nAirgapSpeed = 70
		val nRetractSpeed = 30
		
		assert(tipKind.nDecontaminateVolumeExtra >= 0)
		
		List(
			T0_Wash(
				mTips,
				iWasteGrid = 2, iWasteSite = 1,
				iCleanerGrid = 2, iCleanerSite = 2,
				nWastVolume = nContamInsideVolume + tipKind.nDecontaminateVolumeExtra,
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
				nWastVolume = 4, // FIXME: how should this be calculated? -- ellis, 2011-06-16
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
	
	private def cleanGroupDecontam(tips: Seq[Tip], nContamInsideVolume: Double): List[T0_Token] = {
		val tipKind = getTipKind(tips.head)
		val mTips = EvowareTranslator.encodeTips(tips)
		// TODO: Set these values depending on the tip kind
		val nWasteDelay = 500
		val nCleanerVolume = 10.0
		val nCleanerDelay = 500
		val nAirgapVolume = 10
		val nAirgapSpeed = 70
		val nRetractSpeed = 30
		
		assert(tipKind.nDecontaminateVolumeExtra >= 0)

		val nVolume = nContamInsideVolume + tipKind.nDecontaminateVolumeExtra
		val twvsAspirate = tips.map(tip => new TipWellVolume(tip, robot.plateDecon.wells(1), nVolume))
		val twvsDispense = tips.map(tip => new TipWellVolume(tip, robot.plateDecon.wells(2), nVolume))
		
		T0_Wash(
			mTips,
			iWasteGrid = 2, iWasteSite = 1,
			iCleanerGrid = 2, iCleanerSite = 2,
			nWastVolume = 15, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nWasteDelay = nWasteDelay,
			nCleanerVolume = 3, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nCleanerDelay = nCleanerDelay,
			nAirgapVolume = nAirgapVolume,
			nAirgapSpeed = nAirgapSpeed,
			nRetractSpeed = nRetractSpeed,
			bFastWash = true
		) :: translate(T1_Aspirate(twvsAspirate)) ::: translate(T1_Aspirate(twvsDispense)) :::
		T0_Wash(
			mTips,
			iWasteGrid = 2, iWasteSite = 1,
			iCleanerGrid = 2, iCleanerSite = 2,
			nWastVolume = 2, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nWasteDelay = nWasteDelay,
			nCleanerVolume = 5, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nCleanerDelay = nCleanerDelay,
			nAirgapVolume = nAirgapVolume,
			nAirgapSpeed = nAirgapSpeed,
			nRetractSpeed = nRetractSpeed,
			bFastWash = true
		) ::
		T0_Wash(
			mTips,
			iWasteGrid = 1, iWasteSite = 1,
			iCleanerGrid = 1, iCleanerSite = 2,
			nWastVolume = 2, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nWasteDelay = nWasteDelay,
			nCleanerVolume = 5, // FIXME: how should this be calculated? -- ellis, 2011-06-16
			nCleanerDelay = nCleanerDelay,
			nAirgapVolume = nAirgapVolume,
			nAirgapSpeed = nAirgapSpeed,
			nRetractSpeed = nRetractSpeed,
			bFastWash = true
		) :: Nil
	}
}
