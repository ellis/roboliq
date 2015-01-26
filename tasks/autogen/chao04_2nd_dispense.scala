import scala.util.Random
Random.setSeed(0)

// Get all permutations of 1 - 4 (there are 24 of them)
val permutations = List.range(1, 5).permutations.toList
// Make enough randomly shuffed copies of the permutations to fill a 384-well plate
val l1 = List.fill(384 / permutations.flatten.length)(Random.shuffle(permutations))
// Flatten the lists, giving us a list of 384 tips
val l2 = l1.flatten.flatten
// Map from well (0-indexed) to tip
val wellToTip = l2.zipWithIndex.map(_.swap).toMap
val tipToWells0 = wellToTip.toList.groupBy(_._2).mapValues(_.map(_._1).sorted)
val tipToWells = tipToWells0.mapValues(l => Random.shuffle(l))

val wellToVol = Random.shuffle(List(5, 10, 15, 20, 25, 30, 35, 40).permutations.toList).take(384/8).flatten.zipWithIndex.map(_.swap).toMap

def getWellName(n: Int): String = {
  val col = n / 16 + 1
  val row = n % 16
  val rowLetter = (row + 65).asInstanceOf[Char]
  f"$rowLetter$col%02d"
}

def step(l1: List[Int], l2: List[Int], l3: List[Int], l4: List[Int], wellToVol: Map[Int, Int]) {
  (l1, l2, l3, l4) match {
    case (w1::r1, w2::r2, w3::r3, w4::r4) =>
      val a1 = wellToVol(w1)
      val a2 = wellToVol(w2)
      val a3 = wellToVol(w3)
      val a4 = wellToVol(w4)
      println(s"    - { d: plate1(${getWellName(w1)}), a: $a1, tip: 1 }")
      println(s"    - { d: plate1(${getWellName(w2)}), a: $a2, tip: 2 }")
      println(s"    - { d: plate1(${getWellName(w3)}), a: $a3, tip: 3 }")
      println(s"    - { d: plate1(${getWellName(w4)}), a: $a4, tip: 4 }")
      step(r1, r2, r3, r4, wellToVol)
    case _ =>
  }
}

val wellToVolFirst = wellToVol.mapValues(60 - _)
//step(tipToWells(1), tipToWells(2), tipToWells(3), tipToWells(4), wellToVolFirst)
step(tipToWells(1), tipToWells(2), tipToWells(3), tipToWells(4), wellToVol)
