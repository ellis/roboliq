import java.io.FileReader

import scala.util.parsing.combinator._
import scala.collection.mutable.HashMap

class Arith extends JavaTokenParsers {
	def expr: Parser[Any] = term~rep("+"~term | "-"~term)
	def term: Parser[Any] = factor~rep("*"~factor | "/"~factor)
	def factor: Parser[Any] = floatingPointNumber | "("~expr~")"
}

case class Reagent(name: String, rack: String, iWell: Int, nWells: Int, sLiquidClass: String)

class Parser extends JavaTokenParsers {
	//val ident: Parser[String] = """[a-zA-Z_]\w*""".r
	val word: Parser[String] = """\w+""".r
	val integer: Parser[Int] = """[0-9]+""".r ^^ (_.toInt)
	
	val mapVars = new HashMap[String, String]
	val mapOptions = new HashMap[String, String]
	val mapReagents = new HashMap[String, Reagent]
	
	def doAssign(id: String, s: String) { mapVars(id) = s }
	
	def doOption(args: ~[String, Option[String]]) = args match {
		case id ~ value =>
			mapOptions(id) = value.getOrElse(null)
	}
	
	def doReagent(reagent: String, rack: String, iWell: Int, lc: String, nWells_? : Option[Int]) {
		mapReagents(reagent) = new Reagent(reagent, rack, iWell, nWells_?.getOrElse(1), lc)
	}
	
	val cmds = List[Parser[Any]](
			ident ~"="~ floatingPointNumber ^^
				{ case id ~"="~ s => doAssign(id, s) },
			"OPTION"~> ident~opt(word) ^^ doOption,
			"REAGENT"~> ident~ident~integer~ident~opt(integer) ^^
				{ case reagent ~ rack ~ iWell ~ lc ~ nWells_? => doReagent(reagent, rack, iWell, lc, nWells_?) } 
			)

	
	val cmds0: Parser[Any] =
			("OPTION"~> ident~opt(word) ^^
				{ case id ~ value => println(id, value) }) |
			("REAGENT"~> ident~ident~integer~ident~opt(integer) ^^
				{ case reagent ~ rack ~ iWell ~ lc ~ nWells_? => println(reagent, rack, iWell, lc, nWells_?) }) 
			
}

object Main extends App {
	//val p = new Arith
	//println(p.parseAll(p.expr, args(0)))
	
	test2()
	
	def test1() {
		val p = new Parser
		val lines = List(
				"OPTION A",
				"OPTION B 23",
				"REAGENT PCR_Mix_X5 T10 1 PIE_AUTBOT 2",
				"WET_MIX_VOL = 150",
				"CE_SEQ_DIL_VOL = 28.5"
				)
		
		def findFirstMatch(s: String, cmds: List[p.Parser[Any]]): Boolean = cmds match {
			case Nil => false
			case cmd :: rest => val r = p.parseAll(cmd, s)
				if (r.successful)
					true
				else
					findFirstMatch(s, rest)
		}
		
		for (s <- lines) {
			val b = findFirstMatch(s, p.cmds)
			if (!b) {
				println("Unrecognized command:")
				println(s)
			}
		}
		
		println(p.mapOptions)
		println(p.mapVars)
		println(p.mapReagents)
	}
	
	def test2() {
		val p = new roboliq.roboease.Parser
		
		val plate4, plate5, plate6 = new roboliq.level3.Plate
		p.plates ++= List("P4" -> plate4, "P5" -> plate5, "P6" -> plate6)
		new roboliq.level3.PlateProxy(p.kb, plate4).setDimension(8, 12)
		new roboliq.level3.PlateProxy(p.kb, plate5).setDimension(8, 12)
		new roboliq.level3.PlateProxy(p.kb, plate6).setDimension(8, 12)
		
		val sSource = """
OPTION A
OPTION B 23
REAGENT PCR_Mix_X5 T10 1 PIE_AUTBOT 2
WET_MIX_VOL = 150
CE_SEQ_DIL_VOL = 28.5
LIST DDW_LIST
300
400
500
ENDLIST
"""
		//p.parse(sSource)
		val sSource2 = scala.io.Source.fromFile("/home/ellisw/src/TelAviv/scripts/Rotem_Script01.conf").mkString
		p.parse(sSource2)
		
		println(p.mapOptions)
		println(p.mapVars)
		println(p.mapReagents)
		println(p.mapLists)
		println(p.mapLabware)
	}
}
