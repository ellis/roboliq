package aiclass.hw1

import ailib.ch03
import ailib.ch03._


object Problem5 {
	val childrenL = Map(
		0 -> List(1, 2, 3),
		1 -> List(4, 5),
		2 -> List(6, 7),
		3 -> List(8, 9),
		6 -> List(10, 11),
		7 -> List(12)
	)
	val childrenR = childrenL.map(pair => pair._1 -> pair._2.reverse)
	
	def run() {
		GraphQuestion.searchTree(childrenL, 12)
	}
}
