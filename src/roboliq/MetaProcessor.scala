package roboliq

import meta._


class State {
	val tipContaminations = new Array[Int](8)
}

class MetaProcessor {
	//def translate(in: List[meta.MetaToken]) {
	//	val out = in.map(tr).flatten
	//	println(out.mkString("\n"))
	//}
	def scan(in: List[meta.MetaToken], state: State) {
		in.zipWithIndex.map(pair => scan(pair._1, pair._2))
	}
	
	def scan(tok: meta.MetaToken, iStep: Int): Unit = {
		tok match {
			case t @ meta.CopyPlate(_, _, _, _, _) =>
				/* Attributes needed on plates:
				 * location, rows, cols
				 */
				need(t.source, meta.AttributeKind.Parent, iStep) 
				need(t.source, meta.AttributeKind.Rows, iStep)
				need(t.source, meta.AttributeKind.Cols, iStep)
				
				/* Choose appropriate locations for the plates if necessary (correct form, pipettable, and perhaps cooled)! 
				 */
		}
	}
	
	private def need(obj: MetaObject, kind: AttributeKind.Value, iStep: Int): Boolean = {
		val b = obj.getAttribute(kind, iStep).isDefined
		if (!b) {
			println(kind.toString + " " + iStep)
		}
		b
	}
	
	def tr(tok: meta.MetaToken, iStep: Int): List[concrete.Token] = {
		tok match {
			case t @ meta.CopyPlate(_, _, _, _, _) =>
				/* Need to know whether tips will be clean between aspirations.  This involves knowing:
				 * - How clean does the source well need to remain?
				 * - How contaminated are the tips before the first aspiration?
				 * - Does the dispense involve entering a contaminating liquid?
				 */
				
				/* Choose appropriate locations for the plates if necessary (correct form, pipettable, and perhaps cooled)! 
				 */
				
				// Decide on pipetting pattern, clean between aspirations
		}
	}
}