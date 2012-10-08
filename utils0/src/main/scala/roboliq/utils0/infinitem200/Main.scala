package roboliq.utils0.infinitm200

object InfinitM200 {
	case class Config(
		filename: Option[String] = None,
		tipExpr: Option[String] = None,
		field_l: List[String] = Nil,
		row: Row = Row("", "", "", "", "", "", 0, 0, 0, 0, 0, 0, false, 0, 0, 0),
		field_m: Map[String, String] = Map()
	)

	val parser = new scopt.immutable.OptionParser[Config]("roboliq-infinitm200-parser", "1.0") {
		def options = Seq(
			opt("i", "input", "Filename to take as input.") { (s, c) => c.copy(filename = Some(s)) },
			opt("tip", "Expression for tip ID.") { (s, c) => c.copy(tipExpr = Some(s)) },
			opt("site", "Name of site.") { (s, c) => c.copy(row = c.row.copy(site = s)) },
			keyValueOpt("f", "field", "Field") { (k, v, c) => c.copy(field_m = c.field_m + (k -> v)) },
			arglist("<fields>", "Default values for required fields.") { (s, c) => c.copy(field_l = c.field_l ++ List(s)) }
		)
	}
	
	case class Row(
		id: String,
		date: String,
		script: String,
		site: String,
		plateModel: String,
		liquidClass: String,
		baseVol: BigDecimal,
		baseConc: BigDecimal,
		vol: BigDecimal,
		conc: BigDecimal,
		tip: Int,
		tipVol: BigDecimal,
		multipipette: Boolean,
		row: Int,
		col: Int,
		readout: BigDecimal
	)

	def main(args: Array[String]) {
		val c = parser.parse(args, new Config) match {
			case Some(config) => config
			case _ => sys.exit()
		}
		
		println("Hello")
		
		val replSettings = new scala.tools.nsc.Settings
		replSettings.embeddedDefaults[Config] // Arbitrary class
		val repl = new scala.tools.nsc.interpreter.IMain(replSettings)

		for {
			filename <- c.filename
			tipExpr <- c.tipExpr
		} {
			val row0 = Row(
				id = c.field_l(0),
				date = c.field_l(1),
				script = c.field_l(2),
				site = c.field_l(3),
				plateModel = c.field_l(4),
				liquidClass = c.field_l(5),
				baseVol = 0,
				baseConc = 0,
				vol = 0,
				conc = 0,
				tip = 0,
				tipVol = 0,
				multipipette = false,
				row = 0,
				col = 0,
				readout = 0
			)
			
			repl.interpret("""
				def getBaseVol(row: Int, col: Int): BigDecimal = { """+c.field_m.get("baseVol").getOrElse("0")+""" }
				def getBaseConc(row: Int, col: Int): BigDecimal = { """+c.field_m.get("baseConc").getOrElse("0")+""" }
				def getVol(row: Int, col: Int): BigDecimal = { """+c.field_m.get("vol").getOrElse("0")+""" }
				def getConc(row: Int, col: Int): BigDecimal = { """+c.field_m.get("conc").getOrElse("0")+""" }
				def getTip(row: Int, col: Int): Int = { """+tipExpr+""" }
				def getTipVol(row: Int, col: Int): BigDecimal = { """+c.field_m.get("tipVol").getOrElse("0")+""" }				
				def getMultipipette(row: Int, col: Int): Boolean = { """+c.field_m.get("multipipette").getOrElse("false")+""" }				
			""")

			val xml = scala.xml.XML.loadFile(filename)
			//println((xml \\ "Well"))
			val well_l = xml \\ "Well"
			val rowColVal_l = for {
				well <- well_l.toList
				pos_s = (well \ "@Pos").text
				if (!pos_s.isEmpty)
				single_l = (well \ "Single")
				
			} yield {
				//println(s"${pos_s} ${single_l.last.text}")
				val row_i = (pos_s(0) - 'A') + 1
				val col_i = pos_s.tail.toInt
				(row_i, col_i, single_l.last.text)
			}
			
			repl.bind("rowColVal_l", rowColVal_l)
			
			repl.interpret("""
				val l = for (tuple <- rowColVal_l) yield {
					val (row, col, value_s) = tuple.asInstanceOf[(Int, Int, String)]
					(
						getBaseVol(row, col),
						getBaseConc(row, col),
						getVol(row, col),
						getConc(row, col),
						getTip(row, col),
						getTipVol(row, col),
						getMultipipette(row, col)
					)
				}
			""")

			val l = repl.valueOfTerm("l").get.asInstanceOf[List[(BigDecimal, BigDecimal, BigDecimal, BigDecimal, Int, BigDecimal, Boolean)]]
			
			val row_l = (rowColVal_l zip l).map(pair => {
				val (row_i, col_i, value_s) = pair._1
				val tuple = pair._2
				row0.copy(
					baseVol = tuple._1,
					baseConc = tuple._2,
					vol = tuple._3,
					conc = tuple._4,
					tip = tuple._5,
					tipVol = tuple._6,
					multipipette = tuple._7,
					row = row_i,
					col = col_i,
					readout = BigDecimal(value_s)
				)
			})
			
			row_l.foreach(println)
		}
	}
}
