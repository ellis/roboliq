package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.util.parsing.combinator._

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


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
//case class Reagent(name: String, rack: String, iWell: Int, nWells: Int, sLiquidClass: String)

case class Carrier(
		name: String,
		nRows: Int,
		nCols: Int,
		grid: Int, site: Int, nVolumeMax: Double, carrierType: String
)
//case class Rack(name: String, nRows: Int, nCols: Int)

class Parser extends JavaTokenParsers {
	val kb = new KnowledgeBase
	/** Plate locations */
	val carriers = new HashMap[String, Carrier]
	val mapLocToPlate = new HashMap[String, Plate]
	
	val mapLiquids = new HashMap[String, Liquid]
	val mapVars = new HashMap[String, String]
	val mapOptions = new HashMap[String, String]
	//val mapReagents = new HashMap[String, Reagent]
	val mapLabware = new HashMap[String, Tuple2[String, String]]
	
	private var m_contextPlate: Option[Plate] = None
	

	val word: Parser[String] = """[^\s]+""".r
	val integer: Parser[Int] = """[0-9]+""".r ^^ (_.toInt)
	
	/*def idCarrier: Parser[Carrier] = Parser[Carrier] { in =>
		val res1 = ident.apply(in)
		res1 match {
			case Success(sLoc, _) =>
				carriers.get(sLoc) match {
					case Some(carrier) => Success(carrier, res1.next)
					case None => Failure("Unknown location: "+sLoc, in)
				}
			case ns: NoSuccess => ns
		}
	}*/
	
	def idPlate = Parser[Plate] { in =>
		val res1 = ident.apply(in)
		res1 match {
			case Success(sLoc, _) =>
				getPlateAtLoc(sLoc, in) match {
					case Right(plate) => Success(plate, res1.next)
					case Left(res) => res
				}
			case ns: NoSuccess => ns
		}
	}
	
	def idWell_index = Parser[Well] { input =>
		val res1 = integer.apply(input)
		res1 match {
			case Success(n, _) =>
				m_contextPlate match {
					case None => Failure("Unknown parent plate", input)
					case Some(plate) =>
						getWell_?(plate, n) match {
							case Some(well) => Success(well, res1.next)
							case None => Error("Invalid well index: "+n, input)
						}
				}
			case ns: NoSuccess => ns
		}
	}
	
	def idWell_rowCol = Parser[Well] { input =>
		val res1 = ("[A-Z]".r~integer).apply(input)
		res1 match {
			case Success(sRow ~ nCol, _) =>
				m_contextPlate match {
					case None => Failure("Unknown parent plate", input)
					case Some(plate) =>
						val iRow = sRow.charAt(0) - 'A'
						val iCol = nCol - 1
						getWell(plate, iRow, iCol, input) match {
							case Right(well) => Success(well, res1.next)
							case Left(e) => e
						}
				}
			case ns: NoSuccess => ns
		}
	}
	
	def idWell: Parser[Well] = idWell_index | idWell_rowCol
	
	/*def idWells_index = Parser[List[Well]] { input =>
		val res1 = idWell_index.apply(input)
		res1 match {
			case Success(well0, input2) =>
				val plate = m_contextPlate.get
				val res2 = ("-"~>idWell_index).apply(input2)
				res2 match {
					case Success(well1, _) =>
						val wells = getWells(plate, well0, well1)
						Success(wells, res2.next)
					case ns: NoSuccess =>
						Success(List(well0), res1.next)
				}
			case ns: NoSuccess => ns
		}
	}*/
	
	def idWells = Parser[List[Well]] { input =>
		val res1 = idWell.apply(input)
		res1 match {
			case Success(well0, input2) =>
				val plate = m_contextPlate.get
				val res2 = ("+"~>integer).apply(input2)
				res2 match {
					case Success(nWells, _) =>
						getWells(plate, well0, nWells, input) match {
							case Right(wells) => Success(wells, res2.next)
							case Left(e) => e
						}
					case ns: NoSuccess =>
						val res3 = ("-"~>idWell).apply(input2)
						res3 match {
							case Success(well1, _) =>
								val wells = getWells(plate, well0, well1)
								Success(wells, res3.next)
							case ns: NoSuccess =>
								Success(List(well0), res1.next)
						}
				}
			case ns: NoSuccess => ns
		}
	}
	
