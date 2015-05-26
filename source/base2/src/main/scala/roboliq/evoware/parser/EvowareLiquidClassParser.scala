package roboliq.evoware.parser

import scala.io.Source
import scala.util.Try
import scala.collection.mutable.ArrayBuffer
import roboliq.core._
import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsSuccess
import roboliq.entities.PipettePolicy
import roboliq.entities.PipettePosition

object EvowareLiquidClassParser {
	val RxLiquidClass = """^  <LiquidClass name="([^"]+)".*""".r
	val RxDispense = """^        <Dispense>""".r
	val RxLLD = """^          <LLD detect="([^"]+)" position="([0-9]+)".*""".r
	
	def parseFile(filename: String): RsResult[List[PipettePolicy]] = {
		Try {
			val source = Source.fromFile(filename)
			var liquidClass_s = ""
			var bLiquidClass = false
			var bDispense = false
			val policy_l = new ArrayBuffer[PipettePolicy]
			for (line <- source.getLines) {
				line match {
					case RxLiquidClass(name) =>
						if (!name.startsWith("&gt")) {
							liquidClass_s = name
							bLiquidClass = true
						}
					case RxDispense() if bLiquidClass =>
						bLiquidClass = false
						bDispense = true
					case RxLLD(detect, pos_s) if bDispense =>
						val pos = (detect, pos_s) match {
							case ("True", _) => PipettePosition.WetContact
							case (_, "6") => PipettePosition.WetContact
							case (_, "7") => PipettePosition.WetContact
							case _ => PipettePosition.Free
						}
						policy_l += new PipettePolicy(liquidClass_s, pos)
						bDispense = false
					case _ =>
				}
			}
			RsSuccess(policy_l.toList)
		}
	}
}