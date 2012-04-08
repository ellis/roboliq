//package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.collection.immutable.SortedSet
import scala.reflect.BeanProperty

import roboliq.core._
import roboliq.devices.pipette._
import roboliq.commands.pipette._


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
	
	def getDispenseAllowableTipModels(liquid: Liquid, nVolume: LiquidVolume, nVolumeDest: LiquidVolume): Seq[TipModel] =
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
	
	def getDispensePolicy(liquid: Liquid, tip: Tip, nVolume: LiquidVolume, wellState: WellState): Option[PipettePolicy] =
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

class YamlTest2 {
	import org.yaml.snakeyaml._
	
	val beanA = roboliq.yaml.RoboliqYaml.loadFile("example2a.yaml")
	val beanB = roboliq.yaml.RoboliqYaml.loadFile("example2b.yaml")
	//val text = roboliq.yaml.RoboliqYaml.toString(bean)
	
	val bb = new BeanBase
	bb.addBean(beanA)
	bb.addBean(beanB)
	val ob = new ObjBase(bb)
	
	val builder = new StateBuilder(ob)
	val processor = Processor(bb, builder.toImmutable)
	val cmds = beanB.commands.toList
	val res = processor.process(cmds)
	val nodes = res.lNode
	
	def run {
		println(roboliq.yaml.RoboliqYaml.yamlOut.dump(seqAsJavaList(nodes)))
		println(res.locationTracker.map)
	}
}

object Test extends App {
	new YamlTest2().run
}