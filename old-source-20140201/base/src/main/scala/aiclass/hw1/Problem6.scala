package aiclass.hw1

import ailib.ch03
import ailib.ch03._


object Problem6 {
	val childrenL = Map(
		0 -> List(1, 2),
		1 -> List(3, 4),
		2 -> List(4, 5),
		3 -> List(6, 7),
		4 -> List(7, 8),
		5 -> List(8, 9),
		6 -> List(10),
		7 -> List(10, 11),
		8 -> List(11, 12),
		9 -> List(12),
		10 -> List(13),
		11 -> List(13, 14),
		12 -> List(14),
		13 -> List(15),
		14 -> List(15)
	)
	
	def run() {
		GraphQuestion.searchGraph(childrenL, 9)
	}
}
