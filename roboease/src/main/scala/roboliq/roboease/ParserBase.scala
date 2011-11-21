package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.util.parsing.combinator._

import roboliq.common._
import roboliq.common.{Error => RError, Success => RSuccess}
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


class ParserBase(shared: ParserSharedData) extends JavaTokenParsers {
	val kb = shared.kb
	
	private var m_contextPlate: Option[Plate] = None

	val word: Parser[String] = """[^\s]+""".r
	val integer: Parser[Int] = """[0-9]+""".r ^^ (_.toInt)
	val restOfLine: Parser[String] = """.*""".r
	val string: Parser[String] = "\"" ~> """[^"]*""".r <~ '"'
	def numDouble: Parser[Double] = floatingPointNumber ^^ (_.toDouble)
	def numInt: Parser[Int] = """[0-9]+""".r ^^ (_.toInt)
	
	val idRowCol: Parser[Tuple2[Int, Int]] = "[A-Z]".r ~ integer ^^ { case sRow ~ nCol => {
		val iRow = sRow.charAt(0) - 'A'
		val iCol = nCol - 1
		(iRow, iCol)
	}}
	
	private def idWell_index = Parser[Well] { input =>
		val res1 = integer.apply(input)
		res1 match {
			case Success(n, _) =>
				m_contextPlate match {
					case None => Failure("Unknown parent plate", input)
					case Some(plate) =>
						getWell(plate, n) match {
							case RSuccess(well) => Success(well, res1.next)
							case RError(lsError) => Error(lsError.mkString("; "), input)
						}
				}
			case ns: NoSuccess => ns
		}
	}
	
	private def idWell_rowCol = Parser[Well] { input =>
		val res1 = ("[A-Z]".r~integer).apply(input)
		res1 match {
			case Success(sRow ~ nCol, _) =>
				m_contextPlate match {
					case None => Failure("Unknown parent plate", input)
					case Some(plate) =>
						val iRow = sRow.charAt(0) - 'A'
						val iCol = nCol - 1
						getWell(plate, iRow, iCol) match {
							case RSuccess(well) => Success(well, res1.next)
							case RError(lsError) => Error(lsError.mkString("; "), input)
						}
				}
			case ns: NoSuccess => ns
		}
	}
	
	private def idWell_sub: Parser[Well] = idWell_index | idWell_rowCol
	
	/*def idWells_index = Parser[List[Well]] { input =>
		val res1 = idWell_index.apply(input)
		res1 match {
			case Right(well0, input2) =>
				val plate = m_contextPlate.get
				val res2 = ("-"~>idWell_index).apply(input2)
				res2 match {
					case Right(well1, _) =>
						val wells = getWells(plate, well0, well1)
						Right(wells, res2.next)
					case ns: NoSuccess =>
						Right(List(well0), res1.next)
				}
			case ns: NoSuccess => ns
		}
	}*/
	
