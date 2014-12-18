package roboliq.evoware.config

import spray.json._
import spray.json.DefaultJsonProtocol._
import grizzled.slf4j.Logger
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import roboliq.core._
import roboliq.entities._
import roboliq.input.commands._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import roboliq.utils.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import roboliq.evoware.translator.EvowareConfigData
import roboliq.evoware.translator.EvowareConfig
import roboliq.evoware.translator.EvowareClientScriptBuilder
import scalax.collection.Graph
import scalax.collection.edge.LHyperEdge
import scalax.collection.edge.LkUnDiEdge
import roboliq.plan.CallTree
import roboliq.plan.Call
import roboliq.ai.strips
import roboliq.ai.plan.Unique
import roboliq.plan.CommandSet
import roboliq.plan.OperatorInfo
import roboliq.ai.plan.PartialPlan
import roboliq.commands.OperatorHandler_TransportLabware
import roboliq.plan.ActionHandler
import roboliq.commands._
import roboliq.plan.OperatorHandler
import roboliq.commands.ShakePlateOperatorHandler
import roboliq.commands.ShakePlateActionHandler
import roboliq.evoware.handler.EvowareInfiniteM200InstructionHandler
import roboliq.evoware.translator.EvowareSealerProgram
import roboliq.evoware.commands.OperatorHandler_EvowareTransportLabware
import roboliq.evoware.commands.EvowareCentrifugeRunActionHandler
import roboliq.evoware.commands.EvowareCentrifugeRunOperatorHandler
import roboliq.evoware.commands.EvowareTimerWaitOperatorHandler
import roboliq.evoware.commands.EvowareTimerStartActionHandler
import roboliq.evoware.commands.EvowareTimerWaitActionHandler
import roboliq.evoware.commands.EvowareTimerSleepActionHandler
import roboliq.evoware.commands.EvowareTimerStartOperatorHandler
import roboliq.commands.PromptOperatorOperatorHandler
import roboliq.commands.PromptOperatorActionHandler
import roboliq.evoware.commands.EvowareBeginLoopOperatorHandler
import roboliq.evoware.commands.EvowareEndLoopOperatorHandler
import roboliq.evoware.commands.EvowareBeginLoopActionHandler
import roboliq.evoware.commands.EvowareEndLoopActionHandler
import com.google.gson.Gson
import roboliq.input.EvowareAgentBean

case class WellGroupBean(
	name: String,
	description_? : Option[String],
	well: PipetteDestinations
)

case class SubstanceBean(
	name: String,
	description_? : Option[String],
	type_? : Option[SubstanceKind.Value],
	tipCleanPolicy_? : Option[TipCleanPolicy],
	contaminants: Set[String]
)

private case class SourceSubstanceBean(
	name: String,
	amount_? : Option[AmountSpec],
	description_? : Option[String],
	type_? : Option[SubstanceKind.Value],
	tipCleanPolicy_? : Option[TipCleanPolicy],
	contaminants: Set[String]
)

private case class SourceBean(
	name: List[String],
	well: PipetteDestinations,
	substance: List[SourceSubstanceBean],
	amount_? : Option[LiquidVolume]
)

private case class ReagentBean(
	id: String,
	wells: PipetteDestinations,
	contaminants : Set[String],
	viscosity_? : Option[String],
	sterilize_? : Option[String],
	pipettePolicy_? : Option[String],
	key_? : Option[String]
)

class EvowareProtocolDataGenerator {
	
	private val logger = Logger[this.type]

	val eb = new EntityBase
	val state0 = new WorldStateBuilder
	private var cs: CommandSet = null
	private var tree: CallTree = null
	//private var tasks = new ArrayBuffer[Rel]
	private var var_i = 0

	// HACK: defined here so that loadConfig() and loadEvoware() both have access
	private val offsiteModel = SiteModel(gid)
	private val userArm = Transporter(gid)
	private val userArmSpec = TransporterSpec("userArmSpec")

	private def gid: String = java.util.UUID.randomUUID().toString()
	private def nvar: Int = { var_i += 1; var_i }
	
	/** Specs which consists of a single string.  This is a list of tuples (specIdent, string) */
	val specToString_l = new ArrayBuffer[(String, String)]
	/** Valid device+spec combinations.  List of tuples (deviceIdent, specIdent) */
	val deviceToSpec_l = new ArrayBuffer[(String, String)]
	/** Valid device+model+spec combinations.  List of tuples (deviceIdent, evoware plate model ID, specIdent) */
	val deviceToModelToSpec_l = new ArrayBuffer[(String, String, String)]
	
	val nameToSubstance_m = new HashMap[String, Substance]
	/**
	 * Map of task variable identifier to an internal object -- for example, the name of a text variable to the actual text.
	 */
	val idToObject = new HashMap[String, Object]
	/**
	 * Individual agents may need to map identifiers to internal objects
	 */
	val agentToIdentToInternalObject = new HashMap[String, HashMap[String, Object]]
	val agentToBuilder_m = new HashMap[String, ClientScriptBuilder]