	/*def idWells = Parser[List[Well]] { input =>
		val p1 = ("[A-Z]".r~integer~"+"~integer)
		val p2 = ("[A-Z]".r~integer~"-"~"[A-Z]".r~integer)
		val p3 = ("[A-Z]".r~integer)
		val res1 = integer.apply(input)
		res1 match {
			case Success(n, _) =>
				m_contextPlate match {
					case None => Failure("Unknown parent plate", input)
					case Some(plate) =>
						getWell_?(plate, n) match {
							case Some(well) => Success(well, res1.next)
							case None => Failure("Invalid well index: "+n, input)
						}
				}
			case ns: NoSuccess => ns
		}
	}*/
	
	def idLiquid: Parser[Liquid] = Parser[Liquid] { input =>
		val res1 = ident.apply(input)
		res1 match {
			case Success(sLiq, _) =>
				mapLiquids.get(sLiq) match {
					case Some(liq) => Success(liq, res1.next)
					case None => Error("Undefined reagent: "+sLiq, input)
				}
			case ns: NoSuccess => ns
		}
	}
	
	/** Return list of row/column tuples */
	def plateWells2_sub0: Parser[Tuple3[Char, Int, Int]] = "[A-Z]".r~integer~"+"~integer ^^
		{ case sRow~n0~"+"~n1 => (sRow.charAt(0), n0, n1) }
	def plateWells2_sub1: Parser[List[Tuple3[Char, Int, Int]]] = rep1sep(plateWells2_sub0, ",")
	def plateWells2_sub2: Parser[List[Tuple2[Plate, Int]]] = Parser[List[Tuple2[Plate, Int]]] { input =>
		//val p: Parser[Parser.this.~[Parser.this.~[roboliq.level3.Plate,String],List[(Char, Int, Int)]]] = (idPlate~":"~plateWells2_sub1)
		val res1 = (idPlate~":"~plateWells2_sub1).apply(input)
		//val res1 = p.apply(input)
		res1 match {
			case Success(plate ~ ":" ~ wellsByRowCol, _) =>
				val pc = kb.getPlateSetup(plate)
				var sError: String = null
				val list = wellsByRowCol.flatMap(pair => {
					val dim = pc.dim_?.get
					val (nRows, nCols) = (dim.nRows, dim.nCols)
					val (iRow, iCol, nWells) = (pair._1 - 'A', pair._2 - 1, pair._3)
					if (iRow < 0 || iRow >= nRows) {
						sError = ("Invalid row: "+pair._1)
						Nil
					}
					else if (iCol < 0 || iCol >= nCols) {
						sError = ("Invalid column: "+pair._2)
						Nil
					}
					else {
						val i0 = iRow + iCol * nRows
						val i1 = i0 + nWells - 1
						if (i1 >= nRows * nCols) {
							sError = ("Exceeds plate dimension: "+pair._1+pair._2+"+"+pair._3)
							Nil
						}
						else {
							(i0 to i1).map(plate -> _)
						}
					}
				})
				if (sError == null) {
					Success(list, res1.next)
				}
				else {
					Error(sError, res1.next)
				}
			case ns: NoSuccess => ns
		}
	}
	def plateWells2: Parser[List[Tuple2[Plate, Int]]] = rep1sep(plateWells2_sub2, ";") ^^
		{ ll => ll.flatMap(l => l) }
	
	def valVolumes_var: Parser[List[Double]] = ident ^^
		{ s =>
			if (mapVars.contains(s))
				List(mapVars(s).toDouble)
			else if (m_mapLists.contains(s))
				m_mapLists(s).map(_.toDouble)
			else {
				m_sError = ("Undefined variable: "+s)
				Nil
			}
		}
	def valVolumes_numeric: Parser[List[Double]] = floatingPointNumber ^^
		{ s => List(s.toDouble) }
	def valVolumes: Parser[List[Double]] = valVolumes_var | valVolumes_numeric
	
