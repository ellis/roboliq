package roboliq

import roboliq.commands._
import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._
import evoware._


object Tests {
	class Setup {
		val tips = Array(
			new Tip(0, 2, 950), new Tip(1, 2, 950), new Tip(2, 2, 950), new Tip(3, 2, 950), 
			new Tip(4, 0, 45), new Tip(5, 0, 45), new Tip(6, 0, 45), new Tip(7, 0, 45) 
		)
		val tipGroups = Array(Array(0, 1, 2, 3), Array(4, 5, 6, 7), Array(0, 1, 2, 3, 4, 5, 6, 7))
		val rule1 = new AspirateStrategy("D-BSSE Te-PS Wet Contact")
		val dispenseEnter = new DispenseStrategy("D-BSSE Te-PS Wet Contact", bEnter = true)
		val dispenseHover = new DispenseStrategy("D-BSSE Te-PS Dry Contact", bEnter = false)
		val carrier = new Carrier
		val plate1 = new Plate(nRows = 8, nCols = 12)
		val plate2 = new Plate(nRows = 8, nCols = 12)
		val liquidDirty1 = new Liquid("dirty1", true)
		val liquidDirty2 = new Liquid("dirty2", true)
		val liquidWater1 = new Liquid("water1", true)
		val liquidWater2 = new Liquid("water2", true)
		
		val robotConfig = new RobotConfig(tips, tipGroups)
		val robot = new EvowareRobot(robotConfig)

		val evowareSettings = new EvowareSettings(Map(
				carrier -> 17
		))
	}
	
	object Contamination extends Enumeration {
		val NoContamination, AspirationContaminates, DispenseContaminates = Value
	}

	case class Specs(
			nDestRows: Int, nDestCols: Int,
			nSrcRows: Int, nSrcCols: Int,
			nVolume: Double,
			contamination: Contamination.Value,
			sExpected_? : Option[String])
			
	def a() {
		// Parameters to vary:
		// source liquid contaminates
		// destination liquid contaminates
		// dispense enters destination liquid
		// source wells: 1x1, 2x1, 4x1, 6x1, 8x1, 1x2, 2x2, 4x4, 96x96
		// destination wells: 1x1, 2x1, 4x1, 6x1, 8x1, 1x2, 2x2, 4x4, 96x96
		// volumes: 1, 3, 10, 100, 500
		//
		// dest contaminates, source contaminates, no contamination
		// most common cases for no contamination:
		//  dest 1x1 src 1x1 volume 1µl (only use the small tips)
		//  dest 1x1 src 1x1 volume 100µl (only use the large tips)
		//  dest 8x4 src 1x1 volume 1µl (only use the small tips)
		//  dest 8x6 src 1x1 volume 3µl (use all 8 tips)
		//  dest 8x4 src 1x1 volume 48µl (4 large tips fill all 96 wells)
		//  dest 8x4 src 1x1 volume 90µl (fill with two cycles)
		//  dest 8x4 src 1x1 volume 180µl (fill with four cycles)
		val specs = List(
Specs(1, 1, 1, 1, 1.0, Contamination.NoContamination, Some(
"""Aspirate(16,"AspirateStrategy1",1,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(16,"Dispense Hover",1,0,0,0,0,0,0,0,0,0,0,0,17,1,1,"0C0810000000000000",0,0);""")),
Specs(1, 1, 1, 1, 100.0, Contamination.NoContamination, Some(
"""Aspirate(1,"AspirateStrategy1",1,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(1,"Dispense Hover",1,0,0,0,0,0,0,0,0,0,0,0,17,1,1,"0C0810000000000000",0,0);""")),
Specs(8, 4, 1, 1, 1.0, Contamination.NoContamination, Some(
"""Aspirate(16,"AspirateStrategy1",8,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(32,"AspirateStrategy1",8,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(64,"AspirateStrategy1",8,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(128,"AspirateStrategy1",8,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C08?0000000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C080N000000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C0800l00000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C08000¬0000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C08Ê1000000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C080?300000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C0800p70000000000",0,0);
Dispense(240,"Dispense Hover",1,1,1,1,0,0,0,0,0,0,0,0,17,1,1,"0C080000?000000000",0,0);""")),
Specs(8, 6, 1, 1, 3.0, Contamination.NoContamination, Some(
"""Aspirate(1,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(16,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(32,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(64,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(128,"AspirateStrategy1",18,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(255,"Dispense Hover",3,3,3,3,3,3,3,3,0,0,0,0,17,1,1,"0C08ø1000000000000",0,0);
Dispense(255,"Dispense Hover",3,3,3,3,3,3,3,3,0,0,0,0,17,1,1,"0C080¨300000000000",0,0);
Dispense(255,"Dispense Hover",3,3,3,3,3,3,3,3,0,0,0,0,17,1,1,"0C0800Â70000000000",0,0);
Dispense(255,"Dispense Hover",3,3,3,3,3,3,3,3,0,0,0,0,17,1,1,"0C08000¬?000000000",0,0);
Dispense(255,"Dispense Hover",3,3,3,3,3,3,3,3,0,0,0,0,17,1,1,"0C080000ÊO00000000",0,0);
Dispense(255,"Dispense Hover",3,3,3,3,3,3,3,3,0,0,0,0,17,1,1,"0C0800000?o0000000",0,0);""")),
Specs(8, 4, 1, 1, 48.0, Contamination.NoContamination, Some(
"""Aspirate(1,"AspirateStrategy1",384,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",384,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",384,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",384,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C08?0000000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C080N000000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C0800l00000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C08000¬0000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C08Ê1000000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C080?300000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C0800p70000000000",0,0);
Dispense(15,"Dispense Hover",48,48,48,48,0,0,0,0,0,0,0,0,17,1,1,"0C080000?000000000",0,0);""")),
Specs(8, 4, 1, 1, 240.0, Contamination.NoContamination, Some(
"""Aspirate(1,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C08?0000000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C080N000000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C0800l00000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C08000¬0000000000",0,0);
Aspirate(1,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C08Ê1000000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C080?300000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C0800p70000000000",0,0);
Dispense(15,"Dispense Hover",240,240,240,240,0,0,0,0,0,0,0,0,17,1,1,"0C080000?000000000",0,0);""")),
Specs(8, 4, 1, 1, 480.0, Contamination.NoContamination, Some(
"""Aspirate(1,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C08?0000000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C080N000000000000",0,0);
Aspirate(1,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C08Ê1000000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C080?300000000000",0,0);
Aspirate(1,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C0800l00000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C08000¬0000000000",0,0);
Aspirate(1,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(2,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(4,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Aspirate(8,"AspirateStrategy1",960,0,0,0,0,0,0,0,0,0,0,0,17,0,1,"0C0810000000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C0800p70000000000",0,0);
Dispense(15,"Dispense Hover",480,480,480,480,0,0,0,0,0,0,0,0,17,1,1,"0C080000?000000000",0,0);"""))
		)
		//x(specs(2))
		for (spec <- specs) x(spec)
	}
	
