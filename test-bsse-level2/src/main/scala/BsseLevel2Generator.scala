import scala.collection.immutable.SortedSet

import roboliq.parts._
import roboliq.robot._
import roboliq.tokens._
import roboliq.level2.commands._
import roboliq.level2.tokens._
import evoware._
import bsse._


class BsseLevel2Generator {
	class Setup {
		val carrierClean1 = new Carrier
		val carrierClean2 = new Carrier
		val carrier = new Carrier
		val plate1 = new Plate(nRows = 8, nCols = 12)
		val plate2 = new Plate(nRows = 8, nCols = 12)

		val evowareSetupState = new EvowareSetupState(
			grids = Map(
				carrierClean1 -> 1,
				carrierClean2 -> 2,
				carrier -> 17
			)
		)

		val robot = new BsseRobot(evowareSetupState)

		val builder = new RobotStateBuilder(robot.state)
		builder.sites ++= List(
			robot.plateDecon -> new Site(carrierClean2, 0),
			plate1 -> new Site(carrier, 0),
			plate2 -> new Site(carrier, 1)
		)

		robot.state = builder.toImmutable
		val tips = robot.tips

		val translator = new BsseTranslator(robot)
	}

	object Contamination extends Enumeration {
		val NoContamination, AspirationContaminates, DispenseContaminates = Value
	}

	case class Specs(
		nDestRows: Int, nDestCols: Int,
		nSrcRows: Int, nSrcCols: Int,
		nVolume: Double,
		liquidSrc: Liquid,
		liquidDest: Liquid
	)

	val liquidWater = new Liquid(
		sName = "water",
		bWaterFreeDispense = true,
		bRequireDecontamBeforeAspirate = false,
		bCells = false,
		bDna = false,
		bOtherContaminant = false
	)
	val liquidDna1 = new Liquid(
		sName = "dna1",
		bWaterFreeDispense = true,
		bRequireDecontamBeforeAspirate = true,
		bCells = false,
		bDna = true,
		bOtherContaminant = false
	)

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
			Specs(1, 1, 1, 1, 1.0, liquidDna1, Liquid.empty),
			Specs(1, 1, 1, 1, 100.0, liquidDna1, Liquid.empty),
			Specs(8, 4, 1, 1, 1.0, liquidDna1, Liquid.empty),
			Specs(8, 6, 1, 1, 3.0, liquidDna1, Liquid.empty),
			Specs(8, 4, 1, 1, 48.0, liquidDna1, Liquid.empty),
			Specs(8, 4, 1, 1, 240.0, liquidDna1, Liquid.empty),
			Specs(8, 4, 1, 1, 480.0, liquidDna1, Liquid.empty)
		)
		x(specs(2))
		for (spec <- specs) x(spec)
	}

	def x(spec: Specs) {
		import spec._

		val setup = new Setup
		import setup._

		val srcs = getWells(plate1, nSrcRows, nSrcCols)
		val dests = getWells(plate2, nDestRows, nDestCols)

		val builder = new RobotStateBuilder(RobotState.empty)
		builder.sites += (plate1 -> new Site(carrier, 0))
		builder.sites += (plate2 -> new Site(carrier, 1))
		builder.fillWells(plate1.wells, liquidSrc, 50)
		if (liquidDest ne Liquid.empty)
			builder.fillWells(plate2.wells, liquidDest, 50)
		builder.fillWells(robot.plateDecon.wells, liquidWater, 10000)
		robot.state = builder.toImmutable

		val tok = new T2_PipetteLiquid(
				srcs = srcs,
				mapDestAndVolume = dests.map(_ -> nVolume).toMap
		)

		val compiler = new T2_PipetteLiquid_Compiler(tok, robot)

		val sSrc = spec.nSrcRows+"x"+spec.nSrcCols
		val sDest = spec.nDestRows+"x"+spec.nDestCols
		val fmt = new java.text.DecimalFormat("#.##")
		val sVolume = fmt.format(spec.nVolume)
		val sFilename = "test_"+sSrc+"_"+sDest+"_"+sVolume+".esc"
		val s = translator.translateAndSave(compiler.tokens, sFilename)
		println(sSrc+" -> "+sDest+" "+sVolume+"µl")
		println(s)
		println()
		//assert(s == sExpected)
	}

	private def getWells(plate: Plate, nRows: Int, nCols: Int): SortedSet[Well] = {
		val indexes = (0 until nCols).flatMap(iCol => {
			val iCol0 = iCol * plate.nRows
			(iCol0 until iCol0 + nRows)
		}).toSet
		SortedSet[Well]() ++ plate.wells.filter(well => indexes.contains(well.index))
	}

}
