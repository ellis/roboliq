package roboliq.utils

object MathUtils {
	def gcd(a: Long, b: Long): Long = {
		if (b == 0) a
		else gcd(b, a % b)
	}
	
	def gcd(l: List[Long]): Long =
		l.foldLeft(1L)(gcd)
}
