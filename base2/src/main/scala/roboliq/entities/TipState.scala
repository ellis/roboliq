package roboliq.entities

import scala.language.implicitConversions
import scala.collection.mutable.HashMap


/**
 * State of [[roboliq.core.Tip]].
 * Since [[roboliq.core.Tip]] is used to represent both a synringe and a tip,
 * this class combines state information for both the syringe and its possibly
 * attached tip.
 * 
 * @param conf the syringe/tip.
 * @param model_? optional tip model on this syringe.
 * @param src_? source well from which liquid was aspirated.
 * @param liquid liquid in the tip.
 * @param nVolume volume of liquid in the tip.
 * @param cleanDegreePrev intensity of most recent cleaning
 */
case class TipState(
	val conf: Tip, // REFACTOR: rename `conf` to `tip`
	val model_? : Option[TipModel],
	val src_? : Option[Well],
	val content: Aliquot,
	val contamInside: Set[String], 
	val nContamInsideVolume: LiquidVolume,
	val contamOutside: Set[String],
	val srcsEntered: Set[Liquid],
	val destsEntered: Set[Liquid],
	val cleanDegree: CleanIntensity.Value,
	val cleanDegreePrev: CleanIntensity.Value,
	/** Intensity of cleaning that should be performed after leaving the current liquid group */
	val cleanDegreePending: CleanIntensity.Value
) extends Ordered[TipState] {
	def id = conf.key
	def index = conf.index
	def row = conf.row
	def col = conf.col
	def permanent_? = conf.permanent_?
	
	override def compare(that: TipState): Int = conf.compare(that.conf)
	override def toString =
		s"TipState(${conf.key}, ${model_?.map(_.id).getOrElse("")}, $content, $cleanDegree, $cleanDegreePending)"
}

/** Factory object for [[roboliq.core.TipState]]. */
object TipState {
	/** Create an initial state for `tip` with no liquid in it. */
	def createEmpty(tip: Tip) = TipState(
		conf = tip,
		model_? = tip.permanent_?,
		src_? = None,
		content = Aliquot.empty,
		contamInside = Set(),
		nContamInsideVolume = LiquidVolume.empty,
		contamOutside = Set(),
		srcsEntered = Set(),
		destsEntered = Set(),
		cleanDegree = CleanIntensity.None,
		cleanDegreePrev = CleanIntensity.None,
		cleanDegreePending = CleanIntensity.None
	)
	
	implicit def toTip(o: TipState): Tip = o.conf
}
