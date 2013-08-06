package roboliq.evoware.translator

import roboliq.core._
import roboliq.entities._
import roboliq.input.Protocol
import grizzled.slf4j.Logger
import scala.collection.mutable.ArrayBuffer
import roboliq.tokens._
import scala.collection.mutable.HashMap
import roboliq.evoware.parser.CarrierSite
import roboliq.evoware.parser.EvowareLabwareModel

case class EvowareScript2(
	filename: String,
	siteToModel_m: Map[CarrierSite, EvowareLabwareModel],
	cmd_l: List[Object]
)

class EvowareClientScriptBuilder(config: EvowareConfig, basename: String) extends ClientScriptBuilder {
	private val logger = Logger[this.type]

	val script_l = new ArrayBuffer[EvowareScript2]
	//val builder = new EvowareScriptBuilder
	val cmds = new ArrayBuffer[Object]
	//val mapCmdToLabwareInfo = new HashMap[Object, List[(CarrierSite, EvowareLabwareModel)]]
	val siteToModel_m = new HashMap[CarrierSite, EvowareLabwareModel]
	
	def addOperation(
		protocol: Protocol,
		state0: WorldState,
		operation: String,
		agentIdent: String,
		args: List[String]
	): RsResult[WorldState] = {
		println(s"addOperation: $operation, $agentIdent, $args")
		val identToAgentObject_m: Map[String, Object] = protocol.agentToIdentToInternalObject.get(agentIdent).map(_.toMap).getOrElse(Map())
		operation match {
			case "agent-activate" => RsSuccess(state0)
			case "agent-deactivate" => RsSuccess(state0)
			case "log" =>
				val List(textIdent) = args
				val text = protocol.idToObject(textIdent).toString
				cmds += L0C_Comment(text)
				RsSuccess(state0)
			case "prompt" =>
				val List(textIdent) = args
				val text = protocol.idToObject(textIdent).toString
				cmds += L0C_Prompt(text)
				RsSuccess(state0)
			case "transporter-run" =>
				val List(deviceIdent, labwareIdent, modelIdent, originIdent, destinationIdent, vectorIdent) = args
				//val state = state0.toMutable
				val cmd = {
					if (agentIdent == "user") {
						val model = protocol.eb.getEntity(modelIdent).get
						val modelLabel = model.label.getOrElse(model.key)
						val origin = protocol.eb.getEntity(originIdent).get
						val originLabel = origin.label.getOrElse(origin.key)
						val destination = protocol.eb.getEntity(destinationIdent).get
						val destinationLabel = destination.label.getOrElse(destination.key)
						val text = s"Please move labware `${labwareIdent}` model `${modelLabel}` from `${originLabel}` to `${destinationLabel}`"
						L0C_Prompt(text)
					}
					else {
						val roma_i: Int = identToAgentObject_m(deviceIdent).asInstanceOf[Integer]
						val model = identToAgentObject_m(modelIdent).asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel]
						val origin = identToAgentObject_m(originIdent).asInstanceOf[roboliq.evoware.parser.CarrierSite]
						val destination = identToAgentObject_m(destinationIdent).asInstanceOf[roboliq.evoware.parser.CarrierSite]
						val vectorClass = identToAgentObject_m(vectorIdent).toString
						setModelSites(model, List(origin, destination))
						val carrierSrc = origin.carrier
						val iGridSrc = config.table.mapCarrierToGrid(carrierSrc)
						val lVectorSrc = config.table.configFile.mapCarrierToVectors(carrierSrc)
				
						val carrierDest = destination.carrier
						val iGridDest = config.table.mapCarrierToGrid(carrierDest)
						val lVectorDest = config.table.configFile.mapCarrierToVectors(carrierDest)
				
						L0C_Transfer_Rack(
							roma_i,
							vectorClass,
							//c.sPlateModel,
							//iGridSrc, siteSrc.iSite, siteSrc.carrier.sName,
							//iGridDest, siteDest.iSite, siteDest.carrier.sName,
							model,
							iGridSrc, origin,
							iGridDest, destination,
							LidHandling.NoLid, //c.lidHandling,
							iGridLid = 0,
							iSiteLid = 0,
							sCarrierLid = ""
						)
					}
				}
				cmds += cmd
				RqSuccess(state0)
			case _ =>
				RsError(s"unknown operation `$operation`")
		}
	}
	
	def end(): RsResult[Unit] = {
		endScript()
		RsSuccess(())
	}
	
	private def setModelSites(model: EvowareLabwareModel, sites: List[CarrierSite]) {
		// The assignment of labware to sites is compatible with the current
		// table setup iff none of the sites have already been assigned to a different labware model.
		val isCompatible = sites.forall(site => {
			siteToModel_m.get(site) match {
				case None => true
				case Some(model) => true
				case _ => false
			}
		})
		if (!isCompatible) {
			endScript()
		}
		sites.foreach(site => siteToModel_m(site) = model)
	}
	
	private def endScript() {
		if (!cmds.isEmpty) {
			val script = EvowareScript2(
				basename + ".esc",
				siteToModel_m.toMap,
				cmds.toList
			)
			script_l += script
		}
		siteToModel_m.clear
		cmds.clear
	}

	def saveWithHeader(script: EvowareScript2, sFilename: String) {
		val siteToLabel_m = script.siteToModel_m.map(pair => {
			val (site, _) = pair
			val id0 = f"C${site.carrier.id}%03dS${site.iSite+1}"
			val label = config.config.siteIds.getOrElse(id0, id0)
			site -> label
		})
		val sHeader = config.table.toStringWithLabware(siteToLabel_m, script.siteToModel_m)
		val sCmds = script.cmd_l.mkString("\n")
		val fos = new java.io.FileOutputStream(sFilename)
		writeLines(fos, sHeader)
		writeLines(fos, sCmds);
		fos.close();
	}
	
	private def writeLines(output: java.io.FileOutputStream, s: String) {
		val as = s.split("\r?\n")
		for (sLine <- as if !s.isEmpty) {
			val bytes = sLine.map(_.asInstanceOf[Byte]).toArray
			output.write(bytes)
			output.write("\r\n".getBytes())
		}
	}
}