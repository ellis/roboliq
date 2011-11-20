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
			val cmds4 = output.collect { case RoboeaseCommand(_, _, cmd: CommandL4) => cmd }
			cmds4.foreach(_.addKnowledge(kb))
			Right(RoboeaseResult(shared.kb, output))
		}
		else {
			val log = Log(shared.errors.map(_.sError))
			Left(CompileStageError(log))
		}
	}
	
	private def parseProc(sName: String, lsArg: Array[String], sSource: String) {
		var iLine = 1
		for (sLine <- sSource.lines) {
			val s = sLine.replaceAll("#.*", "").trim
			m_sLine = s
			if (!s.isEmpty())
				println("LOG: sLine: "+s)
			if (s.startsWith("PROC ")) {
				val lsParam = s.drop(5).split("""\s+""")
				//if (lsVar.size != lsArg.size) {
				if (lsParam.size > lsArg.size) {
					shared.addError("Call to "+sName+" expected "+lsParam.size+" arguments but was passed "+lsArg.size+" arguments")
					return
				}
				/*
				// Function to classify whether the argument is a simple variable, a list variable, or unknown
				def categorizeArg(ss: Tuple2[String, String]): Int = {
					if (shared.mapVars contains ss._2) 1
					else if (shared.mapLists contains ss._2) 2
					else 0
				}
				val lParamArgCat = (lsParam zip lsArg).toList.map(ss => (ss._1, ss._2, categorizeArg(ss)))
				println("lParamArgCat: "+lParamArgCat.toList)
				// If there are any unknown arguments, indicate the error
				val l0 = lParamArgCat.filter(_._3 == 0)
				if (!l0.isEmpty) {
					shared.addError("Unknown argument(s): "+l0.map(_._2).mkString(", "))
					return
				}
				shared.mapVars ++= lParamArgCat.filter(_._3 == 1).map(tuple => tuple._1 -> shared.mapVars(tuple._2))
				shared.mapLists ++= lParamArgCat.filter(_._3 == 1).map(tuple => tuple._1 -> shared.mapLists(tuple._2))
				*/
				shared.mapSubstitutions = shared.mapSubstitutions ++ (lsParam zip lsArg)
			}
			else {
				handleScript(s)
			}
			iLine += 1
		}
		Right(())
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
						val l = sLine.split("""\s+""")
						val sName = l.head
						val lsArg = l.tail
						if (!callProcedure(sName, lsArg))
							addError("unrecognized command: " + sCmd)
					case Some(p) =>
						val r = pScript.parseAll(p, rCmd.next)
						if (!r.successful)
							addError(r.toString)
						else {
							val res: roboliq.common.Result[CmdLog] = r.get
							res match {
								case Error(lsError) =>
									lsError.foreach(addError)
								case Success(cmdlog) =>
									cmdlog.cmds.foreach(addRunCommand)
									bFound = true
							}
						}
				}
			}
			else {
				addError("could not pares line")
			}
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

	def addRunCommand(cmd: Command) {
		output += RoboeaseCommand(shared.iLineCurrent, shared.sLineCurrent, cmd)
		println("LOG: addRunCommand: "+cmd.getClass().getCanonicalName())
	}
}