	def x(spec: Specs) {
		import spec._
		
		val setup = new Setup
		import setup._
		
		val liquidSrc = if (spec.contamination == Contamination.AspirationContaminates) liquidDirty1 else liquidWater1
		val liquidDest = if (spec.contamination == Contamination.DispenseContaminates) liquidDirty2 else liquidWater2
		
		val srcs = getWells(plate1, nSrcRows, nSrcCols)
		val dests = getWells(plate2, nDestRows, nDestCols)
		
		val builder = new RobotStateBuilder(RobotState.empty)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))
		builder.fillWells(plate1.wells, liquidSrc, 50)
		builder.fillWells(plate2.wells, liquidDest, 50)
		robot.state = builder.toImmutable
		
		val p = new PipetteLiquid(
				robot = robot,
				srcs = srcs,
				dests = dests,
				volumes = Array.fill(dests.size) { nVolume },
				aspirateStrategy = rule1,
				dispenseStrategy = if (spec.contamination == Contamination.DispenseContaminates) dispenseEnter else dispenseHover)
		
		val sSrc = spec.nSrcRows+"x"+spec.nSrcCols
		val sDest = spec.nDestRows+"x"+spec.nDestCols
		val fmt = new java.text.DecimalFormat("#.##")
		val sVolume = fmt.format(spec.nVolume)
		val sFilename = "test_"+sSrc+"_"+sDest+"_"+sVolume+".esc"
		val s = EvowareTranslator.translateAndSave(p.tokens, evowareSettings, robot.state, sFilename)
		println(sSrc+" -> "+sDest+" "+sVolume+"µl")
		println(s)
		println()
		//assert(s == sExpected)
	}
	
	def x(bSrcContaminates: Boolean, bDestContaminates: Boolean, bDispenseEnters: Boolean, nSrcRows: Int, nSrcCols: Int, nDestRows: Int, nDestCols: Int, nVolume: Double, sExpected: String) {
		val setup = new Setup
		import setup._
		
		val liquidSrc = if (bSrcContaminates) liquidDirty1 else liquidWater1
		val liquidDest = if (bDestContaminates) liquidDirty2 else liquidWater2
		
		val srcs = getWells(plate1, nSrcRows, nSrcCols)
		val dests = getWells(plate2, nDestRows, nDestCols)
		
		val builder = new RobotStateBuilder(RobotState.empty)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))
		builder.fillWells(plate1.wells, liquidSrc, 50)
		builder.fillWells(plate2.wells, liquidDest, 50)
		robot.state = builder.toImmutable
		
		val p = new PipetteLiquid(
				robot = robot,
				srcs = srcs,
				dests = dests,
				volumes = Array.fill(dests.size) { nVolume },
				aspirateStrategy = rule1,
				dispenseStrategy = if (bDispenseEnters) dispenseEnter else dispenseHover)
		val s = EvowareTranslator.translate(p.tokens, evowareSettings, robot.state)
		println(s)
		println()
		//assert(s == sExpected)
	}
	
	private def getWells(plate: Plate, nRows: Int, nCols: Int): Array[Well] = {
		val indexes = (0 until nCols).flatMap(iCol => {
			val iCol0 = iCol * plate.nRows
			(iCol0 until iCol0 + nRows)
		}).toSet
		plate.wells.filter(well => indexes.contains(well.index)).toArray
	}
	
	private def pipetteLiquid(setup: Setup, nSrcs: Int, nDests: Int, nVolume: Double, dispenseStrategy: DispenseStrategy): String = {
		import setup._
		val p = new PipetteLiquid(
				robot = robot,
				srcs = plate1.wells.take(nSrcs),
				dests = plate2.wells.take(nDests),
				volumes = Array.fill(nDests) { nVolume },
				aspirateStrategy = rule1,
				dispenseStrategy = dispenseStrategy)
		EvowareTranslator.translate(p.tokens, evowareSettings, robot.state)
	}
}
