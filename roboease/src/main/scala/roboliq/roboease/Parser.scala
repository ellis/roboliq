package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.util.parsing.combinator._


/*object Tok extends Enumeration {
	val Ident, Int, Word, Double, String,
		Id, IdNew, Rack,
		Source, Plate, Wells, Location,
		Volume, LiquidClass, Params = Value 
}

case class Function(sName: String, args: List[Tok.Value], opts: List[Tok.Value] = Nil)

object X {
	val x = List[Function](
			Function("DIST_REAGENT", List(Tok.Source, Tok.Plate, Tok.Wells, Tok.Volume, Tok.LiquidClass), List(Tok.Params))
			)
	val y = List[Function](
			Function("LABWARE", List(Tok.Id)),
			Function("OPTION", List(Tok.Id), List(Tok.Word)),
			Function("REAGENT", List(Tok.Id, Tok.Rack, Tok.Int, Tok.LiquidClass), List(Tok.Int)),
			Function("TABLE", List(Tok.Word))
			)
}*/
case class Reagent(name: String, rack: String, iWell: Int, nWells: Int, sLiquidClass: String)

class Parser extends JavaTokenParsers {
	//val ident: Parser[String] = """[a-zA-Z_]\w*""".r
	val word: Parser[String] = """\w+""".r
	val integer: Parser[Int] = """[0-9]+""".r ^^ (_.toInt)
	
	val mapVars = new HashMap[String, String]
	val mapOptions = new HashMap[String, String]
	val mapReagents = new HashMap[String, Reagent]
	
	def doAssign(id: String, s: String) { mapVars(id) = s }
	
	def doOption(id: String, value: Option[String]) { mapOptions(id) = value.getOrElse(null) }
	
	def doReagent(reagent: String, rack: String, iWell: Int, lc: String, nWells_? : Option[Int]) {
		mapReagents(reagent) = new Reagent(reagent, rack, iWell, nWells_?.getOrElse(1), lc)
	}
	
	val cmd0List: Parser[String] = "LIST"~>ident 
	val cmd0Assign: Parser[Unit] = ident ~"="~ floatingPointNumber ^^
				{ case id ~"="~ s => doAssign(id, s) }
	val cmds0 = List[Tuple2[String, Parser[Any]]](
			("OPTION", ident~opt(word) ^^
				{ case id ~ value => doOption(id, value) }),
			("REAGENT", ident~ident~integer~ident~opt(integer) ^^
				{ case reagent ~ rack ~ iWell ~ lc ~ nWells_? => doReagent(reagent, rack, iWell, lc, nWells_?) }) 
			)
	
	private var m_section = 0
	private var m_asDoc: List[String] = Nil
	private var m_asList = new ArrayBuffer[String]
	private var m_sListName: String = null
	private var m_sError: String = null
	//private val m_mapVars = new HashMap[String, String]
	private val m_mapLists = new HashMap[String, List[String]]
	
	// "DIST_REAGENT"
	
	def parse(sSource: String) {
		m_section = 0
		for (sLine <- sSource.lines) {
			val s = sLine.replaceAll("#.*", "").trim
			s match {
				case "" =>
				case "DOC" => m_section = 1
				case "SCRIPT" => m_section = 2
				case _ =>
					m_section match {
						case 0 => handleConfig(s)
						case 1 => handleDoc(s)
						case 2 => handleScript(s)
						case 3 => handleConfigList(s)
					}
			}
		}
	}
	
	def handleDoc(s: String) {
		if (s == "ENDDOC") {
			m_asDoc = m_asDoc.reverse
			m_section = 0
		}
		else
			m_asDoc = s :: m_asDoc
	}
	
	def handleConfig(sLine: String) {
		val rAssign = parseAll(cmd0Assign, sLine)
		val rList = parseAll(cmd0List, sLine)
		if (rAssign.successful) {
			// Do nothing
		}
		else if (rList.successful) {
			m_asList.clear
			m_sListName = rList.get
			m_section = 3
		}
		else {
			val rCmd: Parser[String] = """\w+""".r <~ ".*".r
			parse(rCmd, sLine)
			/*val b = findFirstMatch(s, cmds0)
			if (!b) {
				m_sError = "Unrecognized command: " + sName
			}*/
		}
	}
	
	private def findFirstMatch(s: String, cmds: List[Parser[Any]]): Boolean = cmds match {
		case Nil => false
		case cmd :: rest => val r = parseAll(cmd, s)
			if (r.successful)
				true
			else
				findFirstMatch(s, rest)
	}

	def handleConfigList(s: String) {
		if (s == "ENDLIST") {
			m_mapLists(m_sListName) = m_asList.toList
			m_section = 0
		}
		else
			m_asDoc = s :: m_asDoc
	}
	
	def handleScript(s: String) {
		if (s == "ENDSCRIPT")
			m_section = 0
		else {
			
		}
	}
	
	/*
	private def tokenize(s: String, fs: List[Function]): List[String] = {
		// Remove comments and extra spaces
		val s1 = s.replaceAll("#.*", "").trim
		if (s1.isEmpty)
			return Nil
		val as = s1.split(" +")
		val sName = as.head
		val args = as.tail
		val fs1 = fs.filter(_.sName == sName)
		if (fs1.isEmpty) {
			m_sError = "Unrecognized command: " + sName
			return Nil
		}
		val asErrors = new ArrayBuffer[String]
		Nil
	}

	private def matcheArgs(f: Function, args: Array[String]): String = {
		val nArgsMin = f.args.size
		val nArgsMax = nArgsMin + f.opts.size 
		if (args.size < nArgsMin || args.size > nArgsMax)
			return "Wrong number of arguments"
		
		var iarg = 0
		for (tok <- f.args) {
			val sArg = args(iarg)
			iarg += 1
			
			tok match {
				case Tok.Ident =>
				case Tok.Int =>
				case Tok.Word =>
				case Tok.Double =>
					
				case Tok.Id =>
				case Tok.IdNew =>
				case Tok.Rack =>
				case Tok.Source =>
				case Tok.Plate =>
				case Tok.Wells =>
				case Tok.Location =>
				case Tok.Volume =>
				case Tok.LiquidClass =>
				case Tok.Params =>					
			}
		}
		
		return null
	}
	*/
}
