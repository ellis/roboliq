package roboliq.utils

import scala.collection.mutable.ArrayBuffer
import grizzled.slf4j.Logger


object RstDoc {
	def section_ls(name: String, underline: Char, bOverline: Boolean = false): List[String] = {
		val n = name.length
		val underline_s = underline.toString * n
		if (bOverline) List("", underline_s, name, underline_s, "")
		else List("", name, underline_s, "") 
	}
}

class RstBuilder {
	private val logger = Logger[this.type]

	private val line_l = new ArrayBuffer[String]
	private val underline_lc = Array[Char]('=', '=', '-', '~', '.')
	private var padding_n = 0
	private var indent_n = 0
	
	def header1(s: String): RstBuilder = header(s, underline_lc(0), true)
	def header2(s: String): RstBuilder = header(s, 1)
	def header3(s: String): RstBuilder = header(s, 2)
	def header4(s: String): RstBuilder = header(s, 3)
	def header5(s: String): RstBuilder = header(s, 4)
	
	private def header(name: String, underline: Char, bOverline: Boolean = false): RstBuilder = {
		val n = name.length
		val underline_s = underline.toString * n
		pad(1)
		if (bOverline) add(underline_s)
		add(name, underline_s) 
		pad(1)
		this
	}

	private def header(s: String, i: Int): RstBuilder = header(s, underline_lc(i), false)
	
	def add(l: String*): RstBuilder = {
		// Indent
		val l_# = l.map(("  "*indent_n) + _)
		// Add to buffer
		line_l ++= l_#
		// Log
		l_#.foreach(logger.trace(_))
		// Reset padding
		padding_n = 0
		this
	}
	
	//def add(l: Seq[String]): RstBuilder = add(l : _*)
	
	def pad(n: Int): RstBuilder = {
		if (n > padding_n) {
			val n_# = n - padding_n
			if (n_# > 0)
				line_l ++= List.fill(n_#)("")
			padding_n = n
		}
		this
	}
	
	def indent(key: String)(fn: RstBuilder => Unit): RstBuilder = {
		pad(1).add(s".. $key::").pad(1)
		indent_n += 1
		fn(this)
		indent_n -= 1
		this
	}
	
	override def toString = line_l.mkString("\n")
}
/*
class RstIndentedBuilder(builder: RstBuilder, indent: Int) {
	def add(l: String*): RstBuilder = {
		line_l ++= l
		padding_n = 0
		this
	}
	
	def add(l: Seq[String]): RstBuilder = add(l : _*)
}*/