/*package roboliq.labs.bsse

import java.io.File
import java.io.PrintWriter

import scala.collection.JavaConversions._
import scala.collection.mutable.Stack

import com.github.rjeschke.txtmark

import roboliq.core._
import roboliq.robots.evoware.EvowareScript
import roboliq.utils.FileUtils


private class Html(p: PrintWriter) {
	val tags = new Stack[String]
	
	def docPrintln(o: Any) {
		val sIndent = "\t" * tags.size
		p.println(sIndent+o)
	}
	
	def tagPush(tag: String, attributes: Map[String, String] = Map()) {
		val s0 = "<"+tag;
		val s1 = {
			if (attributes.isEmpty) ""
			else " "+attributes.map(pair => pair._1+"=\""+pair._2+"\"").mkString(" ")
		}
		docPrintln(s0+s1+">")
		tags.push(tag)
	}
	
	def tagPop() {
		if (!tags.isEmpty) {
			docPrintln("</"+tags.pop()+">")
		}
	}
	
	def tagMake(tag: String, attributes: Map[String, String] = Map(), contents: String = null) {
		val s0 = "<"+tag;
		val s1 = {
			if (attributes.isEmpty) ""
			else " "+attributes.map(pair => pair._1+"=\""+pair._2+"\"").mkString(" ")
		}
		val s2 = {
			if (contents == null) "/>"
			else ">"+contents+"</"+tag+">";
		}
		docPrintln(s0+s1+s2)
	}
	
	def docHeader(sTitle: String, iLevel: Int) {
		tagMake("h"+iLevel, Map(), sTitle)
	}
}


class EvowareDoc {
	var sProtocolFilename: String = null
	var lNode: List[CmdNodeBean] = null
	var processorResult: ProcessorResult = null
	var lsProcessorError: List[String] = null
	var lsTranslatorError: List[String] = null
	var evowareScript: EvowareScript = null
	
	def printToFile(sFilename: String) {
		FileUtils.printToFile(new File(sFilename))(printToWriter)
	}
	
	def printToWriter(p: PrintWriter) {
		val html = new Html(p)
		
		html.tagPush("html", Map())
		html.tagPush("body", Map())
			
		// Title
		html.docHeader("Documentation for file "+sProtocolFilename, 1)
		
		// Date
		html.tagMake("p", Map(), "Date: "+(new java.util.Date))

		// Input command docs
		html.docHeader("Protocol", 2)
		lNode.foreach(node => {
			html.docHeader("Step "+node.lIndex.last, 3);
			html.docPrintln(node.doc)
		})
		
		if (lsProcessorError != null) {
			html.docHeader("Processor Errors", 2)
			lsProcessorError.foreach(s => html.docPrintln("#. "+s))
			lsProcessorError.foreach(println)
		}

		if (lsTranslatorError != null) {
			html.docHeader("Translation Errors", 2)
			lsTranslatorError.foreach(s => html.docPrintln("#. "+s))
			lsTranslatorError.foreach(println)
		}
		
		if (evowareScript != null) {
			import html._
			
			docHeader("Initial Bench Setup", 2)

			// Print plate locations again
			tagPush("table", Map("border" -> "1"))
			tagPush("tr")
				tagMake("th", Map("colspan" -> "2"), "Plates")
			tagPop()
			tagPush("tr")
				tagMake("th", Map(), "ID")
				tagMake("th", Map(), "Location")
			tagPop()
			processorResult.locationTracker.map.foreach(item => {
				tagPush("tr")
				tagMake("td", Map(), item._1)
				if (!item._2.isEmpty)
					tagMake("td", Map(), item._2.head._2)
				tagPop()
			})
			
			// Print tube locations
			val mapTubeLoc = processorResult.ob.m_mapWell2.filter(pair => pair._2.isInstanceOf[WellPosition])
			if (!mapTubeLoc.isEmpty) {
				tagPush("tr")
					tagMake("th", Map("colspan" -> "2"), "Tubes")
				tagPop()
				tagPush("tr")
					tagMake("th", Map(), "ID")
					tagMake("th", Map(), "Location")
				tagPop()
				mapTubeLoc.foreach(pair => pair._2 match {
					case pos: WellPosition =>
						tagPush("tr")
						tagMake("td", Map(), pair._1)
						tagMake("td", Map(), pos.idPlate+" row "+(pos.iRow + 1)+" col "+(pos.iCol + 1))
						tagPop()
					case _ =>
				})
			}
			tagPop()
			
			// Resource usage
			docHeader("Resource Usage", 2)
			tagPush("table", Map("border" -> "1"))
			tagPush("tr")
				tagMake("th", Map(), "Liquid")
				tagMake("th", Map(), "Vessel")
				tagMake("th", Map(), "Volume")
			tagPop()
			for ((idLiquid, map) <- evowareScript.state.mapLiquidToWellToAspirated) {
				for ((idWell, vol) <- map) {
						tagPush("tr")
						tagMake("td", Map(), idLiquid)
						tagMake("td", Map(), idWell)
						tagMake("td", Map(), vol.toString)
						tagPop()
				}
			}
			tagPop()
		}
		
		val lPlateWell = processorResult.ob.loadedPlateWells.toList.sortBy(_.id)
		if (!lPlateWell.isEmpty) {
			val states0 = processorResult.ob.getBuilder.toImmutable
			val states1_? : Option[RobotState] = {
				if (lNode.isEmpty) None
				else Option(lNode.last.states1)
			}
			html.docHeader("Vessel Contents", 2)
			html.tagPush("table", Map("border" -> "1"))
			html.tagPush("tr")
				html.tagMake("th", Map(), "Vessel")
				html.tagMake("th", Map(), "Initial")
				html.tagMake("th", Map(), "Final")
			html.tagPop()
			def getContentString(content: VesselContent): String = {
				content.docContent.mdLong_?.map(txtmark.Processor.process).getOrElse("")
			}
			for (well <- lPlateWell) {
				html.tagPush("tr")
				html.tagMake("td", Map(), well.id)
				html.tagMake("td", Map(), well.wellState(states0) match {
					case Error(_) => "ERROR"
					case Success(state) => getContentString(state.content)
				})
				html.tagMake("td", Map(), states1_? match {
					case None => "ERROR"
					case Some(states1) => well.wellState(states1) match {
						case Error(_) => ""
						case Success(state) => getContentString(state.content)
					}
				})
				html.tagPop()
			}
			html.tagPop()
		}
		
		html.docHeader("Robot Commands", 2)
		def printDoc(lNode: java.util.List[CmdNodeBean], nIndent: Int) {
			if (lNode.exists(_.doc != null)) {
				html.tagPush("ul")
				val sIndent = " " * (nIndent * 2)
				lNode.foreach(node => {
					val sDoc = {
						if (node.doc != null && node.doc.size > 50)
							node.doc.take(50)+"..."
						else
							node.doc
					}
					if (node.children != null) {
						html.tagPush("li")
						html.docPrintln(sDoc)
						printDoc(node.children, nIndent + 1)
						html.tagPop()
					}
					else {
						html.tagMake("li", Map(), sDoc)
					}
				})
				html.tagPop()
			}
		}
		printDoc(lNode, 0)
		
		html.tagPop()
		html.tagPop()
	}
}
*/