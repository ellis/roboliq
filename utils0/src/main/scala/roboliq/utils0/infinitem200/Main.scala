package roboliq.utils0.infinitm200

import java.io.File
import java.io.FileInputStream

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty

import org.apache.commons.io.FileUtils
import org.yaml.snakeyaml.Yaml
import com.google.gson.Gson


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
	
	val field_l = List("id", "date", "script", "site", "plateModel", "liquidClass", "baseVol", "baseConc", "vol", "conc", "tip", "tipVol", "multipipette", "row", "col", "readout")
	val fieldDefault_m = Map(
			"baseVol" -> "0",
			"baseConc" -> "0",
			"multipipette" -> "false",
			"row" -> "row",
			"col" -> "col",
			"readout" -> "0"
	)
	
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
		val input = yaml.load(new FileInputStream(file)).asInstanceOf[java.util.Map[String, Any]]

		val replSettings = new scala.tools.nsc.Settings
		replSettings.embeddedDefaults[Config] // Arbitrary class
		val repl = new scala.tools.nsc.interpreter.IMain(replSettings)
		repl.bind("field_l", field_l)
		
		val rowAll_l = input.flatMap(pair => {
			val (filename, map0) = pair.asInstanceOf[(String, java.util.Map[String, Any])]
			val map = fieldDefault_m ++ map0.toMap.mapValues(_.toString)
			//println("map: "+map)
			val row_l = handleSpec(c, dir, filename, map, repl)
		
			row_l.foreach(println)
			
			/*if (c.json) {
				val outfile = new File(dir, map("id")+".json")
				val rowBean_l: java.util.Collection[RowBean] = asJavaCollection(row_l.map(RowBean.apply))
				val out_s = gson.toJson(rowBean_l)
				FileUtils.writeStringToFile(outfile, out_s)
			}*/
			if (c.tab) {
				val outfile = new File(dir, map("id")+".tab")
				val header_s = field_l.mkString("\t")
				val out_l = row_l.map(row => field_l.map(row).mkString("\t"))
				FileUtils.writeLines(outfile, header_s :: out_l)
			}
			
			row_l
		})
		
		if (c.outfile != null) {
			val row_l = rowAll_l.toList
			/*
			if (c.json) {
				val outfile = new File(dir, c.outfile)
				val rowBean_l: java.util.Collection[RowBean] = asJavaCollection(row_l.map(RowBean.apply))
				val out_s = gson.toJson(rowBean_l)
				FileUtils.writeStringToFile(outfile, out_s)
			}
			*/
			if (c.tab) {
				val outfile = new File(dir, c.outfile)
				val header_s = field_l.mkString("\t")
				val out_l = row_l.map(row => field_l.map(row).mkString("\t"))
				FileUtils.writeLines(outfile, header_s :: out_l)
			}
		}
	}
	
	def handleSpec(
		c: Config,
		dir: File,
		filename: String,
		field_m: Map[String, String],
		repl: scala.tools.nsc.interpreter.IMain
	): List[Map[String, String]] = {
		val map = field_m
		repl.interpret(
			"val fmap: Map[String, (Int, Int) => String] = Map(\n" +
			field_l.map(name => "(\""+name+"\", (row, col) => ("+field_m(name)+").toString)").mkString(",\n") +
			"\n)")

		val xml = scala.xml.XML.loadFile(new File(dir, filename))
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
				fmap.mapValues(f => f(row, col))
			}
		""")

		val l = repl.valueOfTerm("l").get.asInstanceOf[List[Map[String, String]]]
		
		val row_l = (rowColVal_l zip l).map(pair => {
			val (row_i, col_i, value_s) = pair._1
			val map0 = pair._2
			map0 ++ List("row" -> row_i.toString, "col" -> col_i.toString, "readout" -> value_s)
		})
		row_l
	}
}
