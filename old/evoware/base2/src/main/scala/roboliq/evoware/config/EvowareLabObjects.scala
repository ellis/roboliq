package roboliq.evoware.config

import roboliq.input.SiteObject
import roboliq.input.LabObject

case class EvowareSiteObject(
	name: String,
	evowareCarrier: Int,
	evowareGrid: Int,
	evowareSite: Int
) extends SiteObject

case class EvowareDeviceObject(
	`type`: String,
	evowareName: String
) extends LabObject
