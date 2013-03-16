package roboliq.test.bsse

import roboliq.common
import roboliq.common.WellIndex
import roboliq.commands.pipette._
import roboliq.labs.bsse.Protocol


class Wells extends roboliq.common.WellPointerVar
class EmptyWells extends Wells

trait ProtocolSettings {
	import roboliq.common._
	import roboliq.robots.evoware._
	
	val kb: roboliq.common.KnowledgeBase
	
	def setLiquid(liquid: Liquid, loc: WellPointer) {
		
	}
	
	def setWells(wells: Wells, loc: WellPointer) {
		
	}
	
	/*def reagent(reagent: Reagent, plateE: PlateObj, iWell0: Int, nWells: Int = 1) {
		val plate = plateE.commonObj
		val plateSetup = kb.getPlateSetup(plate)
		val wells = plateSetup.dim_?.get.wells
		val range = (iWell0 - 1) until (iWell0 - 1 + nWells)
		for (iWell <- range) {
			val well = wells(iWell)
			val wellSetup = kb.getWellSetup(well)
			wellSetup.reagent_? = Some(reagent)
		}
	}*/
	
	def setPlate(plate: common.Plate, site: SiteObj, model: common.PlateModel) {
		val setup = kb.getPlateSetup(plate)
		val proxy = new PlateProxy(kb, plate)
		setup.model_? = Some(model)
		proxy.location = site.sName
		proxy.setDimension(model.nRows, model.nCols)
	}
}

class ProtocolTest extends Protocol {
	//class EmptyPlate extends Plate
	type EmptyPlate = Plate
	
	class MixByVolumeProgram {
		def apply(w: Wells) {}
	}
}

class Protocol01(station: roboliq.labs.bsse.station1.StationConfig) extends ProtocolTest with ProtocolSettings {
	val plateSrc = new Plate
	val plateDest = new EmptyPlate
	
	pipette(plateSrc(A1+12), plateDest(A1+4)+plateDest(A2+2)+plateDest(A1+4)+plateDest(A2+2), 30)
	
	import station._
	setPlate(plateSrc, station.Sites.cooled1, station.LabwareModels.platePcr)
	setPlate(plateDest, station.Sites.cooled1, station.LabwareModels.platePcr)
	
	val sExpect = """Subroutine("C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashBigTips.esc",0);
Aspirate(15,"Roboliq_Water_Wet_1000","30","30","30","30",0,0,0,0,0,0,0,0,17,0,1,"0C08?0000000000000",0,0);
Dispense(15,"Roboliq_Water_Air_1000","30","30","30","30",0,0,0,0,0,0,0,0,17,0,1,"0C08?0000000000000",0,0);
Subroutine("C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashBigTips.esc",0);
Aspirate(3,"Roboliq_Water_Wet_1000","30","30",0,0,0,0,0,0,0,0,0,0,17,0,1,"0C08`0000000000000",0,0);
Dispense(3,"Roboliq_Water_Air_1000","30","30",0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0806000000000000",0,0);
Subroutine("C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashBigTips.esc",0);
Aspirate(3,"Roboliq_Water_Wet_1000","30","30",0,0,0,0,0,0,0,0,0,0,17,0,1,"0C08p1000000000000",0,0);
Aspirate(12,"Roboliq_Water_Wet_1000",0,0,"30","30",0,0,0,0,0,0,0,0,17,0,1,"0C0806000000000000",0,0);
Dispense(15,"Roboliq_Water_Air_1000","30","30","30","30",0,0,0,0,0,0,0,0,17,0,1,"0C08?0000000000000",0,0);
Subroutine("C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashBigTips.esc",0);
Aspirate(3,"Roboliq_Water_Wet_1000","30","30",0,0,0,0,0,0,0,0,0,0,17,0,1,"0C080H000000000000",0,0);
Dispense(3,"Roboliq_Water_Air_1000","30","30",0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0806000000000000",0,0);
Subroutine("C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashBigTips.esc",0);
Subroutine("C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashSmallTips.esc",0);"""
}

class Protocol02(nVolume: Double) extends ProtocolTest {
	val plateSrc = new Plate
	val plateDest = new EmptyPlate
	val wellsSrc = new Wells
	val wellsDest = new Wells
	pipette(wellsSrc, wellsDest, nVolume)
}

class Protocol02Setup(protocol: Protocol02, station: roboliq.labs.bsse.station1.StationConfig) extends ProtocolSettings {
	val kb = protocol.kb
}
