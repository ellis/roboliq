package roboliq.tokens

import roboliq.entities._

/*import roboliq.pipette.TipWellVolumePolicy
import roboliq.pipette.Plate
import roboliq.pipette.PlateLocation*/

package control {
	case class CommentToken(
		text: String
	) extends Token
	
	case class PromptToken(
		text: String
	) extends Token
	
	package low {
		case class CallToken(
			text: String
		) extends Token

		case class ExecToken(
			text: String,
			waitTillDone: Boolean,
			checkResult: Boolean
		) extends Token
	}
}

package pipette {
	package low {
		case class AspirateToken(
			val items: List[TipWellVolumePolicy]
		) extends Token
		
		case class DispenseToken(
			val items: List[TipWellVolumePolicy]
		) extends Token
	}
}

package transport {
	case class EvowareTransporterRunToken(
		val roma_i: Int,
		val vectorClass: String,
		val model: roboliq.evoware.parser.EvowareLabwareModel,
		val origin: roboliq.evoware.parser.CarrierSite,
		val destination: roboliq.evoware.parser.CarrierSite
	) extends Token
}