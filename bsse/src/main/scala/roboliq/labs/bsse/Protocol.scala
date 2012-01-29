package roboliq.labs.bsse

import scala.collection.mutable.HashMap

import roboliq.common
import roboliq.common._
import roboliq.protocol.CommonProtocol
import roboliq.devices.pipette._
import roboliq.robots.evoware
import roboliq.robots.evoware._


trait Protocol extends CommonProtocol {
	val tableFile: EvowareTableFile
	class EvowareLab {
		private val mapSiteToPlate = new HashMap[CarrierSite, common.Plate]
		//private val mapPlates = new HashMap[evoware.PlateObj, common.Plate]
		//private val mapPlateModels = new HashMap[evoware.PlateModel, common.PlateModel]
		
		private def getPlate(site: CarrierSite): common.Plate = {
			mapSiteToPlate.get(site) match {
				case Some(plate) => plate
				case None =>
					val plate = new common.Plate
					val setup = plate.setup
					val labwareModel
					val plateModel = new PlateModel(val id: String, val nRows: Int, val nCols: Int, val nWellVolume: Double)
					setup.model_? = Some(model)
					setup.sLabel_? = Some(sLabel)
					setup.location_? = Some(site.sName)
					setup.setDimension(model.nRows, model.nCols)
					plate
			}
		}
		
		def reagent(reagent: Reagent, site: CarrierSite, iWell0: Int, nWells: Int = 1) {
			val plate = plateE.commonObj
			val plateSetup = kb.getPlateSetup(plate)
			val wells = plateSetup.dim_?.get.wells
			val range = (iWell0 - 1) until (iWell0 - 1 + nWells)
			for (iWell <- range) {
				val well = wells(iWell)
				val wellSetup = kb.getWellSetup(well)
				wellSetup.reagent_? = Some(reagent)
			}
		}
		
		/*
		def labware(plate: common.Plate, site: SiteObj, model: common.PlateModel) {
			val setup = kb.getPlateSetup(plate)
			val proxy = new PlateProxy(kb, plate)
			setup.model_? = Some(model)
			proxy.location = site.sName
			proxy.setDimension(model.nRows, model.nCols)
		}
		*/
		
		/*private def getPlate(e: PlateObj): common.Plate = {
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
		}*/
		
		/*private def getPlateModel(e: evoware.PlateModel): common.PlateModel = {
			mapPlateModels.getOrElseUpdate(e, new common.PlateModel(e.sName, e.nRows, e.nCols, e.nVolume))
		}*/
	}
}
