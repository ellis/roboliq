package roboliq.commands

import scala.reflect.runtime.universe
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import grizzled.slf4j.Logger
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.CleanIntensity
import roboliq.entities.EntityBase
import roboliq.entities.LiquidSource
import roboliq.entities.LiquidVolume
import roboliq.entities.PipetteAmount
import roboliq.entities.PipetteAmount_Dilution
import roboliq.entities.PipetteAmount_Volume
import roboliq.entities.PipetteDestinations
import roboliq.entities.Pipetter
import roboliq.entities.TipModel
import roboliq.entities.WorldState
import roboliq.input.AgentInstruction
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import roboliq.input.Context


sealed trait TitrateAmount
case class TitrateAmount_Volume(volume: LiquidVolume) extends TitrateAmount
case class TitrateAmount_Range(min: LiquidVolume, max: LiquidVolume) extends TitrateAmount

case class TitrateActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	allOf: List[TitrateStepParams],
	destination: PipetteDestinations,
	amount_? : Option[LiquidVolume],
	replicates_? : Option[Int],
	clean_? : Option[CleanIntensity.Value],
	cleanBegin_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanBetweenSameSource_? : Option[CleanIntensity.Value],
	cleanEnd_? : Option[CleanIntensity.Value],
	pipettePolicy_? : Option[String],
	tipModel_? : Option[TipModel],
	tip: List[Int]
) {
	def getItems: RqResult[List[TitrateItem]] = {
		RqResult.toResultOfList(allOf.map(_.getItem)).map(_.flatten)
	}
}

case class TitrateStepParams(
	allOf: List[TitrateStepParams],
	oneOf: List[TitrateStepParams],
	source: List[LiquidSource],
	amount_? : Option[List[PipetteAmount]],
	//contact_? : Option[PipettePosition.Value],
	pipettePolicy_? : Option[String],
	clean_? : Option[CleanIntensity.Value],
	cleanBegin_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanBetweenSameSource_? : Option[CleanIntensity.Value],
	cleanEnd_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel],
	tip: List[Int]
) {
	def getSourceLabwares: List[String] = {
		source.flatMap(_.l.map(_.labwareName)) ++ allOf.flatMap(_.getSourceLabwares) ++ oneOf.flatMap(_.getSourceLabwares)
	}
	
	def getItem: RqResult[List[TitrateItem]] = {
		(allOf, oneOf, source) match {
			case (l, Nil, Nil) if !l.isEmpty =>
				RqResult.toResultOfList(allOf.map(_.getItem)).map(l => List(TitrateItem_And(l.flatten)))
			case (Nil, l, Nil) if !l.isEmpty =>
				RqResult.toResultOfList(oneOf.map(_.getItem)).map(l => List(TitrateItem_Or(l.flatten)))
			case (Nil, Nil, source) if !source.isEmpty =>
				val l: List[TitrateItem_SourceVolume] = source.flatMap(src => {
					amount_? match {
						case None => List(TitrateItem_SourceVolume(this, src, None))
						case Some(amount_l) => amount_l.map(amount => TitrateItem_SourceVolume(this, src, Some(amount)))
					}
				})
				val l2 = if (l.size == 1) l else List(TitrateItem_Or(l))
				RqSuccess(l2)
			case _ =>
				RqError("Each titration step must specify exactly one of: `allOf`, `oneOf`, or `source`")
		}
	}
}

sealed trait TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String)
}
case class TitrateItem_And(l: List[TitrateItem]) extends TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String) {
		println(indent + "allOf:")
		l.foreach(_.printShortHierarchy(eb, indent+"  "))
	}
}
case class TitrateItem_Or(l: List[TitrateItem]) extends TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String) {
		println(indent + "oneOf:")
		l.foreach(_.printShortHierarchy(eb, indent+"  "))
	}
}
case class TitrateItem_SourceVolume(
	step: TitrateStepParams,
	source: LiquidSource,
	amount_? : Option[PipetteAmount]
) extends TitrateItem {
	def printShortHierarchy(eb: EntityBase, indent: String) {
		val amount = amount_? match {
			case None => "*"
			case Some(PipetteAmount_Volume(x)) => x.toString
			case Some(PipetteAmount_Dilution(num, den)) => s"$num:$den"
		}
		println(indent + source.l.head + " " + amount)
	}
}

class TitrateActionHandler extends ActionHandler {

	def getActionName = "titrate"

	def getActionParamNames = List("agent", "device", "allOf", "destination", "amount", "replicates", "clean", "cleanBegin", "cleanBetween", "cleanBetweenSameSource", "cleanEnd", "pipettePolicy", "tipModel", "tip")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[TitrateActionParams](paramToJsval_l, eb, state0)
		} yield {
			val sourceLabware_l = params.allOf.flatMap(_.getSourceLabwares)
			val destinationLabware_l = params.destination.l.map(_.labwareName)
			val labwareIdent_l = (sourceLabware_l ++ destinationLabware_l).distinct
			val n = labwareIdent_l.size

			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = {
				"?agent" -> m.getOrElse("agent", "?agent") ::
				"?device" -> m.getOrElse("device", "?device") ::
				labwareIdent_l.zipWithIndex.map(pair => s"?labware${pair._2 + 1}" -> s"${pair._1}")
			}
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, s"titrate$n", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class TitrateOperatorHandler(n: Int) extends OperatorHandler {
	private val logger = Logger[this.type]
	
	def getDomainOperator: Strips.Operator = {
		val name = s"titrate$n"
		val paramName_l = "?agent" :: "?device" :: (1 to n).flatMap(i => List(s"?labware$i", s"?model$i", s"?site$i", s"?siteModel$i")).toList
		val paramTyp_l = "agent" :: "pipetter" :: List.fill(n)(List("labware", "model", "site", "siteModel")).flatten
		val preconds =
			Strips.Literal(true, "agent-has-device", "?agent", "?device") ::
			Strips.Literal(Strips.Atom("ne", (1 to n).map(i => s"?site$i")), true) ::
			(1 to n).flatMap(i => List(
				Strips.Literal(true, "device-can-site", "?device", s"?site$i"),
				Strips.Literal(true, "model", s"?labware$i", s"?model$i"),
				Strips.Literal(true, "location", s"?labware$i", s"?site$i"),
				Strips.Literal(true, "model", s"?site$i", s"?siteModel$i"),
				Strips.Literal(true, "stackable", s"?siteModel$i", s"?model$i")
			)).toList

		Strips.Operator(
			name = name,
			paramName_l = paramName_l,
			paramTyp_l = paramTyp_l,
			preconds = Strips.Literals(Unique(preconds : _*)),
			effects = aiplan.strips2.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		for {
			agent <- Context.getEntityAs[Agent](operator.paramName_l(0))
			pipetter <- Context.getEntityAs[Pipetter](operator.paramName_l(1))
			params <- Converter.convInstructionParamsAs[TitrateActionParams](instructionParam_m)
			pipetteActionParams_l <- new TitrateMethod(params).createPipetteActionParams()
			_ <- Context.foreachFirst(pipetteActionParams_l)(pipetteActionParams => new PipetteMethod().run(agent, pipetter, pipetteActionParams))
		} yield ()
	}
}
