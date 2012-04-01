package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

import roboliq.core._


class PipetteCmdBean extends CmdBean(isFinal = false) {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[PipetteCmdItemBean] = null
	
	def process(ob: ObjBase): Result[Unit] = {
		if (items == null)
			Error("`items` must be set")
		else if (items.isEmpty())
			Error("`items` must not be empty")
		else {
			for {
				items <- Result.mapOver(items.toList) {item => item.toTokenItem(ob)}
			} yield {
				List(new PipetteToken(items))
			}
		}
	}
}

class PipetteCmdItemBean {
	@BeanProperty var src: String = null
	@BeanProperty var dest: String = null
	@BeanProperty var volume: List[java.math.BigDecimal] = null
	//val premix_? : Option[MixSpec],
	//val postmix_? : Option[MixSpec]
	//tipOverrides
	@BeanProperty var policy: String = null
	@BeanProperty var tipModel: String = null
	
	
	def toTokenItem(ob: ObjBase): Result[PipetteTokenItem] = {
		for {
			idTip <- Result.mustBeSet(tip, "tip")
			idWell <- Result.mustBeSet(well, "well")
			idPolicy <- Result.mustBeSet(policy, "policy")
			idLocation <- Result.mustBeSet(location, "location")
			well <- ob.findWell(idWell)
		} yield {
			val iTip = idTip.drop(3).toInt
			new PipetteTokenItem(iTip, well, idPolicy, idLocation)
		}
	}
}
