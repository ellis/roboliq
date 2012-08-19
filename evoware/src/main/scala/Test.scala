//package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet
import scala.reflect.BeanProperty

import roboliq.core._
import roboliq.devices.pipette._
import roboliq.commands.pipette._
import roboliq.robots.evoware._


class TestPipetteDevice extends PipetteDevice {
	//@BeanProperty var tipModels: java.util.LinkedHashMap[String, TipModelBean] = null
	var tipModels: List[TipModel] = Nil
	var tips = SortedSet[Tip]()
	
	def setObjBase(ob: ObjBase): Result[Unit] = {
		for {
			tipModels <- ob.findAllTipModels()
			tips <- ob.findAllTips()
		} yield {
			this.tipModels = tipModels
			this.tips = SortedSet(tips : _*)
		}
	}
	
	def getTipModels = tipModels
	def getTips = tips
	
	def supportTipModelCounts(tipModelCounts: Map[TipModel, Int]): Result[Boolean] =
		Success(tipModelCounts.values.forall(_ <= 4))

	def assignTips(tipsFree: SortedSet[Tip], tipModel: TipModel, nTips: Int): Result[SortedSet[Tip]] =
		Success(tipsFree.take(nTips))
	
	def areTipsDisposable: Boolean =
		false
	
	def getDispenseAllowableTipModels(liquid: Liquid, nVolume: LiquidVolume): Seq[TipModel] =
		tipModels
	
	def getTipAspirateVolumeMin(tip: TipState, liquid: Liquid): LiquidVolume =
		tip.model_? match {
			case None => LiquidVolume.empty
			case Some(model) => model.nVolumeAspirateMin
		}
	
	def getTipHoldVolumeMax(tip: TipState, liquid: Liquid): LiquidVolume =
		tip.model_? match {
			case None => LiquidVolume.empty
			case Some(model) => model.nVolume
		}
	
	def getAspiratePolicy(tipState: TipState, nVolume: LiquidVolume, wellState: WellState): Option[PipettePolicy] =
		Some(new PipettePolicy("POLICY", PipettePosition.Free))
	
	def getDispensePolicy(liquid: Liquid, tipModel: TipModel, nVolume: LiquidVolume, wellState: WellState): Option[PipettePolicy] =
		Some(new PipettePolicy("POLICY", PipettePosition.Free))

	def getMixSpec(tipState: TipState, wellState: WellState, mixSpec_? : Option[MixSpec]): Result[MixSpec] =
		Success(new MixSpec(Some(wellState.nVolume * 0.7), Some(4), None))
	
	def canBatchSpirateItems(states: StateMap, lTwvp: List[TipWellVolumePolicy]): Boolean =
		true
	
	def canBatchMixItems(states: StateMap, lTwvp: List[TipWellMix]): Boolean =
		true
	
	def getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll: SortedSet[Tip], lTipCleaning: SortedSet[Tip]): SortedSet[Tip] =
		lTipAll
	
	def batchCleanTips(lTipAll: SortedSet[Tip]): Seq[SortedSet[Tip]] =
		List(lTipAll)
	
	def batchCleanSpecs(lTipAll: SortedSet[Tip], mTipToCleanSpec: Map[Tip, WashSpec]): Seq[Tuple2[WashSpec, SortedSet[Tip]]] =
		mTipToCleanSpec.toList.groupBy(_._2).mapValues(l => SortedSet(l.map(_._1) : _*)).toList
}

class TestEvowareTable(configFile: EvowareConfigFile, sFilename: String) extends EvowareTable(configFile, sFilename) {
	object Locations {
		val List(reagents15, reagents50) = labelSites(List("reagents15", "reagents50"), "Cooled 8Pos*15ml 8Pos*50ml")
		val ethanol = labelSite("ethanol", "Trough 1000ml", 0)
		val holder = labelSite("holder", "Downholder", 0)
		val List(cover, shaker) = labelSites(List("cover", "shaker"), "MP 2Pos H+P Shake")
		val eppendorfs = labelSite("eppendorfs", "Block 20Pos", 0)
		val List(cooled1, cooled2) = labelSites(List("cooled1", "cooled2"), "MP 3Pos Cooled 1 PCR")
		val List(cooled3, cooled4, cooled5) = labelSites(List("cooled3", "cooled4", "cooled5"), "MP 3Pos Cooled 2 PCR")
		//val (filter1, filter2) = createSites(Carriers.filters, "filter1", "filter2", Seq(0, 1))
		val waste = labelSite("waste", "Te-VacS", 6)
		val sealer = labelSite("sealer", "RoboSeal", 0)
		val peeler = labelSite("peeler", "RoboPeel", 0)
		val pcr1 = labelSite("pcr1", "TRobot1", 0)
		val pcr2 = labelSite("pcr2", "TRobot2", 0)
		val centrifuge = labelSite("centrifuge", "Centrifuge", 0)
		val regrip = labelSite("regrip", "ReGrip Station", 0)
	}
	// HACK: force evaluation of Locations
	Locations.toString()
}

class YamlTest2 {
	import org.yaml.snakeyaml._
	import roboliq.yaml.RoboliqYaml
	
	val sHome = System.getProperty("user.home")
	val pathbase = sHome+"/src/roboliq/testdata/"
	
	val beans = List(
		"classes-bsse1-20120408.yaml",
		"robot-bsse1-20120408.yaml",
		"database-001-20120408.yaml",
		"protocol-001-20120408.yaml"
	).map(s => RoboliqYaml.loadFile(pathbase+s))
	
	val bb = new BeanBase
	beans.foreach(bb.addBean)
	val ob = new ObjBase(bb)
	
	val builder = new StateBuilder(ob)
	val processor = Processor(bb, builder.toImmutable)
	val cmds = beans.last.commands.toList
	val res = processor.process(cmds)
	val nodes = res.lNode
	
	val evowareConfigFile = new EvowareConfigFile(pathbase+"tecan-bsse1-20120408/carrier.cfg")
	//val evowareTableFile = EvowareTableParser.parseFile(evowareConfigFile, pathbase+"tecan-bsse1-20120408/bench1.esc")
	val evowareTable = new TestEvowareTable(evowareConfigFile, pathbase+"tecan-bsse1-20120408/bench1.esc")
	val config = new EvowareConfig(evowareTable.tableFile, evowareTable.mapLabelToSite)
	val translator = new EvowareTranslator(config)
	
	def run {
		println(roboliq.yaml.RoboliqYaml.yamlOut.dump(seqAsJavaList(nodes)))
		println(res.locationTracker.map)
		translator.translate(res) match {
			case Error(ls) => ls.foreach(println)
			case Success(tres) =>
				tres.cmds.foreach(println)
		}
	}
}

object Test extends App {
	new YamlTest2().run
}