package roboliq.utils

object RstDoc {
	def section_ls(name: String, underline: Char, bOverline: Boolean = false): List[String] = {
		val n = name.length
		val underline_s = underline.toString * n
		if (bOverline) List("", underline_s, name, underline_s, "")
		else List("", name, underline_s, "") 
	}
}
