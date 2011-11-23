package examples

import roboliq.common
import roboliq.common.WellIndex
import roboliq.commands.pipette._
import roboliq.labs.bsse.Protocol


class Wells extends roboliq.common.WellPointerVar
class EmptyWells extends Wells

class ProtocolSettings {
	import roboliq.common._
	
	def setLiquid(liquid: Liquid, loc: WellPointer) {
		
	}
	
	def setWells(wells: Wells, loc: WellPointer) {
		
	}
}

class ProtocolX extends Protocol {
	class ThermocycleProgram {
		def apply(w: Wells) {}
	}
	class CentrifugeProgram {
		def apply(w: Wells, balance: Wells) {}
	}
	class PcrMixProgram(
		buffer: Liquid,
		dNTP: Liquid,
		primerF: Liquid,
		primerR: Liquid,
		polymerase: Liquid
	) {
		def apply(source: Wells, target: Wells) {}
	}
	
	def seal(w: Wells) {}
	def balance(reference: Wells, target: Wells, liquid: Liquid) {}
}

//class PresentationScript1(station: roboliq.labs.bsse.station1.StationConfig) extends ProtocolX {
class PresentationScript1 extends ProtocolX {
	// Liquids
	val water      = new Liquid(Properties.Water, CleanPolicy.ThoroughNone)
	val buffer10x  = new Liquid(Properties.Water, CleanPolicy.Thorough)
	val dNTP       = new Liquid(Properties.Water, CleanPolicy.Thorough)
	val primerF    = new Liquid(Properties.Water, CleanPolicy.Decontaminate, Contaminant.DNA)
	val primerR    = new Liquid(Properties.Water, CleanPolicy.Decontaminate, Contaminant.DNA)
	val polymerase = new Liquid(Properties.Glycerol, CleanPolicy.Thorough)
	
	// Wells
	val wellsT = new Wells      // wells containing DNA template
	val wellsP = new EmptyWells // wells for the PCR reaction
	val wellsB = new EmptyWells // balance wells for centrifuge
	
	// Programs for external devices
	val pcrMix = new PcrMixProgram(
		buffer     = buffer10x,
		dNTP       = dNTP,
		primerF    = primerF,
		primerR    = primerR,
		polymerase = polymerase
	)
	val thermocycle = new ThermocycleProgram
	val centrifuge = new CentrifugeProgram
	
	// Instructions
	pcrMix(source = wellsT, target = wellsP)
	seal(wellsP)
	thermocycle(wellsP)
	balance(reference = wellsP, target = wellsB, liquid = water)
	seal(wellsB)
	centrifuge(wellsP, balance = wellsB)
}

/*
class PresentationSetup1(station: roboliq.labs.bsse.station1.StationConfig) extends ProtocolSettings {
	import station._
	
	configLiquid(water, station.Labwares.reagents50(1))
}
*/