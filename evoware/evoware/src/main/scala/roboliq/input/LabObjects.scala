package roboliq.input

import scala.language.dynamics


trait LabObject /*extends Dynamic*/ {
	val `type`: String
}

case class PlateObject() extends LabObject {
	val `type` = "Plate"
}

class PlateModelObject extends LabObject {
	val `type` = "PlateModel"
}

class SiteObject extends LabObject {
	val `type` = "Site"
}

class SiteModelObject extends LabObject {
	val `type` = "SiteModel"
}
