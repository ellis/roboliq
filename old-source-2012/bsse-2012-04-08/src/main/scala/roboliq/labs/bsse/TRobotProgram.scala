package roboliq.labs.bsse

object TRobotProgram {
	def generate(
		dir: Int,
		prog: Int,
		name: String,
		cycles: List[Tuple2[Int, List[Tuple2[Int, Int]]]]
	): String = {
		val name1 = "%-8s".format(name.take(8))
		val lHeader = List(
			":c",
			"b "+dir+","+prog,
			"a "+dir+","+prog,
			"a 63,1,'"+name1+"'"
		)
		
		var iLine = 0
		val lBody = cycles.flatMap(cycle => {
			val (nTimes, lTempToSec) = cycle
			val (iLineStart, nRep) = {
				if (nTimes > 1) (iLine + 1, nTimes - 1)
				else (0, 0)
			}
			
			(for ((nTemp, nSec) <- lTempToSec.init) yield {
				iLine += 1
				"b "+hex(iLine)+","+hex(nTemp*100)+","+hex(nSec)+",0,0,0,0,0,1f4"
			}) ++
			List[String]({
				val (nTemp, nSec) = lTempToSec.last
				iLine += 1
				"b "+hex(iLine)+","+hex(nTemp*100)+","+hex(nSec)+","+hex(iLineStart)+","+hex(nRep)+",0,0,0,1f4"
			})
		})
		
		val lFooter = List("g\r")
		
		val l = lHeader ++ lBody ++ lFooter
		val s = l.mkString("\r")
		s
	}
	
	private def hex(n: Int): String = "%x".format(n)
	
	def generateTouchdown(
		dir: Int,
		prog: Int,
		name: String,
		nBoilingTemp: Int,
		nExtensionTemp: Int,
		nExtensionTime: Int,
		nAnnealingTemp1: Int,
		nAnnealingTemp2: Int,
		nAnnealingTime: Int,
		nFinalReps: Int
	): String = {
		val nTempDiff = nAnnealingTemp1 - nAnnealingTemp2
		// Get annealing temperatures from high to final, not including final
		val lnAnnealingTemp = ((nAnnealingTemp2 + 1) to nAnnealingTemp1).toList.reverse
		val cycles: List[Tuple2[Int, List[Tuple2[Int, Int]]]] =
			List(1 -> List(nBoilingTemp -> 60)) ++
			lnAnnealingTemp.map(nTemp => (1 -> List(nBoilingTemp -> 10, nTemp -> 20, nExtensionTemp -> nExtensionTime))) ++
			List(
				nFinalReps -> List(nBoilingTemp -> 10, nAnnealingTemp2 -> 20, nExtensionTemp -> nExtensionTime),
				1 -> List(nExtensionTemp -> 4 * 60),
				1 -> List(9 -> 99 * 60 * 60)
			)
		generate(dir, prog, name, cycles)
	}
}
/*
:c
b 0,7
a 0,7
a 63,1,'tchdntaq'
b 1,251c,3c,0,0,0,0,0,1f4
b 2,251c,a,0,0,0,0,0,1f4
b 3,1a90,14,0,0,0,0,0,1f4
b 4,1c20,1e,0,0,0,0,0,1f4
b 5,2648,a,0,0,0,0,0,1f4
b 6,1a2c,14,0,0,0,0,0,1f4
b 7,1c20,1e,0,0,0,0,0,1f4
b 8,2648,a,0,0,0,0,0,1f4
b 9,19c8,14,0,0,0,0,0,1f4
b a,1c20,1e,0,0,0,0,0,1f4
b b,2648,a,0,0,0,0,0,1f4
b c,1964,14,0,0,0,0,0,1f4
b d,1c20,1e,0,0,0,0,0,1f4
b e,2648,a,0,0,0,0,0,1f4
b f,1900,14,0,0,0,0,0,1f4
b 10,1c20,1e,0,0,0,0,0,1f4
b 11,2648,a,0,0,0,0,0,1f4
b 12,189c,14,0,0,0,0,0,1f4
b 13,1c20,1e,0,0,0,0,0,1f4
b 14,2648,a,0,0,0,0,0,1f4
b 15,1838,14,0,0,0,0,0,1f4
b 16,1c20,1e,0,0,0,0,0,1f4
b 17,2648,a,0,0,0,0,0,1f4
b 18,17d4,14,0,0,0,0,0,1f4
b 19,1c20,1e,0,0,0,0,0,1f4
b 1a,2648,a,0,0,0,0,0,1f4
b 1b,1770,14,0,0,0,0,0,1f4
b 1c,1c20,1e,0,0,0,0,0,1f4
b 1d,2648,a,0,0,0,0,0,1f4
b 1e,170c,14,0,0,0,0,0,1f4
b 1f,1c20,1e,0,0,0,0,0,1f4
b 20,2648,a,0,0,0,0,0,1f4
b 21,16a8,14,0,0,0,0,0,1f4
b 22,1c20,1e,20,f,0,0,0,1f4
b 23,1c20,f0,0,0,0,0,0,1f4
b 24,384,57030,0,0,0,0,0,1f4
g
*/