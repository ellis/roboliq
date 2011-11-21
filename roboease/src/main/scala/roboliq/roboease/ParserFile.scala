package roboliq.roboease

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
//import scala.util.parsing.combinator._

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


/**
 * @param sPathProc path to the folder where procedure files are stored
 */
class ParserFile(
	dirProc: java.io.File,
	dirLog: java.io.File,
	mapTables: Map[String, Table],
	mapTipModel: Map[String, TipModel],
	mapLcToPolicy: Map[String, PipettePolicy],
	mapPlateModel: Map[String, PlateModel]
) {
	private val shared = new ParserSharedData(dirProc, dirLog, mapTipModel, mapLcToPolicy, mapPlateModel)
	//import shared._
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
	private var m_iLine: Int = 0
	private val m_errors = new ArrayBuffer[LineError]
	private val output = new ArrayBuffer[RoboeaseCommand]
	
	def sTable = shared.sTable
	def sHeader = shared.sHeader
	def mapLabware = shared.mapLabware.toMap
	
	def parseFile(sFilename: String): Either[CompileStageError, RoboeaseResult] = {
		shared.file = new java.io.File(sFilename)
		val sSource = scala.io.Source.fromFile(shared.file).mkString
		parse(sSource)
	}
	
	private def parse(sSource: String): Either[CompileStageError, RoboeaseResult] = {
		m_iLine = 1
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
					shared.kb.concretize() match {
						case Left(errK) =>
							m_map31 = null
							return Left(errK)
						case Right(succK) =>
							m_map31 = succK.mapper
					}
					m_section = Section.Script
				case _ =>
					val res = m_section match {
						case Section.Doc => parseLineDoc(s)
						case Section.Config => parseLineConfig(s)
						case Section.ConfigList => parseLineConfigList(s)
						case Section.Script => parseLineScript(s)
					}
					res match {
						case Error(lsError) => lsError.foreach(addError)
						case _ =>
					}
			}
			m_iLine += 1
		}
		if (m_errors.isEmpty) {
			val cmds4 = output.collect { case RoboeaseCommand(_, _, cmd: CommandL4) => cmd }
			cmds4.foreach(_.addKnowledge(shared.kb))
			Right(RoboeaseResult(shared.kb, output))
		}
		else {
			val lLogItem = m_errors.map(error => {
				new LogItem(error.file.getName()+": line "+error.iLine+error.iCol_?.map(":"+_.toString).getOrElse(""), error.sError)
			})
			val log = new Log(lLogItem, Seq(), Seq())
			Left(CompileStageError(log))
		}
	}
	
	private def parseProc(sName: String, lsArg: Array[String], sSource: String) {
		m_iLine = 1
		for (sLine <- sSource.lines) {
			val s = sLine.replaceAll("#.*", "").trim
			m_sLine = s
			if (!s.isEmpty())
				println("LOG: sLine: "+s)
			if (s.startsWith("PROC ")) {
				val lsParam = s.drop(5).split("""\s+""")
				//if (lsVar.size != lsArg.size) {
				if (lsParam.size > lsArg.size) {
					addError("Call to "+sName+" expected "+lsParam.size+" arguments but was passed "+lsArg.size+" arguments")
					return
				}
				shared.mapSubstitutions = shared.mapSubstitutions ++ (lsParam zip lsArg)
			}
			else if (!s.isEmpty) {
				val res = parseLineScript(s)
				res match {
					case Error(lsError) => lsError.foreach(addError)
					case _ =>
				}
			}
			m_iLine += 1
		}
		Right(())
	}
	
	private def parseLineDoc(s: String): Result[Unit] = {
		if (s == "ENDDOC") {
			m_asDoc = m_asDoc.reverse
			m_section = Section.Config
		}
		else
			m_asDoc = s :: m_asDoc
		Success(())
	}
	
	private def parseLineConfig(sLine: String): Result[Unit] = {
		val rAssign = pConfig.parseAll(pConfig.cmd0Assign, sLine)
		val rList = pConfig.parseAll(pConfig.cmd0List, sLine)
		if (rAssign.successful) {
			// Do nothing, because already handled by parser
			Success()
		}
		else if (rList.successful) {
			m_asList.clear
			m_sListName = rList.get
			m_section = Section.ConfigList
			Success()
		}
		else {
			val rCmd = pConfig.parse(pConfig.word, sLine)
			if (rCmd.successful) {
				val sCmd: String = rCmd.get
				pConfig.cmds0.get(sCmd) match {
					case None =>
						Error("unrecognized command: " + sCmd)
					case Some(p) =>
						val r = pConfig.parseAll(p, rCmd.next)
						if (!r.successful) Error(r.toString)
						else {
							val res: roboliq.common.Result[Unit] = r.get
							res match {
								case Error(lsError) => Error(lsError.mkString("; "))
								case _ => Success()
							}
						}
				}
			}
			else {
				Error("could not parse config line")
			}
		}
	}
	
	private def parseLineConfigList(s: String): Result[Unit] = {
		if (s == "ENDLIST") {
			shared.mapLists(m_sListName) = m_asList.toList
			m_section = Section.Config
		}
		else
			m_asList += s
		Success()
	}
	
	private def parseLineScript(sLine: String): Result[Unit] = {
		if (sLine == "ENDSCRIPT") {
			m_section = Section.Config
			Success()
		}
		else if (m_map31 != null) {
			//m_sScriptLine = sLine
			val rCmd = pScript.parse(pScript.word, sLine)
			if (rCmd.successful) {
				val sCmd: String = rCmd.get
				pScript.cmds2.get(sCmd) match {
					case None =>
						val l = sLine.split("""\s+""")
						val sName = l.head
						val lsArg = l.tail
						if (callProcedure(sName, lsArg)) Success()
						else Error("unrecognized command: " + sCmd)
					case Some(p) =>
						val r = pScript.parseAll(p, rCmd.next)
						if (!r.successful)
							Error(r.toString)
						else {
							val res: roboliq.common.Result[CmdLog] = r.get
							res match {
								case Error(lsError) => Error(lsError)
								case Success(cmdlog) =>
									cmdlog.cmds.foreach(addRunCommand)
									Success()
							}
						}
				}
			}
			else {
				Error("could not parse script line")
			}
		}
		else {
			//Error("config could not be parsed")
			Success()
		}
	}
	
	private def callProcedure(sName: String, lsArg: Array[String]): Boolean = {
		val sFilename = sName + ".proc"
		val lFile = List(new File(sFilename), new File(shared.file.getParentFile, sFilename), new File(dirProc, sFilename))
		lFile.find(_.exists) match {
			case None => false
			case Some(file) =>
				// New file reference
				val sSource = scala.io.Source.fromFile(file).mkString
				shared.stackFile.push(shared.file)
				shared.file = file
				val mapSubstitutions = shared.mapSubstitutions
				// Store variables of calling script
				//val mapVars = shared.mapVars.toMap
				//val mapLists = shared.mapLists.toMap
				// Parse the procedure file
				parseProc(sName, lsArg, sSource)
				// Restore previous file and variable map
				shared.file = shared.stackFile.pop
				shared.mapSubstitutions = mapSubstitutions
				//shared.mapVars.clear()
				//shared.mapVars ++= mapVars
				//shared.mapLists.clear()
				//shared.mapLists ++= mapLists
				true
		}
	}

	def addError(sError: String) {
		m_errors += LineError(shared.file, m_iLine, None, m_sLine, sError)
	}
	
	private def toLabel(well: Well): String = {
		shared.kb.getWellSetup(well).sLabel_?.get
	}
	
	def DefineRack(name: String, grid: Int, site: Int, xsize: Int, ysize: Int, nVolumeMax: Double, carrierType: String = "") {
		val rack = Rack(
				name, xsize, ysize, grid, site, nVolumeMax, carrierType
				)
		shared.mapRacks(name) = rack
	}
	
	def racks = shared.mapRacks.values

	def addRunCommand(cmd: Command) {
		output += RoboeaseCommand(m_iLine, m_sLine, cmd)
		println("LOG: addRunCommand: "+cmd.getClass().getCanonicalName())
	}
}
