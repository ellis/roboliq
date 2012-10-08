package roboliq.utils0.infinitm200

import java.io.File
import java.io.FileInputStream

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

import org.apache.commons.io.FileUtils
import org.yaml.snakeyaml.Yaml
import com.google.gson.Gson


class SpecBean {
	@BeanProperty var file: String = null
	@BeanProperty var id: String = null
	@BeanProperty var date: String = null
	@BeanProperty var script: String = null
	@BeanProperty var site: String = null
	@BeanProperty var plateModel: String = null
	@BeanProperty var liquidClass: String = null
	@BeanProperty var baseVol: String = null
	@BeanProperty var baseConc: String = null
	@BeanProperty var vol: String = null
	@BeanProperty var conc: String = null
	@BeanProperty var tip: String = null
	@BeanProperty var tipVol: String = null
	@BeanProperty var multipipette: String = null
}

object InfinitM200 {
	case class Config(
		filename: String = "",
		outfile: String = null,
		json: Boolean = false,
		tab: Boolean = false
	)

	val parser = new scopt.immutable.OptionParser[Config]("roboliq-infinitm200-parser", "1.0") {
		def options = Seq(
			opt("o", "output", "Output filename") { (s, c) => c.copy(outfile = s) },
			flag("json", "JSON output") { (c: Config) => c.copy(json = true) },
			flag("tab", "Tab output") { (c: Config) => c.copy(tab = true) },
			arg("SPECFILE", "A .yaml file with specs.") { (s, c) => c.copy(filename = s) }
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
	
	case class RowBean(
		id: String,
		date: String,
		script: String,
		site: String,
		plateModel: String,
		liquidClass: String,
		baseVol: java.math.BigDecimal,
		baseConc: java.math.BigDecimal,
		vol: java.math.BigDecimal,
		conc: java.math.BigDecimal,
		tip: Int,
		tipVol: java.math.BigDecimal,
		multipipette: Boolean,
		row: Int,
		col: Int,
		readout: java.math.BigDecimal
	)
	
	object RowBean {
		def apply(r: Row): RowBean = {
			new RowBean(r.id, r.date, r.script, r.site, r.plateModel, r.liquidClass,
					r.baseVol.bigDecimal, r.baseConc.bigDecimal, r.vol.bigDecimal, r.conc.bigDecimal, r.tip, r.tipVol.bigDecimal, r.multipipette, r.row, r.col, r.readout.bigDecimal)
		}
	}
	
	def main(args: Array[String]) {
		val c = parser.parse(args, new Config) match {
			case Some(config) => config
			case _ => sys.exit()
		}
		
		val yaml = new Yaml
		val gson = new Gson

		val file = new File(c.filename)
		val dir = file.getParentFile
		if (!file.exists) {
			println("Could not find file `"+c.filename+"`.")
			sys.exit()
		}
		val input = yaml.loadAll(new FileInputStream(file))

		val replSettings = new scala.tools.nsc.Settings
		replSettings.embeddedDefaults[Config] // Arbitrary class
		val repl = new scala.tools.nsc.interpreter.IMain(replSettings)
		
		val rowAll_l = input.flatMap(o => {
			val spec = o.asInstanceOf[SpecBean]
			val row_l = handleSpec(c, dir, spec, repl)
		
			row_l.foreach(println)
			
			if (c.json) {
				val outfile = new File(dir, spec.id+".json")
				val rowBean_l: java.util.Collection[RowBean] = asJavaCollection(row_l.map(RowBean.apply))
				val out_s = gson.toJson(rowBean_l)
				FileUtils.writeStringToFile(outfile, out_s)
			}
			if (c.tab) {
				val outfile = new File(dir, spec.id+".tab")
				val header_s = "id\tdate\tscript\tsite\tplateModel\tliquidClass\tbaseVol\tbaseConc\tvol\tconc\ttip\ttipVol\tmultipipette\trow\tcol\treadout"
				val out_l = row_l.map(row1 => {
					import row1._
					s"$id\t$date\t$script\t$site\t$plateModel\t$liquidClass\t$baseVol\t$baseConc\t$vol\t$conc\t$tip\t$tipVol\t$multipipette\t$row\t$col\t$readout"
				})
				FileUtils.writeLines(outfile, header_s :: out_l)
			}
			
			row_l
		})
		
		if (c.outfile != null) {
			val row_l = rowAll_l.toList
			if (c.json) {
				val outfile = new File(dir, c.outfile)
				val rowBean_l: java.util.Collection[RowBean] = asJavaCollection(row_l.map(RowBean.apply))
				val out_s = gson.toJson(rowBean_l)
				FileUtils.writeStringToFile(outfile, out_s)
			}
			if (c.tab) {
				val outfile = new File(dir, c.outfile)
				val header_s = "id\tdate\tscript\tsite\tplateModel\tliquidClass\tbaseVol\tbaseConc\tvol\tconc\ttip\ttipVol\tmultipipette\trow\tcol\treadout"
				val out_l = row_l.map(row1 => {
					import row1._
					s"$id\t$date\t$script\t$site\t$plateModel\t$liquidClass\t$baseVol\t$baseConc\t$vol\t$conc\t$tip\t$tipVol\t$multipipette\t$row\t$col\t$readout"
				})
				FileUtils.writeLines(outfile, header_s :: out_l)
			}
		}
	}
	
	def handleSpec(
		c: Config,
		dir: File,
		spec: SpecBean,
		repl: scala.tools.nsc.interpreter.IMain
	): List[Row] = {
		val row0 = Row(
			id = spec.id,
			date = spec.date,
			script = spec.script,
			site = spec.site,
			plateModel = spec.plateModel,
			liquidClass = spec.liquidClass,
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
			def getBaseVol(row: Int, col: Int): BigDecimal = { """+Option(spec.baseVol).getOrElse("0")+""" }
			def getBaseConc(row: Int, col: Int): BigDecimal = { """+Option(spec.baseConc).getOrElse("0")+""" }
			def getVol(row: Int, col: Int): BigDecimal = { """+Option(spec.vol).getOrElse("0")+""" }
			def getConc(row: Int, col: Int): BigDecimal = { """+Option(spec.conc).getOrElse("0")+""" }
			def getTip(row: Int, col: Int): Int = { """+Option(spec.tip).getOrElse("1")+""" }
			def getTipVol(row: Int, col: Int): BigDecimal = { """+Option(spec.tipVol).getOrElse("0")+""" }				
			def getMultipipette(row: Int, col: Int): Boolean = { """+Option(spec.multipipette).getOrElse("false")+""" }				
		""")

		val xml = scala.xml.XML.loadFile(new File(dir, spec.file))
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
				val (row, col, _) = tuple.asInstanceOf[(Int, Int, String)]
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
		row_l
	}
}
