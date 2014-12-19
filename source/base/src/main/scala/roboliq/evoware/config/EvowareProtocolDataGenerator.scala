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
import roboliq.input.ProtocolDataA

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
	def createProtocolData(
		agentIdent: String,
		agentBean: EvowareAgentBean,
		table_l: List[String],
		searchPath_l: List[File]
	): ResultC[ProtocolDataA] = {
		import roboliq.entities._
		
		val user = Agent(gid, Some("user"))
		val offsite = Site(gid, Some("offsite"))
		
		// TODO: put these into a for-comprehension in order to return warnings and errors
		eb.addAgent(user, "user")
		
		val tableNameDefault = s"${agentIdent}_default"
		val tableSetup_m = agentBean.tableSetups.toMap.map(pair => s"${agentIdent}_${pair._1}" -> pair._2)
		for {
			// Load carrier file
			evowarePath <- ResultC.from(Option(agentBean.evowareDir), "evowareDir must be set")
			carrierData <- ResultC.from(roboliq.evoware.parser.EvowareCarrierData.loadFile(new File(evowarePath, "Carrier.cfg").getPath))
			// FIXME: for debug only
			//_ = carrierData.printCarriersById
			// ENDIF
			// Choose a table
			tableName <- table_l.filter(tableSetup_m.contains) match {
				case Nil =>
					if (tableSetup_m.contains(tableNameDefault))
						ResultC.unit(tableNameDefault)
					else
						ResultC.error(s"No table specified for agent `$agentIdent`")
				case s :: Nil => ResultC.unit(s)
				case l => ResultC.error(s"Agent `$agentIdent` can only be assigned one table, but multiple tables were specified: $l")
			}
			tableSetupBean = tableSetup_m(tableName)
			// Load table file
			tableFile <- ResultC.from(Option(tableSetupBean.tableFile), s"tableFile property must be set on tableSetup `$tableName`")
			tableData <- ResultC.from(roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile))
			configEvoware = new ConfigEvoware(eb, agentIdent, carrierData, tableData,
					agentBean, tableSetupBean, offsiteModel, userArm,
					specToString_l.toList, deviceToSpec_l.toList, deviceToModelToSpec_l.toList)
			scriptBuilder <- ResultC.from(configEvoware.loadEvoware())
		} yield {
			agentToIdentToInternalObject(agentIdent) = configEvoware.identToAgentObject
			agentToBuilder_m += agentIdent -> scriptBuilder
			if (!agentToBuilder_m.contains("user"))
				agentToBuilder_m += "user" -> scriptBuilder
			new ProtocolDataA(
				objects = RjsMap(),
				val commands: RjsMap,
				val commandOrderingConstraints: List[List[String]],
				val commandOrder: List[String],
				val planningDomainObjects: Map[String, String],
				val planningInitialState: strips.Literals
			)
		}
	}
	
	def createProblem(
		planInfo_l: List[OperatorInfo],
		domain: strips.Domain
	): RqResult[strips.Problem] = {
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