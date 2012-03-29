package roboliq.roboease

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.common.{Error => RError, Success => RSuccess}
import roboliq.commands.pipette._


class ParserLineConfig(shared: ParserSharedData, mapTables: Map[String, Table]) extends ParserBase(shared) {
	import shared._
	
	val cmd0List: Parser[String] = "LIST"~>ident 
	val cmd0Assign: Parser[Unit] = ident ~"="~ floatingPointNumber ^^
				{ case id ~"="~ s => setVar(id, s) }
	val cmds0 = Map[String, Parser[Result[Unit]]](
			("LABWARE", ident~idRack~string ^^
				{ case id ~ rack ~ sType => setLabware(id, rack, sType) }),
			("MIXDEF", idReagent~rep1(idReagent~valVolume) ^^
				//{ case id ~ List[~[String, Double]] => () }),
				{ case reagent ~ l => setMixDef(reagent, l.map(x => x._1 -> x._2)) }),
			("OPTION", ident~opt(word) ^^
				{ case id ~ value => setOption(id, value) }),
			("REAGENT", ident~idPlate~integer~ident~opt(integer) ^^
				{ case id ~ plate ~ iWell ~ lc ~ nWells_? => setReagent(id, plate, iWell, lc, nWells_?) }),
			("TABLE", ident ^^
				{ case id => setTable(id) })
			)

	private def setLabware(id: String, rack: Rack, sType: String): Result[Unit] = {
		val labware = Labware(id, sType, rack)
		shared.mapLabware((rack.grid, rack.site)) = labware
		for { _ <- createPlate(id, rack, Some(sType)) }
		yield ()
		//println("ADDED LABWARE: "+labware)
		//RSuccess()
	}
	
	private def setMixDef(reagent: Reagent, l: List[Tuple2[Reagent, Double]]): Result[Unit] = {
		shared.mapMixDefs(reagent.id) = new MixDef(reagent, l)
		RSuccess()
	}
	
	private def setOption(id: String, value: Option[String]): Result[Unit] = {
		mapOptions(id) = value.getOrElse(null)
		RSuccess()
	}
	
	def setReagent(id: String, plate: PlateObj, iWellPlus1: Int, lc: String, nWells_? : Option[Int]): Result[Unit] = {
		//println("ADDIND REAGENT: "+id)
		//println("shared.getPipettePolicy(lc): "+shared.getPipettePolicy(lc))
		for {
			policy_? <- if (lc == "DEFAULT") RSuccess(None) else shared.getPipettePolicy(lc).map(Some(_))
			dim <- shared.getDim(plate)
		} yield {
			//mapReagents(reagent) = new Reagent(reagent, rack, iWell, nWells_?.getOrElse(1), lc)
			
			val iWell = iWellPlus1 - 1
			val iWellEnd = iWell + (nWells_? match { case Some(n) => n; case None => 1 })
			val wells = dim.wells.toIndexedSeq.slice(iWell, iWellEnd)
			
			// Create liquid with given name
			val reagent = new Reagent(id, new roboliq.common.Reagent, wells, policy_?)
			val reagentSetup = kb.getReagentSetup(reagent.reagent)
			reagentSetup.sName_? = Some(id)
			reagentSetup.sFamily_? = Some(lc)
			
			for (well <- wells) {
				kb.addWell(well, true) // Indicate that it's a source
				val wellSetup = kb.getWellSetup(well)
				wellSetup.reagent_? = Some(reagent.reagent)
				//wellSetup.nVolume_? = Some(0)
				//println(kb.getWellSetup(well))
			}
			
			//println("ADDED REAGENT: "+reagent)
			
			shared.mapReagents(id) = reagent
			//shared.lReagentsInWells += (reagent -> wells)
			//shared.mapReagentToPolicy(reagent) = policy
		}
	}

	private def setTable(id: String): Result[Unit] = {
		mapTables.get(id) match {
			case None =>
				RError("unknown table \""+id+"\"")
			case Some(table) =>
				shared.sTable = id
				shared.sHeader = table.sHeader
				shared.mapRacks.clear()
				shared.mapRacks ++= table.racks.map(rack => rack.id -> rack)
				RSuccess()
		}
	}
			
	private def setVar(id: String, s: String) { mapVars(id) = s }
}
