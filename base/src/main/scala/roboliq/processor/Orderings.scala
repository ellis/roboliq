package roboliq.processor

object ListIntOrdering extends Ordering[List[Int]] {
	def compare(a: List[Int], b: List[Int]): Int = {
		(a, b) match {
			case (Nil, Nil) => 0
			case (Nil, _) => -1
			case (_, Nil) => 1
			case (a1 :: rest1, a2 :: rest2) =>
				if (a1 != a2) a1 - a2
				else compare(rest1, rest2)
		}
	}
}

object ListStringOrdering extends Ordering[List[String]] {
	def compare(a: List[String], b: List[String]): Int = {
		(a, b) match {
			case (Nil, Nil) => 0
			case (Nil, _) => -1
			case (_, Nil) => 1
			case (a1 :: rest1, a2 :: rest2) =>
				val n = a1.compare(a2)
				if (n != 0) n
				else compare(rest1, rest2)
		}
	}
}
