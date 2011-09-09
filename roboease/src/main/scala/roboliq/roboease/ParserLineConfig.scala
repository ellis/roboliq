package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._


class ParserLineConfig(shared: ParserSharedData, mapTables: Map[String, Table]) extends ParserBase(shared) {
	import shared._
	
	val cmd0List: Parser[String] = "LIST"~>ident 
	val cmd0Assign: Parser[Unit] = ident ~"="~ floatingPointNumber ^^
				{ case id ~"="~ s => setVar(id, s) }
	val cmds0 = Map[String, Parser[Unit]](
			("TABLE", ident ^^
				{ case id => setTable(id) }),
			("OPTION", ident~opt(word) ^^
				{ case id ~ value => setOption(id, value) }),
			("REAGENT", ident~idPlate~integer~ident~opt(integer) ^^
				{ case id ~ plate ~ iWell ~ lc ~ nWells_? => setReagent(id, plate, iWell, lc, nWells_?) }),
			("LABWARE", ident~ident~string ^^
				{ case id ~ sRack ~ sType => setLabware(id, sRack, sType) })
			)

	private def setTable(id: String) {
		mapTables.get(id) match {
			case None =>
				shared.addError("unknown rack \""+id+"\"")
				return
			case Some(table) =>
				println("table: "+table)
				shared.sHeader = table.sHeader
				shared.mapRacks.clear()
				shared.mapRacks ++= table.racks.map(rack => rack.name -> rack)
				
		}
	}
			
	private def setVar(id: String, s: String) { mapVars(id) = s }
	
	private def setOption(id: String, value: Option[String]) { mapOptions(id) = value.getOrElse(null) }
	
	private def setReagent(id: String, plate: Plate, iWellPlus1: Int, lc: String, nWells_? : Option[Int]) {
		//mapReagents(reagent) = new Reagent(reagent, rack, iWell, nWells_?.getOrElse(1), lc)
		
		// Create liquid with given name
		val reagent = new Reagent
		val reagentSetup = kb.getReagentSetup(reagent)
		reagentSetup.sName_? = Some(id)
		reagentSetup.sFamily_? = Some("ROBOEASE")
		//println("setReagent(): liq = "+liq)
		
		val pc = kb.getPlateSetup(plate)
		val iWell = iWellPlus1 - 1
		val iWellEnd = iWell + (nWells_? match { case Some(n) => n; case None => 1 })
		val wells = pc.dim_?.get.wells.toIndexedSeq.slice(iWell, iWellEnd)
		
		for (well <- wells) {
			kb.addWell(well, true) // Indicate that it's a source
			val wellSetup = kb.getWellSetup(well)
			wellSetup.reagent_? = Some(reagent)
			//wellSetup.nVolume_? = Some(0)
			//println(kb.getWellSetup(well))
		}
		
		shared.mapReagents(id) = reagent
		//shared.lReagentsInWells += (reagent -> wells)
		shared.mapDefaultLiquidClass(id) = lc
	}

	private def setLabware(id: String, sRack: String, sType: String) {
		shared.mapRacks.get(sRack) match {
			case None =>
				shared.addError("unknown rack \""+sRack+"\"")
				return
			case Some(rack) =>
				val labware = Labware(id, sType, rack)
				shared.mapLabware((rack.grid, rack.site)) = labware
				createPlate(id, sRack)
		}
	}
}
