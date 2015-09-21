package roboliq.core

import scala.reflect.BeanProperty
import RqPimper._


/**
 * YAML JavaBean for [[roboliq.core.MixSpec]].
 */
class MixSpecBean extends Bean {
	/** Volume (in liters) for mixing. */
	@BeanProperty var volume: java.math.BigDecimal = null
	/** Number of times to mix. */
	@BeanProperty var count: java.lang.Integer = null
	/** Name of pipette policy for mixing. */
	@BeanProperty var policy: String = null
}

case class MixSpec(
	val volume: LiquidVolume,
	val count: Integer,
	val mixPolicy: PipettePolicy
)

/**
 * Specification for mixing the contents of a well (i.e. not mixing together, but making the liquid "well-mixed").
 * 
 * @param nVolume_? optional volume of aspiration (default will be about 70%)
 * @param nCount_? optional number of times to mix (default will be about 4)
 * @param mixPolicy_? optional pipette policy
 */
case class MixSpecOpt(
	val nVolume_? : Option[LiquidVolume],
	val nCount_? : Option[Integer],
	val mixPolicy_? : Option[PipettePolicy] = None
) {
	def toMixSpec: RqResult[MixSpec] = {
		for {
			volume <- nVolume_?.asRq("MixSpec requires volume")
			count <- nCount_?.asRq("MixSpec requires volume")
			mixPolicy <- mixPolicy_?.asRq("MixSpec requires volume")
		} yield MixSpec(volume, count, mixPolicy)
	}
		
	/**
	 * Merge two mix specs by using the values of `that` for any empty values of `this`.
	 */
	def +(that: MixSpecOpt): MixSpecOpt = {
		MixSpecOpt(
			if (nVolume_?.isEmpty) that.nVolume_? else nVolume_?,
			if (nCount_?.isEmpty) that.nCount_? else nCount_?,
			if (mixPolicy_?.isEmpty) that.mixPolicy_? else mixPolicy_?
		)
	}
}

object MixSpecOpt {
	/** Convert YAML JavaBean to MixSpec. */
	def fromBean(bean: MixSpecBean): MixSpecOpt = {
		MixSpecOpt(
			nVolume_? = if (bean.volume != null) Some(LiquidVolume.l(bean.volume)) else None,
			nCount_? = if (bean.count != null) Some(bean.count) else None,
			mixPolicy_? = if (bean.policy != null) Some(PipettePolicy.fromName(bean.policy)) else None
		)
	}
	
	/** Convert from MixSpec to a MixSpecBean. */
	def toBean(mixSpec: MixSpecOpt): MixSpecBean = {
		val bean = new MixSpecBean
		bean.volume = mixSpec.nVolume_?.map(_.l.bigDecimal).orNull
		bean.count = mixSpec.nCount_?.orNull
		bean.policy = mixSpec.mixPolicy_?.map(_.id).orNull
		bean
	}
}