	def valInts_var: Parser[List[Int]] = ident ^^
		{ s =>
			if (mapVars.contains(s))
				List(mapVars(s).toInt)
			else if (m_mapLists.contains(s))
				m_mapLists(s).map(_.toInt)
			else {
				m_sError = ("Undefined variable: "+s)
				Nil
			}
		}
	def valInts_numeric: Parser[List[Int]] = integer ^^
		{ s => List(s.toInt) }
	def valInts: Parser[List[Int]] = valInts_var | valInts_numeric
	
	def getPlateAtLoc(sLoc: String, input: Input): Either[ParseResult[Nothing], Plate] = {
		// Do we already have a plate at the given location?
		if (!mapLocToPlate.contains(sLoc)) {
			// Try to create a new plate:
			carriers.get(sLoc) match {
				case Some(carrier) =>
					val plate = new Plate
					val pp = new PlateProxy(kb, plate)
					pp.setDimension(carrier.nCols, carrier.nRows)
					pp.location = sLoc
					mapLocToPlate(sLoc) = plate
				case None =>
			}
		}
		
		mapLocToPlate.get(sLoc) match {
			case Some(plate) =>
				m_contextPlate = Some(plate)
				Right(plate)
			case None =>
				Left(Error("Undefined location: "+sLoc, input))
		}
	}
	
	def getWell_?(plate: Plate, iWell: Int): Option[Well] = {
		kb.getPlateSetup(plate).dim_? match {
			case Some(dim) =>
				if (iWell < 0 || iWell >= dim.wells.size)
					return Some(dim.wells(iWell))
				else
					None
			case None =>
				None
		}
	}
	
	def getWell(plate: Plate, iRow: Int, iCol: Int, input: Input): Either[ParseResult[Nothing], Well] = {
		val pc = kb.getPlateSetup(plate)
		val dim = pc.dim_?.get
		val (nRows, nCols) = (dim.nRows, dim.nCols)
		if (iRow < 0 || iRow >= nRows) {
			Left(Error("Invalid row: plate has "+nRows+" rows, but row"+(iRow+1)+"was requested", input))
		}
		else if (iCol < 0 || iCol >= nCols) {
			Left(Error("Invalid column: plate has "+nCols+" columns, but column "+(iCol+1)+"was requested", input))
		}
		else {
			val iWell = iRow + iCol * nRows
			val well = dim.wells(iWell)
			Right(well)
		}
	}
	
	/*def getWells(plate: Plate, iRow: Int, iCol: Int, nWells: Int, input: Input): Either[ParseResult[Nothing], List[Well]] = {
		getWell(plate, iRow, iCol, input) match {
			case Left(e) => Left(e)
			case Right(well) =>
				val pd = kb.getPlateData(plate)
				val (nRows, nCols) = (pd.nRows.get, pd.nCols.get)
				val d = kb.getPartData(well)
				val i0 = d.index.get
				val i1 = i0 + nWells - 1
				if (i1 >= nRows * nCols) {
					Left(Error("Plate dimension exceeded", input))
				}
				else {
					val l = (i0 to i1).map(i => pd.wells.get.apply(i)).toList
					Right(l)
				}
		}
	}*/
	
	def getWells(plate: Plate, well0: Well, nWells: Int, input: Input): Either[ParseResult[Nothing], List[Well]] = {
		val pc = kb.getPlateSetup(plate)
		val dim = pc.dim_?.get
		val (nRows, nCols) = (dim.nRows, dim.nCols)
		val wc = kb.getWellSetup(well0)
		val i0 = wc.index_?.get
		val i1 = i0 + nWells - 1
		if (i1 >= nRows * nCols) {
			Left(Error("Plate dimension exceeded", input))
		}
		else {
			val l = (i0 to i1).map(i => dim.wells.apply(i)).toList
			Right(l)
		}
	}
	
	def getWells(plate: Plate, well0: Well, well1: Well): List[Well] = {
		val i0 = kb.getWellSetup(well0).index_?.get
		val i1 = kb.getWellSetup(well1).index_?.get
		val pc = kb.getPlateSetup(plate)
		(i0 to i1).map(pc.dim_?.get.wells.apply).toList
	}
	
	def setVar(id: String, s: String) { mapVars(id) = s }
	
