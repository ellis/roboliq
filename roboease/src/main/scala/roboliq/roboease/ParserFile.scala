package roboliq.roboease

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
//import scala.util.parsing.combinator._

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


class ParserFile(mapTables: Map[String, Table], mapTipModel: Map[String, TipModel], mapLcToPolicy: Map[String, PipettePolicy]) {
	private val shared = new ParserSharedData(mapTipModel, mapLcToPolicy)
	import shared._
	private val pConfig = new ParserLineConfig(shared, mapTables)
	private val pScript = new ParserLineScript(shared)
	
	private object Section extends Enumeration {
		val Config, ConfigList, Doc, Script = Value
	}

	private var m_section = Section.Config
	private var m_asDoc: List[String] = Nil
	private var m_asList = new ArrayBuffer[String]
	private var m_sListName: String = null
	private var m_map31: ObjMapper = null
	private var m_sLine: String = null
	private val m_lsErrors = new ArrayBuffer[Tuple2[String, String]]
	
	def sTable = shared.sTable
	def sHeader = shared.sHeader
	def mapLabware = shared.mapLabware.toMap
	
	
	def parse(sSource: String): Either[CompileStageError, RoboeaseResult] = {
		var iLine = 1
		for (sLine <- sSource.lines) {
			val s = sLine.replaceAll("#.*", "").trim
			m_sLine = s
			if (!s.isEmpty())
				println("LOG: sLine: "+s)
			s match {
				case "" =>
				case "DOC" =>
					m_section = Section.Doc
				case "SCRIPT" =>
					kb.concretize() match {
						case Left(errK) =>
							m_map31 = null
							return Left(errK)
						case Right(succK) =>
							m_map31 = succK.mapper
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
			iLine += 1
		}
		if (shared.errors.isEmpty) {
			val cmds4 = pScript.cmds.collect { case RoboeaseCommand(_, _, cmd: CommandL4) => cmd }
			cmds4.foreach(_.addKnowledge(kb))
			Right(RoboeaseResult(shared.kb, pScript.cmds))
		}
		else {
			val log = Log(shared.errors.map(_.sError))
			Left(CompileStageError(log))
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
		val rAssign = pConfig.parseAll(pConfig.cmd0Assign, sLine)
		val rList = pConfig.parseAll(pConfig.cmd0List, sLine)
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
			val rCmd = pConfig.parse(pConfig.word, sLine)
			if (rCmd.successful) {
				val sCmd: String = rCmd.get
				pConfig.cmds0.get(sCmd) match {
					case None =>
						addError("unrecognized command: " + sCmd)
					case Some(p) =>
						val r = pConfig.parseAll(p, rCmd.next)
						if (!r.successful)
							addError(r.toString)
						else
							bFound = true
				}
			}
			else {
				addError("could not pares line")
			}
		}
	}
	
	private def handleConfigList(s: String) {
		if (s == "ENDLIST") {
			mapLists(m_sListName) = m_asList.toList
			m_section = Section.Config
		}
		else
			m_asList += s
	}
	
	private def handleScript(sLine: String) {
		if (sLine == "ENDSCRIPT")
			m_section = Section.Config
		else if (m_map31 != null) {
			//m_sScriptLine = sLine
			var bFound = false
			val rCmd = pScript.parse(pScript.word, sLine)
			if (rCmd.successful) {
				val sCmd: String = rCmd.get
				//println("sCmd = "+sCmd)
				pScript.cmds2.get(sCmd) match {
					case None =>
						addError("unrecognized command: " + sCmd)
					case Some(p) =>
						val r = pScript.parseAll(p, rCmd.next)
						if (!r.successful)
							addError(r.toString)
						else
							bFound = true
				}
			}
			else {
				addError("could not pares line")
			}
		}
	}
	
	private def toLabel(well: Well): String = {
		kb.getWellSetup(well).sLabel_?.get
	}
	
	def DefineRack(name: String, grid: Int, site: Int, xsize: Int, ysize: Int, nVolumeMax: Double, carrierType: String = "") {
		val rack = Rack(
				name, xsize, ysize, grid, site, nVolumeMax, carrierType
				)
		mapRacks(name) = rack
	}
	
	def racks = shared.mapRacks.values
}
