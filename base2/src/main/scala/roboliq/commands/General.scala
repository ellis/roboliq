package roboliq.commands

import roboliq.entities._

sealed trait Command

case class AgentActivate() extends Command
case class AgentDeactivate() extends Command
case class EvowareSubroutine(path: String) extends Command
case class Log(text: String) extends Command
case class PipetterAspirate(
	val item_l: List[TipWellVolumePolicy]
) extends Command

/*case class PipetterWash(
	tip_l: List[Tip],
	intensity: CleanIntensity.Value
)*/

case class PipetterDispense(
	val item_l: List[TipWellVolumePolicy]
) extends Command

case class PipetterTipsRefresh(
	device: Pipetter,
	// tip, clean intensity, tipModel_?
	item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])]
) extends Command

case class Prompt(text: String) extends Command

case class SealerRun(
	deviceIdent: String,
	specIdent: String,
	labwareIdent: String,
	siteIdent: String
) extends Command

case class ThermocyclerClose(
	deviceIdent: String
) extends Command

case class ThermocyclerOpen(
	deviceIdent: String
) extends Command

case class ThermocyclerRun(
	deviceIdent: String,
	specIdent: String
) extends Command

case class TransporterRun(
	deviceIdent: String,
	labwareIdent: String,
	modelIdent: String,
	originIdent: String,
	destinationIdent: String,
	vectorIdent: String
) extends Command
