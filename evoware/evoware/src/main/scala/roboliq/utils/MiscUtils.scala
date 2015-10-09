package roboliq.utils

import scala.annotation.tailrec

object MiscUtils {
	def md5Hash(text: String): String =
		java.security.MessageDigest.getInstance("MD5").digest(text.getBytes()).map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}

	def compareNatural(s1: String, s2: String): Int = {
		// Skip all identical characters
		val len1 = s1.length
		val len2 = s2.length
		
		// Find index of difference or end of strings
		val i = {
			var i = 0
			while ((i < len1) && (i < len2) && s1.charAt(i) == s2.charAt(i)) {
				i += 1
			}
			i
		}

		// Check whether either string has reached the end
		val end1 = i == len1
		val end2 = i == len2
		if (end1 && end2) return 0
		else if (!end1 && end2) return 1
		else if (end1 && !end2) return -1
		
		// Get chars where they first differ
		val c1: Char = s1.charAt(i)
		val c2: Char = s2.charAt(i)

		// Check digit in first string
		if (Character.isDigit(c1))
		{
			// Check digit only in first string 
			if (!Character.isDigit(c2))
				return 1

			// Scan all integer digits
			var x1 = i + 1
			while ((x1 < len1) && Character.isDigit(s1.charAt(x1))) {
				x1 += 1
			}
			var x2 = i + 1
			while ((x2 < len2) && Character.isDigit(s2.charAt(x2))) {
				x2 += 1
			}

			// Longer integer wins, first digit otherwise
			return (if (x2 == x1) c1 - c2 else x1 - x2)
		}

		// Check digit only in second string
		if (Character.isDigit(c2))
			return -1

		// No digits
		return (c1 - c2)
	}
}
