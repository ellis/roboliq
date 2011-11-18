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
	
	def idPlate: Parser[Plate] = idHandler(getPlateAndAssignContext)
	
	val idRowCol: Parser[Tuple2[Int, Int]] = "[A-Z]".r ~ integer ^^ { case sRow ~ nCol => {
		val iRow = sRow.charAt(0) - 'A'
		val iCol = nCol - 1
		(iRow, iCol)
	}}
	
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
			case Right(n, _) =>
				m_contextPlate match {
					case None => Failure("Unknown parent plate", input)
					case Some(plate) =>
						getWell_?(plate, n) match {
							case Some(well) => Right(well, res1.next)
							case None => Failure("Invalid well index: "+n, input)
						}
				}
			case ns: NoSuccess => ns
		}
	}*/
	
	/*def idLiquid: Parser[Reagent] = Parser[Reagent] { input =>
		val res1 = ident.apply(input)
		res1 match {
			case Success(sLiq, _) =>
				shared.mapReagents.get(sLiq) match {
					case Some(liq) => Success(liq, res1.next)
					case None => Error("Undefined reagent: "+sLiq, input)
				}
			case ns: NoSuccess => ns
		}
	}*/
	
	def idList: Parser[List[String]] = idHandler(shared.getList)
	def idMixDef: Parser[MixDef] = idHandler(shared.getMixDef)
	def idRack: Parser[Rack] = idHandler(shared.getRack)
	def idReagent: Parser[Reagent] = idHandler(shared.getReagent)
	
	private def idHandler[T](f: String => Result[T]): Parser[T] = Parser[T] { input =>
		val res1 = ident.apply(input)
		res1 match {
			case ns: NoSuccess => ns
			case Success(id, _) =>
				f(id) match {
					case RError(lsError) => Error(lsError.mkString("; "), input)
					case RSuccess(o) => Success(o, res1.next)
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
	def plateWells2: Parser[List[Tuple2[Plate, Int]]] = rep1sep(plateWells2_sub2, ";") ^^
		{ ll => ll.flatMap(l => l) }
	
	def valVolume_var: Parser[Double] = ident ^^
		{ s =>
			if (shared.mapVars.contains(s))
				shared.mapVars(s).toDouble
			else {
				shared.addError("Undefined variable: "+s)
				0
			}
		}
	def valVolume_numeric: Parser[Double] = floatingPointNumber ^^
		{ s => s.toDouble }
	def valVolume: Parser[Double] = valVolume_var | valVolume_numeric
	
	def valVolumes_var: Parser[List[Double]] = ident ^^
		{ s =>
			if (shared.mapVars.contains(s))
				List(shared.mapVars(s).toDouble)
			else if (shared.mapLists.contains(s))
				shared.mapLists(s).map(_.toDouble)
			else {
				shared.addError("Undefined variable: "+s)
				Nil
			}
		}
	def valVolumes_numeric: Parser[List[Double]] = floatingPointNumber ^^
		{ s => List(s.toDouble) }
	def valVolumes: Parser[List[Double]] = valVolumes_var | valVolumes_numeric
	
	def valInt_var: Parser[Int] = ident ^^
		{ s =>
			if (shared.mapVars.contains(s))
				shared.mapVars(s).toInt
			else {
				shared.addError("Undefined variable: "+s)
				0
			}
		}
	def valInt_numeric: Parser[Int] = integer ^^
		{ s => s.toInt }
	def valInt: Parser[Int] = valInt_var | valInt_numeric
	
	def valInts_var: Parser[List[Int]] = ident ^^
		{ s =>
			if (shared.mapVars.contains(s))
				List(shared.mapVars(s).toInt)
			else if (shared.mapLists.contains(s))
				shared.mapLists(s).map(_.toInt)
			else {
				shared.addError("Undefined variable: "+s)
				Nil
			}
		}
	def valInts_numeric: Parser[List[Int]] = integer ^^
		{ s => List(s.toInt) }
	def valInts: Parser[List[Int]] = valInts_var | valInts_numeric
	
	def getPlate(rack: Rack): Result[Plate] = {
		shared.mapRackToPlate.get(rack) match {
			case Some(plate) => RSuccess(plate)
			case None => createPlate(rack.id, rack, None)
		}
	}
		
	def getPlateAndAssignContext(sLoc: String): Result[Plate] = {
		for { plate <- (shared.getRack(sLoc) >>= getPlate) }
		yield {
			m_contextPlate = Some(plate)
			plate
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
	
	def toDouble(s: String): Result[Double] = {
		try { RSuccess(s.toDouble) } catch { case _ => RError("expected a numeric value: "+s) }
	}
}
