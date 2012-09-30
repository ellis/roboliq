package roboliq.commands.arm

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class MovePlateCmdBean extends CmdBean {
	@BeanProperty var deviceId: String = null
	@BeanProperty var plate: String = null
	@BeanProperty var plateDest: String = null
	//@BeanProperty var locationLid: String = null
	//@BeanProperty var lidHandling: LidHandling.Value
}

class MovePlateToken(
	val deviceId_? : Option[String],
	val plate: Plate,
	val plateDest: PlateLocation
) extends CmdToken

object MovePlateToken {
	def fromBean(bean: MovePlateCmdBean, ob: ObjBase): Result[MovePlateToken] = {
		for {
			plate <- ob.findPlate(bean.plate)
			plateDest <- ob.findPlateLocation(bean.plateDest)
		} yield {
			new MovePlateToken(
				Option(bean.deviceId),
				plate,
				plateDest)
		}
	}
}

class MovePlateCmdHandler extends CmdHandlerA[MovePlateCmdBean] {
	def expand1A(cmd: CmdType, messages: CmdMessageWriter): Expand1Result = {
		messages.paramMustBeNonEmpty("plate")
		messages.paramMustBeNonEmpty("plateDest")
		if (messages.hasErrors)
			return Expand1Errors()
		
		// Item wells are sources
		Expand1Resources(
			List(NeedPlate(cmd.plate))
		)
	}

	def expand2A(
		cmd: CmdType,
		ctx: ProcessorContext,
		messages: CmdMessageWriter
	): Expand2Result = {
		MovePlateToken.fromBean(cmd, ctx.ob) match {
			case Error(ls) => ls.foreach(messages.addError); Expand2Errors()
			case Success(token) =>
				val event = PlateLocationEventBean(token.plateDest.id)
				val doc = s"Move plate `${token.plate.id}` to location `${token.plateDest.id}`"
				
				Expand2Tokens(List(token), List(event), doc)
		}
	}
}
