package roboliq.core

import scala.reflect.BeanProperty

class MixSpecBean extends Bean {
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var count: java.lang.Integer = null
	@BeanProperty var policy: String = null
}

case class MixSpec(
	val nVolume_? : Option[java.math.BigDecimal],
	val nCount_? : Option[Int],
	val mixPolicy_? : Option[PipettePolicy] = None
) {
	def +(that: MixSpec): MixSpec = {
		MixSpec(
			if (nVolume_?.isEmpty) that.nVolume_? else nVolume_?,
			if (nCount_?.isEmpty) that.nCount_? else nCount_?,
			if (mixPolicy_?.isEmpty) that.mixPolicy_? else mixPolicy_?
		)
	}
	/*
	def toL2(): Result[MixSpec] = {
		for {
			nVolume <- Result.get(nVolume_?, "need to specify volume for mix")
			nCount <- Result.get(nCount_?, "need to specify repetitions for mix")
			mixPolicy <- Result.get(mixPolicy_?, "need to specify pipettet policy for mix")
		} yield MixSpecL2(nVolume, nCount, mixPolicy)
	}
	*/
}

object MixSpec {
	def fromBean(bean: MixSpecBean): MixSpec = {
		MixSpec(
			nVolume_? = if (bean.volume != null) Some(bean.volume) else None,
			nCount_? = if (bean.count != null) Some(bean.count) else None,
			mixPolicy_? = if (bean.policy != null) Some(PipettePolicy.fromName(bean.policy)) else None
		)
	}
}