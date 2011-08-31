package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._


class ParserLineConfig(shared: ParserSharedData) extends ParserBase(shared) {
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
			("LABWARE", ident~ident~stringLiteral ^^
				{ case id ~ sRack ~ sType => setLabware(id, sRack, sType) })
			)

	private def setTable(id: String) {
		println("WARNING: TABLE directive not yet implemented")
	}
			
	private def setVar(id: String, s: String) { mapVars(id) = s }
	
	private def setOption(id: String, value: Option[String]) { mapOptions(id) = value.getOrElse(null) }
	
	private def setReagent(id: String, plate: Plate, iWellPlus1: Int, lc: String, nWells_? : Option[Int]) {
		//mapReagents(reagent) = new Reagent(reagent, rack, iWell, nWells_?.getOrElse(1), lc)
		
		// Create liquid with given name
		val liq = new Liquid(id, true, false, Set())
		kb.addLiquid(liq)
		//println("setReagent(): liq = "+liq)
		
		// Add liquid to wells
		val pc = kb.getPlateSetup(plate)
		val iWell = iWellPlus1 - 1
		val iWellEnd = iWell + (nWells_? match { case Some(n) => n; case None => 1 })
		val wells = pc.dim_?.get.wells.toIndexedSeq
		for (iWell <- (iWell until iWellEnd)) {
			val well = wells(iWell)
			kb.addWell(well, true) // Indicate that it's a source
			kb.getWellSetup(well).liquid_? = Some(liq)
			//println(kb.getWellSetup(well))
		}
			
		mapLiquids(id) = liq
		mapDefaultLiquidClass(liq) = lc
	}

	private def setLabware(id: String, sRack: String, sType: String) {
		createPlate(id, sRack)
	}
}
