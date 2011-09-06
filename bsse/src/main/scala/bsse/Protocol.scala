package bsse

import scala.collection.mutable.HashMap

import roboliq.common
import roboliq.common._
//import roboliq.commands.pipette._
import roboliq.protocol.CommonProtocol
import roboliq.devices.pipette._
import evoware._

trait Protocol extends CommonProtocol with PipetteCommands {
	class EvowareLab {
		private val mapPlates = new HashMap[evoware.PlateObj, common.Plate]
		private val mapPlateModels = new HashMap[evoware.PlateModel, common.PlateModel]
		
		def reagent(liquid: Liquid, plateE: PlateObj, iWell0: Int, nWells: Int = 1) {
			val plate = getPlate(plateE)
			val plateSetup = kb.getPlateSetup(plate)
			val wells = plateSetup.dim_?.get.wells
			val range = (iWell0 - 1) until (iWell0 - 1 + nWells)
			for (iWell <- range) {
				val well = wells(iWell)
				val wellSetup = kb.getWellSetup(well)
				wellSetup.liquid_? = Some(liquid)
			}
		}
		
		def labware(plate: common.Plate, site: SiteObj, model: evoware.PlateModel) {
			val setup = kb.getPlateSetup(plate)
			val proxy = new PlateProxy(kb, plate)
			setup.model_? = Some(getPlateModel(model))
			proxy.location = site.sName
			proxy.setDimension(model.nRows, model.nCols)
		}
		
		private def getPlate(e: PlateObj): common.Plate = {
			mapPlates.get(e) match {
				case Some(plate) => plate
				case None =>
					val plate = new Plate(PlateFamily.Standard)
					val setup = kb.getPlateSetup(plate)
					val proxy = new PlateProxy(kb, plate)
					setup.model_? = Some(getPlateModel(e.model))
					proxy.label = e.sLabel
					proxy.location = e.site.sName
					proxy.setDimension(e.model.nRows, e.model.nCols)
					mapPlates(e) = plate
					plate
			}
		}
		
		private def getPlateModel(e: evoware.PlateModel): common.PlateModel = {
			mapPlateModels.getOrElseUpdate(e, new common.PlateModel(e.sName, e.nRows, e.nCols, e.nVolume))
		}
	}
}
