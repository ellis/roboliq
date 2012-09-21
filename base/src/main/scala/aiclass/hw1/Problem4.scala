package aiclass.hw1

import ailib.ch03._


object Problem4 {
	val children = Map(
		0 -> List(1, 2, 3),
		1 -> List(4, 5),
		2 -> List(6, 7),
		3 -> List(8, 9)
	)
	
	def run() {
		GraphQuestion.searchTree(children, 5)
	}
}
