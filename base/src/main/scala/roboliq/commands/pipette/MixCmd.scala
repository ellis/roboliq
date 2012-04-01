package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class MixCmdBean extends CmdBean(isFinal = true) {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[MixCmdItemBean] = null
	
	def process(ob: ObjBase): Result[Unit] = {
		if (items == null)
			Error("`items` must be set")
		else if (items.isEmpty())
			Error("`items` must not be empty")
		else {
			for {
				items <- Result.mapOver(items.toList) {item => item.toTokenItem(ob)}
			} yield {
				tokens = List(new MixToken(items))
			}
		}
	}

	/*
	override def toDebugString = {
		val wells = items.map(_.well)
		val sTips = TipSet.toDebugString(items.map(_.tip))
		val sVolumes = Printer.getSeqDebugString(items.map(_.getVolume()))
		val sPolicies = Printer.getSeqDebugString(items.map(_.getPolicy()))
		getClass().getSimpleName() + "("+sTips+", "+sVolumes+", "+sPolicies+", "+getWellsDebugString(wells)+")" 
	}
	*/
}

class MixCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	@BeanProperty var location: String = null
	
	
	def toTokenItem(ob: ObjBase): Result[MixTokenItem] = {
		for {
			idTip <- Result.mustBeSet(tip, "tip")
			idWell <- Result.mustBeSet(well, "well")
			idPolicy <- Result.mustBeSet(policy, "policy")
			idLocation <- Result.mustBeSet(location, "location")
			well <- ob.findWell(idWell)
		} yield {
			val iTip = idTip.drop(3).toInt
			new MixTokenItem(iTip, well, idPolicy, idLocation)
		}
	}
}

case class MixToken(
	val items: List[MixTokenItem]
) extends CmdToken

case class MixTokenItem(
	val tip: java.lang.Integer,
	val well: Well,
	val policy: String,
	val location: String
)
