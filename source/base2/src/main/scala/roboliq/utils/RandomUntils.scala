package roboliq.utils

import scala.util.Random

object RandomUntils {
	def randomize384Wells(seed: Int): List[String] =
		randomize384Wells(new Random(seed))

	// Assumes we will use four tips
	def randomize384Wells(r: Random): List[String] = {
		// Create a 4x24 list of permutations of the four columns/tips
		// I think of this as representing rows A-D and all 24 columns
		val ll0 = List.range(0, 4).permutations.toList
		// Shuffle the permutations to fill 384 wells
		val ll1 = List.fill(4)(r.shuffle(ll0)).flatten
		val ll2 = ll1.zipWithIndex.map { case (l, i) =>
			val col = i % 24
			val row0 = (i / 24) * 4
			val row0_c = 'A' + row0
			l.map(offset => (row0_c + offset).asInstanceOf[Char] + f"${col+1}%02d")
		}
		val ll3 = ll2.transpose
		val ll4 = ll3.map(l => r.shuffle(l))
		ll4.transpose.flatten
	}
}