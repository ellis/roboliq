package roboliq.pipette

case class TipModel()
case class Tip(id: Int)
case class TipState()
object TipState {
	def createEmpty(tip: Tip): TipState = {
		TipState()
	}
}
case class Well(id: String)