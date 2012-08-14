package roboliq.core

import scala.reflect.BeanProperty


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

/**
 * Specification for mixing the contents of a well (i.e. not mixing together, but making the liquid "well-mixed").
 * 
 * @param nVolume_? optional volume of aspiration (default will be about 70%)
 * @param nCount_? optional number of times to mix (default will be about 4)
 * @param mixPolicy_? optional pipette policy
 */
case class MixSpec(
	val nVolume_? : Option[LiquidVolume],
	val nCount_? : Option[Integer],
	val mixPolicy_? : Option[PipettePolicy] = None
) {
	/**
	 * Merge two mix specs by using the values of `that` for any empty values of `this`.
	 */
	def +(that: MixSpec): MixSpec = {
		MixSpec(
			if (nVolume_?.isEmpty) that.nVolume_? else nVolume_?,
			if (nCount_?.isEmpty) that.nCount_? else nCount_?,
			if (mixPolicy_?.isEmpty) that.mixPolicy_? else mixPolicy_?
		)
	}
}

object MixSpec {
	/** Convert YAML JavaBean to MixSpec. */
	def fromBean(bean: MixSpecBean): MixSpec = {
		MixSpec(
			nVolume_? = if (bean.volume != null) Some(LiquidVolume.l(bean.volume)) else None,
			nCount_? = if (bean.count != null) Some(bean.count) else None,
			mixPolicy_? = if (bean.policy != null) Some(PipettePolicy.fromName(bean.policy)) else None
		)
	}
	
	/** Convert from MixSpec to a MixSpecBean. */
	def toBean(mixSpec: MixSpec): MixSpecBean = {
		val bean = new MixSpecBean
		bean.volume = mixSpec.nVolume_?.map(_.l.bigDecimal).orNull
		bean.count = mixSpec.nCount_?.orNull
		bean.policy = mixSpec.mixPolicy_?.map(_.id).orNull
		bean
	}
}
