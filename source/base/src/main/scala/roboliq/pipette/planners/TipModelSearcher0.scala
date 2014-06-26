package roboliq.pipette.planners

import ailib.ch03
import ailib.ch03._
import roboliq.core._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsSuccess
import grizzled.slf4j.Logger


class TipModelSearcher0[Item, TipModel <: AnyRef] {
	
	private val logger = Logger[this.type]

	def searchGraph(
		tipModel_l: List[TipModel],
		itemToModels_m: Map[Item, Seq[TipModel]]
	): RqResult[TipModel] = {
		val tipModelToScore_l: List[(TipModel, Int)] = tipModel_l.flatMap(tipModel => {
			val l = itemToModels_m.values.toList.map { l => l.indexOf(tipModel) }
			if (l.exists(_ < 0)) None
			else Some(tipModel -> l.sum)
		})
		//println("tipModelToScore_l:")
		//tipModelToScore_l.foreach(println)
		val sorted = tipModelToScore_l.sortBy(_._2)
		sorted match {
			case Nil => RqError("No tip model found to handle all items")
			case x :: _ => RqSuccess(x._1)
		}
	}
}
