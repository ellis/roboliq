package roboliq.commands

import roboliq.entities.TipWellVolumePolicy

sealed trait Command

case class AgentActivate() extends Command
case class AgentDeactivate() extends Command
case class Log(text: String) extends Command
case class PipetterAspirate(
	val item_l: List[TipWellVolumePolicy]
) extends Command

case class PipetterDispense(
	val item_l: List[TipWellVolumePolicy]
) extends Command

case class Prompt(text: String) extends Command

case class SealerRun(
	deviceIdent: String,
	specIdent: String,
	labwareIdent: String,
	siteIdent: String
) extends Command

case class TransporterRun(
	deviceIdent: String,
	labwareIdent: String,
	modelIdent: String,
	originIdent: String,
	destinationIdent: String,
	vectorIdent: String
) extends Command
