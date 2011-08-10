import java.io.FileReader

import scala.util.parsing.combinator._
import scala.collection.mutable.HashMap

class Arith extends JavaTokenParsers {
	def expr: Parser[Any] = term~rep("+"~term | "-"~term)
	def term: Parser[Any] = factor~rep("*"~factor | "/"~factor)
	def factor: Parser[Any] = floatingPointNumber | "("~expr~")"
}

case class Reagent(name: String, rack: String, iWell: Int, nWells: Int, sLiquidClass: String)

class Parser1 extends JavaTokenParsers {
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
		val p = new Parser1
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
		import roboliq.roboease._
		import roboliq.level3._
		
		val p = new Parser
		
		//p.DefineRack($DITI_WASTE,1,6,8,12, 0) ;
		p.DefineRack("CSL",2,0,1,8, 5000000,"Carousel MTP") ;
		p.DefineRack("TS4",15,0,1,16, 5000000) ;
		p.DefineRack("TS5",16,0,1,16, 5000000) ;
		//p.DefineRack("TR1",14,0,1,8, 5000000) ;
		p.DefineRack("TR2",14,1,1,8, 5000000) ;
		p.DefineRack("TR3",14,2,1,8, 5000000) ;
		p.DefineRack("TR4",17,0,1,8, 5000000) ;
		p.DefineRack("TR5",17,1,1,8, 5000000) ;
		p.DefineRack("TR6",17,2,1,8, 5000000) ;
		p.DefineRack("TR7",18,0,1,8, 5000000) ;
		p.DefineRack("TR8",18,1,1,8, 5000000) ;
		p.DefineRack("TR9",18,2,1,8, 5000000) ;
		//p.DefineRack("T1",21,0,1,16, 2100) ;
		//p.DefineRack("T2",20,0,1,16, 2100) ;
		//p.DefineRack("T3",19,0,1,16, 2100) ;
		p.DefineRack("E3",22,0,1,16, 2100) ;
		//p.DefineRack("P1",23,0,12,8, 200,"MP 3Pos Fixed") ;
		p.DefineRack("P2",23,1,12,8, 1200,"MP 3Pos Fixed") ;
		p.DefineRack("T2",23,1,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF2",23,1,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M2",23,1,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P3",23,2,12,8, 1200,"MP 3Pos Fixed") ;
		p.DefineRack("T3",23,2,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF3",23,2,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M3",23,2,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P4",29,0,12,8, 1200,"MP 3Pos Fixed") ;
		p.DefineRack("T4",29,0,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF4",29,0,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M4",29,0,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P5",29,1,12,8, 200,"MP 3Pos Fixed") ;
		p.DefineRack("T5",29,1,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF5",29,1,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M5",29,1,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P6",29,2,12,8, 200,"MP 3Pos Fixed") ;
		p.DefineRack("T6",29,2,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF6",29,2,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M6",29,2,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P7",35,0,12,8, 200,"MP 3Pos Fixed PCR") ;
		p.DefineRack("P8",35,1,12,8, 200,"MP 3Pos Fixed PCR") ;
		p.DefineRack("P9",35,2,12,8, 200,"MP 3Pos Fixed PCR") ;
		p.DefineRack("P10",41,0,12,8, 200,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("T10",41,0,6,4, 2100,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("BUF10",41,0,6,8, 2100,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("M10",41,0,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P11",41,1,12,8, 1200,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("T11",41,1,6,4, 2100,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("BUF11",41,1,6,8, 2100,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("M11",41,1,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P12",41,2,12,8, 200,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("T12",41,2,6,4, 2100,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("BUF12",41,2,6,8, 2100,"MP 3Pos Fixed 2+clips") ;
		p.DefineRack("M12",41,2,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P13",47,0,12,8, 200,"MP 3Pos Fixed") ;
		p.DefineRack("T13",47,0,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF13",47,0,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M13",47,0,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P14",47,1,12,8, 1200,"MP 3Pos Fixed") ;
		p.DefineRack("T14",47,1,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF14",47,1,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M14",47,1,24,16,1200,"MP 3Pos Fixed") ;
		p.DefineRack("P15",47,2,12,8, 200,"MP 3Pos Fixed") ;
		p.DefineRack("T15",47,2,6,4, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("BUF15",47,2,6,8, 2100,"MP 3Pos Fixed") ;
		p.DefineRack("M15",47,2,24,16,1200,"MP 3Pos Fixed") ;
		//p.DefineRack("E10",41,0,6,4, 2100) ;
		//p.DefineRack("E2",47,1,6,4, 2100) ;
		//p.DefineRack("TR1",47,2,6,8, 2100) ;
		p.DefineRack("TR1",41,2,6,8, 2100) ;
		p.DefineRack("LNK",65,0,12,8, 200,"Te-Link") ;
		p.DefineRack("S1",53,0,12,8, 200,"Te-Shake 2Pos") ;
		p.DefineRack("S2",53,1,12,8, 1200,"Te-Shake 2Pos") ;
		p.DefineRack("MS1",53,0,24,16, 200,"Te-Shake 2Pos") ;
		p.DefineRack("MS2",53,1,24,16, 1200,"Te-Shake 2Pos") ;
		p.DefineRack("TP1",59,0,12,8, 200,"Torrey pines") ;
		p.DefineRack("TP2",59,1,12,8, 1200,"Torrey pines") ;
		//p.DefineRack("T1",41,0,6,4, 2100) ;
		//p.DefineRack("T2",59,1,6,4, 2100) ;
		p.DefineRack("HA1",4,0,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HA2",4,1,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HA3",4,2,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HA4",4,3,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HA5",4,4,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HB1",10,0,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HB2",10,1,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HB3",10,2,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HB4",10,3,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HB5",10,4,12,8, 200,"HOTEL5A") ;
		p.DefineRack("HC1",66,0,12,8, 200,"HOTEL5B") ;
		p.DefineRack("HC2",66,1,12,8, 200,"HOTEL5B") ;
		p.DefineRack("HC3",66,2,12,8, 200,"HOTEL5B") ;
		p.DefineRack("HC4",66,3,12,8, 200,"HOTEL5B") ;
		p.DefineRack("HC5",66,4,12,8, 200,"HOTEL5B") ;
		p.DefineRack("RCH",9,0,12,8, 200,"ROCHE") ;
		p.DefineRack("READER",48,0,12,8, 200,"PLATE_READER") ;
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
