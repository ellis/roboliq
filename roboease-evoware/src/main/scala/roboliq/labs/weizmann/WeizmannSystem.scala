package roboliq.labs.weizmann

import roboliq.common._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.devices.pipette._
import evoware.EvowareSystem
import evoware.SiteObj
import evoware.WashProgramArgs
import roboliq.robots.evoware.devices._
import roboliq.labs.weizmann.devices._


class WeizmannSystem(val sites: Iterable[SiteObj]) extends RoboliqSystem with EvowareSystem {
	val mover = new WeizmannMoveDevice
	val pipetter = new WeizmannPipetteDevice
	
	val devices = Seq(
		mover,
		pipetter
	)
	
	val processors = Seq(
		new L3P_CleanPending(pipetter),
		new L3P_Mix(pipetter),
		new L3P_MovePlate(mover),
		new L3P_Pipette(pipetter),
		//new L3P_Shake_HPShaker("shaker"),
		new L3P_TipsDrop("WASTE"),
		new L3P_TipsReplace
	)

	val washProgramArgs0 = new WashProgramArgs(
		iWasteGrid = 1, iWasteSite = 1,
		iCleanerGrid = 1, iCleanerSite = 0,
		nWasteVolume_? = Some(2),
		nWasteDelay = 500,
		nCleanerVolume = 1,
		nCleanerDelay = 500,
		nAirgapVolume = 20,
		nAirgapSpeed = 70,
		nRetractSpeed = 30,
		bFastWash = true,
		bUNKNOWN1 = true
	)	
}
