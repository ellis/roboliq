package roboliq.evoware.config

import roboliq.input.SiteObject

case class EvowareSiteObject(
	name: String,
	evowareCarrier: Int,
	evowareGrid: Int,
	evowareSite: Int
) extends SiteObject

case class EvowareDeviceObject(
	name: String,
	evowareCarrier: Int,
	evowareGrid: Int,
	evowareSite: Int
) extends SiteObject