	private def idWells_sub = Parser[List[Well]] { input =>
		val res1 = idWell_sub.apply(input)
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
	def idWells: Parser[List[Well]] = directOrVar(idWells_sub)
	
	def idList: Parser[List[String]] = idHandler(shared.getList)
	def idMixDef: Parser[MixDef] = idHandler(shared.getMixDef)
	def idRack: Parser[Rack] = idHandler(shared.getRack)
	def idReagent: Parser[Reagent] = idHandler(shared.getReagent)
	def idPlate: Parser[Plate] = idHandler(getPlateAndAssignContext)
	def idWell: Parser[Well] = idHandler(getWell)
	
	private def idHandler[T](f: String => Result[T]): Parser[T] = Parser[T] { input =>
		val res1 = ident.apply(input)
		res1 match {
			case ns: NoSuccess => ns
			case Success(id, _) =>
				val s = shared.subst(id)
				f(s) match {
					case RError(lsError) => Error(lsError.mkString("; "), input)
					case RSuccess(o) => Success(o, res1.next)
				}
		}
	}
	
	private def directOrVar[T](parser: Parser[T]): Parser[T] = Parser[T] { input =>
		val res1 = parser.apply(input)
		res1 match {
			case Success(output, _) => Success(output, res1.next)
			case ns1: NoSuccess =>
				val res2 = word.apply(input)
				res2 match {
					case Success(id, _) =>
						val sValue = shared.mapSubstitutions.get(id) match {
							case Some(id2) =>
								shared.mapVars.get(id2) match {
									case Some(s) => s
									case None => id2
								}
							case None =>
								shared.mapVars.get(id) match {
									case Some(s) => s
									case None => null
								}
						}
						if (sValue == null) {
							Error("unknown variable \""+id+"\"", input)
						}
						else {
							this.parseAll(parser, sValue) match {
								case Success(output, _) => Success(output, res2.next)
								case ns3: NoSuccess => ns3
							}
						}
					case ns2: NoSuccess => ns1
				}
		}
	}
	
	private def directOrVarOrList[T](parser: Parser[T]): Parser[List[T]] = Parser[List[T]] { input =>
		val res1 = directOrVar(parser).apply(input)
		res1 match {
			case Success(output, next) => Success(List(output), next)
			case _ =>
				val res2 = word.apply(input)
				res2 match {
					case Success(id, _) =>
						val sList = shared.subst(id)
						shared.mapLists.get(sList) match {
							case None => Error("unknown list \""+sList+"\"", input)
							case Some(ls) =>
								Result.mapOver(ls)(sValue => {
									this.parseAll(parser, sValue) match {
										case Success(output, _) => RSuccess(output)
										case ns2: NoSuccess => RError("error in list \""+sList+"\" item \""+sValue+"\": "+ns2.msg)
									}
								}) match {
									case RSuccess(l) => Success(l, res2.next)
									case RError(lsError) => Error(lsError.mkString("; "), input)
								}
						}
					case ns1: NoSuccess => ns1
				}
		}
	}
	
	/** Return list of row/column tuples */
	def plateWells2_sub0A: Parser[Tuple3[Char, Int, Int]] = "[A-Z]".r~integer~"+"~integer ^^
		{ case sRow~n0~"+"~n1 => (sRow.charAt(0), n0, n1) }
	def plateWells2_sub0B: Parser[Tuple3[Char, Int, Int]] = "[A-Z]".r~integer ^^
		{ case sRow~n0 => (sRow.charAt(0), n0, 1) }
	def plateWells2_sub0: Parser[Tuple3[Char, Int, Int]] = plateWells2_sub0A | plateWells2_sub0B
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
							sError = ("Exceeds plate dimension of "+nRows+"x"+nCols+": "+pair._1+pair._2+"+"+pair._3)
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
	def plateWells2_sub3: Parser[List[Tuple2[Plate, Int]]] = rep1sep(plateWells2_sub2, ";") ^^
		{ ll => ll.flatMap(l => l) }
	def plateWells2: Parser[List[Tuple2[Plate, Int]]] = directOrVar(plateWells2_sub3)
	
	def valVolume: Parser[Double] = directOrVar(numDouble)
	def valVolumes: Parser[List[Double]] = directOrVarOrList(numDouble)
	
	def valInt: Parser[Int] = directOrVar(numInt)
	def valInts: Parser[List[Int]] = directOrVarOrList(numInt)
	
	protected def parseWells(sWells0: String): Result[List[Well]] = {
		val sWells = shared.subst(sWells0)
		if (shared.mapReagents.contains(sWells)) {
			val reagent = shared.mapReagents(sWells)
			RSuccess(reagent.wells.toList)
		}
		else {
			parse(plateWells2, sWells) match {
				case Success(lPI, _) => RSuccess(getWells(lPI))
				case ns: NoSuccess => RError(ns.msg)
			}
		}
	}
	
	private def getPlate(): Result[Plate] = {
		for (plate <- Result.get(m_contextPlate, "Unknown parent plate"))
		yield plate
	}
	
	def getPlate(rack: Rack): Result[Plate] = {
		shared.mapRackToPlate.get(rack) match {
			case Some(plate) => RSuccess(plate)
			case None => createPlate(rack.id, rack, None)
		}
	}
		
	def getPlateAndAssignContext(sLoc0: String): Result[Plate] = {
		val sLoc = shared.subst(sLoc0)
		for { plate <- (shared.getRack(sLoc) >>= getPlate) }
		yield {
			m_contextPlate = Some(plate)
			plate
		}
	}
	
	private def getWell(s0: String): Result[Well] = {
		val s = shared.subst(s0)
		if (s.isEmpty) return RError("empty string")
		val c = s.charAt(0)
		if (c.isDigit) {
			for {
				iWell1 <- toInt(s)
				well <- getWell(iWell1 - 1)
			} yield well
		}
		else if (c >= 'A' && c <= 'Z') {
			for {
				nRow <- toInt(s.substring(1))
				plate <- getPlate()
				val iCol = (c - 'A').asInstanceOf[Int]
				well <- getWell(plate, nRow - 1, iCol)
			} yield {
				well
			}
		}
		else {
			RError("could not be interpreted as a well identifier")
		}
	}
	
	private def getWell(iWell: Int): Result[Well] = {
		for {
			plate <- getPlate()
			well <- getWell(plate, iWell)
		} yield well
	}

	def getWell(plate: Plate, iWell: Int): Result[Well] = {
		for {
			dim <- Result.get(kb.getPlateSetup(plate).dim_?, "INTERNAL: no dimension information for plate "+plate)
			_ <- Result.assert(iWell >= 0 && iWell < dim.wells.size, "Invalid well index: "+(iWell+1))
		} yield dim.wells(iWell)
	}
	
	def getWell(plate: Plate, iRow: Int, iCol: Int): Result[Well] = {
		val pc = kb.getPlateSetup(plate)
		val dim = pc.dim_?.get
		val (nRows, nCols) = (dim.nRows, dim.nCols)
		if (iRow < 0 || iRow >= nRows) {
			RError("Invalid row: plate has "+nRows+" rows, but row"+(iRow+1)+"was requested")
		}
		else if (iCol < 0 || iCol >= nCols) {
			RError("Invalid column: plate has "+nCols+" columns, but column "+(iCol+1)+"was requested")
		}
		else {
			val iWell = iRow + iCol * nRows
			val well = dim.wells(iWell)
			RSuccess(well)
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
	
	def getWell(pi: Tuple2[Plate, Int]): Well = {
		val (plate, iWell) = pi
		val dim = kb.getPlateSetup(plate).dim_?.get
		dim.wells(iWell)
	}
	
	def getWells(l: Seq[Tuple2[Plate, Int]]): List[Well] = l.map(getWell).toList
	
	def createPlate(id: String, rack: Rack, sModel_? : Option[String]): Result[Plate] = {
		val plate = new Plate
		val pp = new PlateProxy(kb, plate)
		pp.label = id
		pp.setDimension(rack.nCols, rack.nRows)
		pp.location = rack.id
		shared.mapRackToPlate(rack) = plate
		pp.wells.foreach(well => {
			val setup = kb.getWellSetup(well)
			setup.sLabel_? = Some(id+":"+(setup.index_?.get+1))
		})
		
		// Try to assign plate model, if one was specified
		sModel_?.foreach(sModel => {
			if (!shared.mapPlateModel.contains(sModel)) {
				return RError("undefined labware \""+sModel+"\"")
			}
			val setup = kb.getPlateSetup(plate)
			setup.model_? = Some(shared.mapPlateModel(sModel))
		})
		
		RSuccess(plate)
	}
	
	def toInt(s: String): Result[Int] = {
		try { RSuccess(s.toInt) } catch { case _ => RError("expected an integer value: "+s) }
	}
	
	def toDouble(s: String): Result[Double] = {
		try { RSuccess(s.toDouble) } catch { case _ => RError("expected a numeric value: "+s) }
	}
}