	// TODO: This should probably be moved out of the Protocol class
	def loadEvowareAgentBean(
		agentIdent: String,
		agentBean: EvowareAgentBean,
		table_l: List[String],
		searchPath_l: List[File]
	): RsResult[Unit] = {
		import roboliq.entities._
		
		val user = Agent(gid, Some("user"))
		val offsite = Site(gid, Some("offsite"))
		
		// TODO: put these into a for-comprehension in order to return warnings and errors
		eb.addAgent(user, "user")
		eb.addModel(offsiteModel, "offsiteModel")
		eb.addSite(offsite, "offsite")
		eb.addDevice(user, userArm, "userArm")
		eb.addDeviceSpec(userArm, userArmSpec, "userArmSpec")
		// userArm can transport from offsite
		eb.addRel(Rel("transporter-can", List("userArm", "offsite", "userArmSpec")))
		
		val tableNameDefault = s"${agentIdent}_default"
		val tableSetup_m = agentBean.tableSetups.toMap.map(pair => s"${agentIdent}_${pair._1}" -> pair._2)
		for {
			// Load carrier file
			evowarePath <- RsResult(agentBean.evowareDir, "evowareDir must be set")
			carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile(new File(evowarePath, "Carrier.cfg").getPath)
			// FIXME: for debug only
			//_ = carrierData.printCarriersById
			// ENDIF
			// Choose a table
			tableName <- table_l.filter(tableSetup_m.contains) match {
				case Nil =>
					if (tableSetup_m.contains(tableNameDefault))
						RsSuccess(tableNameDefault)
					else
						RsError(s"No table specified for agent `$agentIdent`")
				case s :: Nil => RsSuccess(s)
				case l => RsError(s"Agent `$agentIdent` can only be assigned one table, but multiple tables were specified: $l")
			}
			tableSetupBean = tableSetup_m(tableName)
			// Load table file
			tableFile <- RsResult(tableSetupBean.tableFile, s"tableFile property must be set on tableSetup `$tableName`")
			tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile)
			configEvoware = new ConfigEvoware(eb, agentIdent, carrierData, tableData,
					agentBean, tableSetupBean, offsiteModel, userArm,
					specToString_l.toList, deviceToSpec_l.toList, deviceToModelToSpec_l.toList)
			scriptBuilder <- configEvoware.loadEvoware()
		} yield {
			agentToIdentToInternalObject(agentIdent) = configEvoware.identToAgentObject
			agentToBuilder_m += agentIdent -> scriptBuilder
			if (!agentToBuilder_m.contains("user"))
				agentToBuilder_m += "user" -> scriptBuilder
		}
	}
	
	def createDomain(cs: CommandSet, operatorInfo_l: List[OperatorInfo]): RqResult[strips.Domain] = {
		RsResult.prependError("createDomain:") {
			val name_l = (cs.nameToAutoOperator_l ++ operatorInfo_l.map(_.operatorName)).distinct.sorted
			for {
				operatorHandler_l <- RsResult.mapAll(name_l)(cs.getOperatorHandler)
			} yield {
				val operator_l = operatorHandler_l.map(_.getDomainOperator)

				// FIXME: HACK: need to programmatically figure out the parent classes of type -- this is here as a hack 
				val type0_m = Map(
					"centrifuge" -> "device",
					"peeler" -> "device",
					"pipetter" -> "device",
					"reader" -> "device",
					"sealer" -> "device",
					"shaker" -> "device",
					"thermocycler" -> "device",
					"transporter" -> "device"
				)
				// Get types used, and have them all inherit from 'any'
				val type1_m = operator_l.flatMap(_.paramTyp_l.map(_ -> "any")).toMap
				// The type0 types take precedence
				val type_m = type1_m ++ type0_m
				
				strips.Domain(
					type_m = type_m,
					constantToType_l = Nil,
					predicate_l = List[strips.Signature](
						strips.Signature("agent-has-device", "?agent" -> "agent", "?device" -> "device"),
						strips.Signature("device-can-site", "?device" -> "device", "?site" -> "site"),
						strips.Signature("location", "?labware" -> "labware", "?site" -> "site"),
						strips.Signature("model", "?labware" -> "labware", "?model" -> "model"),
						strips.Signature("stackable", "?sm" -> "siteModel", "?m" -> "model")
					),
					operator_l = operator_l
				)
			}
		}
	}


	def createProblem(planInfo_l: List[OperatorInfo], domain: strips.Domain): RqResult[strips.Problem] = {
		val typToObject_l: List[(String, String)] = eb.createProblemObjects.map(_.swap) ++ planInfo_l.flatMap(_.problemObjectToTyp_l).map(_.swap)
		
		val state0 = strips.State(Set[strips.Atom]() ++ planInfo_l.flatMap(_.problemState_l) ++ eb.createProblemState.map(rel => strips.Atom(rel.name, rel.args)))
		
		RqSuccess(strips.Problem(
			domain = domain,
			typToObject_l = typToObject_l,
			state0 = state0,
			goals = strips.Literals.empty
		))
	}

}