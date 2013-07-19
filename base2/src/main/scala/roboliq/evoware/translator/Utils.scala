package roboliq.evoware.translator

import scala.collection.mutable
import grizzled.slf4j.Logger
import roboliq.core._
import roboliq.pipette._
import ch.ethz.reactivesim.RsSuccess
import ch.ethz.reactivesim.RsResult


object Utils {
	private val logger = Logger("roboliq.evoware.translator.Utils")
	
	// Test all adjacent items for equidistance
	def equidistant(items: Seq[HasTip with HasWell]): Boolean = {
		val lWellInfo = items.map(_.well).toList
		val l = items zip lWellInfo
		equidistant2(l)
	}
		
	// Test all adjacent items for equidistance
	def equidistant2(tws: Seq[(HasTip, Well)]): Boolean = tws match {
		case Seq() => true
		case Seq(_) => true
		case Seq(a, b, rest @ _*) =>
			equidistant3(a, b) match {
				case false => false
				case true => equidistant2(Seq(b) ++ rest)
			}
	}
	
	// All tip/well pairs are equidistant or all tips are going to the same well
	// Assert that tips are spaced at equal distances to each other as the wells are to each other
	def equidistant3(a: Tuple2[HasTip, Well], b: Tuple2[HasTip, Well]): Boolean = {
		(b._1.tip.row - a._1.tip.row) == (b._2.row - a._2.row) &&
		(b._1.tip.col - a._1.tip.col) == (b._2.col - a._2.col) &&
		(b._2.plate == a._2.plate)
	}
	
	/*
	/**
	 * Encode a list of tips as an integer.
	 */
	def encodeHasTips(list: Iterable[HasTip]): Int =
		list.foldLeft(0) { (sum, x) => sum | (1 << x.tip.index) }
	/**
	 * Encode a list of tips as an integer.
	 */
	def encodeTips(list: Iterable[Tip]): Int =
		list.foldLeft(0) { (sum, tip) => sum | (1 << tip.index) }

	/**
	 * Encode a list of indexes of wells on a plate.
	 */
	def encodeWells(holder: Plate, aiWells: Traversable[Int]): String = {
		encodeIndexes(holder.nRows, holder.nCols, aiWells)
	}

	/**
	 * Encode a list of indexes of wells on a plate.
	 */
	def encodeIndexes(row_n: Int, col_n: Int, i_l: Traversable[Int]): String = {
		//println("encodeWells:", holder.nRows, holder.nCols, aiWells)
		val nWellMaskChars = math.ceil(row_n * col_n / 7.0).asInstanceOf[Int]
		val amWells = new Array[Int](nWellMaskChars)
		for (i <- i_l) {
			val iChar = i / 7;
			val iWell1 = i % 7;
			if (iChar >= amWells.size)
				logger.error(s"encodeIndexes(${row_n}, ${col_n}, ${i_l}): index `$i` exceeds size of holder.")
			else
				amWells(iChar) += 1 << iWell1
		}
		val sWellMask = amWells.map(encode).mkString
		val sPlateMask = Array('0', hex(col_n), '0', hex(row_n)).mkString + sWellMask
		sPlateMask
	}
	*/
}