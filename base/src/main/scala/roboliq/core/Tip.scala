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
	val id = "TIP"+(index+1)
	
	def state(states: StateMap): TipState = states.findTipState(id).get
	def stateWriter(builder: StateBuilder): TipStateWriter = new TipStateWriter(this, builder)

	override def compare(that: Tip) = index - that.index
	override def toString = id
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