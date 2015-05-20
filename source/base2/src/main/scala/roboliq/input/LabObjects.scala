package roboliq.input

trait LabObject {
	val `type`: String
}

case class PlateObject() extends LabObject {
	val `type` = "Plate"
}

case class PlateModelObject() extends LabObject {
	val `type` = "PlateModel"
}

case class SiteObject() extends LabObject {
	val `type` = "Site"
}

case class SiteModelObject() extends LabObject {
	val `type` = "SiteModel"
}