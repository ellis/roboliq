package roboliq.core

import scala.collection.mutable.HashMap
import scala.reflect.BeanProperty

class TipBean extends Bean {
	@BeanProperty var index: java.lang.Integer = null
	@BeanProperty var model: String = null
}

class Tip(
	val index: Int,
	val modelPermanent_? : Option[TipModel]
) extends Ordered[Tip] {
	val id = "TIP"+index
	
	def state(states: StateMap): TipState = states(this.id).asInstanceOf[TipState]
	def stateWriter(builder: StateBuilder): TipStateWriter = new TipStateWriter(this, builder)

	override def compare(that: Tip) = index - that.index
	override def toString = id

	// For use by TipStateWriter
	def createState0(model_? : Option[TipModel]): TipState = {
		new TipState(this, model_?, Liquid.empty, LiquidVolume.l(0), Set(), LiquidVolume.l(0), Set(), Set(), Set(), WashIntensity.None, WashIntensity.None, WashIntensity.None)
	}
}

object Tip {
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
			new Tip(index, modelPermanent_?)
		}
	}
	
	def fromBean(ob: ObjBase, messages: CmdMessageWriter)(bean: TipBean): Result[Tip] = {
		for {
			index <- Result.mustBeSet(bean.index, "index")
		} yield {
			val modelPermanent_? = if (bean.model == null) None else ob.findTipModel_?(bean.model, messages)
			new Tip(index, modelPermanent_?)
		}
	}
}

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