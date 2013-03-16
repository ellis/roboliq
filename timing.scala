val nN = 100
val times = new Array[Int](nN)

def time[T](x : => T) =  {
	val start = System.nanoTime
	val result = x
	val duration = System.nanoTime - start
	//println("Elapsed time " + duration / 1000000.0 + " msecs")
	println("Elapsed time " + duration + " nsecs")
	result
}

def time1[T](x : => T): Int = {
	val start = System.nanoTime
	val result = x
	val duration = System.nanoTime - start
	math.round(duration).asInstanceOf[Int]
}

def timeN[T](f : => T): Int = {
	(0 until nN).foreach(i => times(i) = time1(f))
	val median: Int = times.toIndexedSeq.sortBy(identity).apply(nN / 2)
	println("Median time " + median + " nsecs")
	median
}

//val l1 = 0 to 1000000
val l2 = (0 to 1000000).toList
val l3 = l2.toArray

def byListFoldLeft() {
	l2.foldLeft(0)((acc, n) => acc + n)
}

def byListFoldRight() {
	l2.foldRight(0)((n, acc) => acc + n)
}

def rec1(l: List[Int], acc: Int): Int = {
	l match {
		case Nil => acc
		case n :: ns => rec1(ns, acc + n)
	}
}

def byListRecursion1() {
	rec1(l2, 0)
}

def rec2(l: List[Int], acc: Int): Int = {
	if (l.isEmpty) acc
	else rec1(l.tail, acc + l.head)
}

def byListRecursion2() {
	rec2(l2, 0)
}

def byListReduce() {
	l2.reduce(_ + _)
}

def byListLoop() {
	var n = 0
	l2.foreach(n += _)
}

def byArrayLoop() {
	var n = 0
	var i = l3.length - 1
	while (i >= 0) {
		n += l3(i)
		i -= 1
	}
}

def rec3(a: Array[Int], i: Int, acc: Int): Int = {
	if (i < 0) acc
	else rec3(a, i - 1, acc + a(i))
}

def byArrayRecursion() {
	rec3(l3, l3.length - 1, 0)
}

def run() {
	//timeN(l1.reduce(_ + _))
	println("byListFoldLeft:")
	timeN(byListFoldLeft)
	//println("byListFoldRight:")
	//timeN(byListFoldRight)
	println("byListRecursion1:")
	timeN(byListRecursion1)
	println("byListRecursion2:")
	timeN(byListRecursion2)
	println("byListReduce:")
	timeN(byListReduce)
	println("byListLoop:")
	timeN(byListLoop)
	println("byArrayLoop:")
	timeN(byArrayLoop)
	println("byArrayRecusion:")
	timeN(byArrayRecursion)
}
