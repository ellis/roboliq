package roboliq.core

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty

/** YAML JavaBean representation of [[roboliq.core.Tip]]. */
class TipBean extends Bean {
	@BeanProperty var index: java.lang.Integer = null
	@BeanProperty var model: String = null
}

/**
 * Represents a tip/syringe for pipetting.
 * More precisely, this represents a syringe which may or may not have a tip on it.
 * Whether there is actually a tip on the syringe is indicated by [[roboliq.core.TipState]].
 * 
 * @see [[roboliq.core.TipModel]]
 * @see [[roboliq.core.TipState]]
 * 
 * @param index unique internal index of this tip.
 * @param modelPermanent_? optional tip model if this tip is permanent.
 */
// TODO: add deviceId_? and make id a parameter rather than automatically generating it.
case class Tip(
	val id: String,
	val index: Int,
	val permanent_? : Option[TipModel]
) extends Ordered[Tip] {
	//	Tip("TIP)
	
	def state(states: StateMap): TipState = states.findTipState(id).get
	def stateWriter(builder: StateBuilder): TipStateWriter = new TipStateWriter(this, builder)

	override def compare(that: Tip) = index - that.index
	override def toString = id
}

object Tip {
	def apply(index: Int, permanent_? : Option[TipModel] = None) = new Tip("TIP"+(index+1), index, permanent_?)
	
	/** Convert [[roboliq.core.TipBean]] to [[roboliq.core.Tip]]. */
	def fromBean(ob: ObjBase)(bean: TipBean): Result[Tip] = {
		for {
			index <- Result.mustBeSet(bean.index, "index")
		} yield {
			val modelPermanent_? = {
				if (bean.model == null)
					None
				else {
					ob.findTipModel(bean.model) match {
						case Error(ls) => return Error(ls)
						case Success(model) => Some(model)
					}
				}
			}
			Tip(index, modelPermanent_?)
		}
	}
	
	/** Convert [[roboliq.core.TipBean]] to [[roboliq.core.Tip]]. */
	def fromBean(ob: ObjBase, messages: CmdMessageWriter)(bean: TipBean): Result[Tip] = {
		for {
			index <- Result.mustBeSet(bean.index, "index")
		} yield {
			val modelPermanent_? = if (bean.model == null) None else ob.findTipModel_?(bean.model, messages)
			Tip(index, modelPermanent_?)
		}
	}
}

/** Convenience class for making debug strings from sets of tips. */
object TipSet {
	def toDebugString(set: Set[Tip]): String = toDebugString(collection.immutable.SortedSet(set.toSeq : _*))
	def toDebugString(set: collection.immutable.SortedSet[Tip]): String = toDebugString(set.toSeq)
	def toDebugString(seq: Seq[Tip]): String = {
		if (seq.isEmpty)
			"NoTips"
		else {
			var indexPrev = -1
			var indexLast = -1
			val l = seq.foldLeft(Nil: List[Tuple2[Int, Int]]) { (acc, tip) => {
				acc match {
					case Nil => List((tip.index, tip.index))
					case first :: rest =>
						if (tip.index == first._2 + 1)
							(first._1, tip.index) :: rest
						else
							(tip.index, tip.index) :: acc
				}
			}}
			val ls = l.reverse.map(pair => {
				if (pair._1 == pair._2) (pair._1 + 1).toString
				else (pair._1 + 1) + "-" + (pair._2 + 1)
			})
			ls.mkString("Tip", ",", "")
		}
	}
}