	def setOption(id: String, value: Option[String]) { mapOptions(id) = value.getOrElse(null) }
	
	def setReagent(reagent: String, plate: Plate, iWell: Int, lc: String, nWells_? : Option[Int]) {
		//mapReagents(reagent) = new Reagent(reagent, rack, iWell, nWells_?.getOrElse(1), lc)
		
		// Create liquid with given name
		val liq = new Liquid(reagent, true, false, false, false, false)
		kb.addLiquid(liq)
		
		// Add liquid to wells
		val pc = kb.getPlateSetup(plate)
		val wells = pc.dim_?.get.wells
		for (well <- wells) {
			kb.getWellSetup(well).liquid_? = Some(liq)
		}
			
		mapLiquids(reagent) = liq
	}

	def setLabware(id: String, rack: String, name: String) { mapLabware(id) = (rack, name) }
	
	def addRunError(s: String) {
		m_lsScriptErrors += (m_sScriptLine -> s)
		println(s)
	}
	
	def addRunCommand(cmd: Command) {
		m_scriptCommands += cmd
		println(cmd)
	}
	
	def run_DIST_REAGENT2(liq: Liquid, wells: Seq[Tuple2[Plate, Int]], volumes: Seq[Double], sLiquidClass: String, opts_? : Option[String]) {
		if (wells.isEmpty) {
			addRunError("list of destination wells must be non-empty")
			return
		}
		if (volumes.isEmpty) {
			addRunError("list of volumes must be non-empty")
			return
		}
		if (volumes.size > 1 && wells.size != volumes.size) {
			addRunError("lists of wells and volumes must have the same dimensions")
			return
		}
		
		val wells2 = wells.map(pair => {
			val (plate, iWell) = pair
			val dim = kb.getPlateSetup(plate).dim_?.get
			dim.wells(iWell)
		})
		val wvs = {
			if (volumes.size > 1)
				wells2 zip volumes
			else
				wells2.map(_ -> volumes.head)
		}
		
		val items = wvs.map(pair => {
			val (well, nVolume) = pair
			new L3A_PipetteItem(WPL_Liquid(liq), WP_Well(well), nVolume)
		})
		val cmd = L3C_Pipette(items)
		addRunCommand(cmd)
	}
	
	def run_MIX_WELLS(plate: Plate, wells: List[Well], lnCount: List[Int], lnVolume: List[Double], sLiquidClass: String, opts_? : Option[String]) {
		println(plate)
		println(wells)
		println(lnVolume)
	}
	
	
	val cmd0List: Parser[String] = "LIST"~>ident 
	val cmd0Assign: Parser[Unit] = ident ~"="~ floatingPointNumber ^^
				{ case id ~"="~ s => setVar(id, s) }
	val cmds0 = Map[String, Parser[Unit]](
			("OPTION", ident~opt(word) ^^
				{ case id ~ value => setOption(id, value) }),
			("REAGENT", ident~idPlate~integer~ident~opt(integer) ^^
				{ case reagent ~ plate ~ iWell ~ lc ~ nWells_? => setReagent(reagent, plate, iWell, lc, nWells_?) }),
			("LABWARE", ident~ident~stringLiteral ^^
				{ case id ~ rack ~ name => setLabware(id, rack, name) })
			)
	
	val cmds2 = Map[String, Parser[Unit]](
			("DIST_REAGENT2", idLiquid~plateWells2~valVolumes~ident~opt(word) ^^
				{ case liquid ~ wells ~ vol ~ lc ~ opts_? => run_DIST_REAGENT2(liquid, wells, vol, lc, opts_?) }),
			("MIX_WELLS", idPlate~idWells~valInts~valVolumes~ident~opt(word) ^^
				{ case plate ~ wells ~ lnCount ~ lnVolume ~ lc ~ opts_? => run_MIX_WELLS(plate, wells, lnCount, lnVolume, lc, opts_?) })
			)
			
	//----------------------------------------------
	
	object Section extends Enumeration {
		val Config, ConfigList, Doc, Script = Value
	}

	private var m_section = Section.Config
	private var m_asDoc: List[String] = Nil
	private var m_asList = new ArrayBuffer[String]
	private var m_sListName: String = null
	private var m_sError: String = null
	//private val m_mapVars = new HashMap[String, String]
	private val m_mapLists = new HashMap[String, List[String]]
	private var m_map31: ObjMapper = null
	private var m_sScriptLine: String = null
	private val m_lsScriptErrors = new ArrayBuffer[Tuple2[String, String]]()
	private val m_scriptCommands = new ArrayBuffer[Command]()
	
	def mapLists = m_mapLists.asInstanceOf[scala.collection.Map[String, List[String]]]
	
	
	def parse(sSource: String) {
		// Clear variables
		m_section = Section.Config
		m_asDoc = Nil
		m_asList.clear
		m_sError = null
		m_mapLists.clear
		m_lsScriptErrors.clear()
		m_scriptCommands.clear()
		
		for (sLine <- sSource.lines) {
			val s = sLine.replaceAll("#.*", "").trim
			println(s)
			s match {
				case "" =>
				case "DOC" =>
					m_section = Section.Doc
				case "SCRIPT" =>
					kb.concretize() match {
						case Left(errors) =>
							println("Errors:")
							errors.foreach(println)
							return
						case Right(map31) =>
							m_map31 = map31
					}
					m_section = Section.Script
				case _ =>
					m_section match {
						case Section.Doc => handleDoc(s)
						case Section.Config => handleConfig(s)
						case Section.ConfigList => handleConfigList(s)
						case Section.Script => handleScript(s)
					}
			}
			if (m_sError != null) {
				println(m_sError)
				m_sError = null
			}
		}
	}
	
	private def handleDoc(s: String) {
		if (s == "ENDDOC") {
			m_asDoc = m_asDoc.reverse
			m_section = Section.Config
		}
		else
			m_asDoc = s :: m_asDoc
	}
	
	private def handleConfig(sLine: String) {
		val rAssign = parseAll(cmd0Assign, sLine)
		val rList = parseAll(cmd0List, sLine)
		if (rAssign.successful) {
			// Do nothing, because already handled by parser
		}
		else if (rList.successful) {
			m_asList.clear
			m_sListName = rList.get
			m_section = Section.ConfigList
		}
		else {
			var bFound = false
			val rCmd = parse(word, sLine)
			if (rCmd.successful) {
				val sCmd: String = rCmd.get
				cmds0.get(sCmd) match {
					case None =>
						m_sError = "Unrecognized command: " + sCmd
					case Some(p) =>
						val r = parseAll(p, rCmd.next)
						if (!r.successful)
							m_sError = r.toString
						else
							bFound = true
				}
			}
			else {
				m_sError = "Unrecognized line: " + sLine
			}
		}
	}
	
	/*private def findFirstMatch(s: String, cmds: List[Parser[Any]]): Boolean = cmds match {
		case Nil => false
		case cmd :: rest => val r = parseAll(cmd, s)
			if (r.successful)
				true
			else
				findFirstMatch(s, rest)
	}*/

	private def handleConfigList(s: String) {
		if (s == "ENDLIST") {
			m_mapLists(m_sListName) = m_asList.toList
			m_section = Section.Config
		}
		else
			m_asList += s
	}
	
	private def handleScript(sLine: String) {
		if (sLine == "ENDSCRIPT")
			m_section = Section.Config
		else {
			m_sScriptLine = sLine
			var bFound = false
			val rCmd = parse(word, sLine)
			if (rCmd.successful) {
				val sCmd: String = rCmd.get
				println("sCmd = "+sCmd)
				cmds2.get(sCmd) match {
					case None =>
						m_sError = "Unrecognized command: " + sCmd
					case Some(p) =>
						val r = parseAll(p, rCmd.next)
						if (!r.successful)
							m_sError = r.toString
						else
							bFound = true
				}
			}
			else {
				m_sError = "Unrecognized line: " + sLine
			}
		}
	}
	
	def DefineRack(name: String, grid: Int, site: Int, xsize: Int, ysize: Int, nVolumeMax: Double, carrierType: String = "") {
		val carrier = Carrier(
				name, xsize, ysize, grid, site, nVolumeMax, carrierType
				)
		carriers(name) = carrier
	}
}
