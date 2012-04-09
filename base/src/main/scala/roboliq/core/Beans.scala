package roboliq.core

import scala.collection.mutable.ListBuffer
import scala.reflect.BeanProperty
import scala.collection.JavaConversions
import org.yaml.snakeyaml.Yaml


class CmdToken

class CmdBean {
	override def toString: String = CmdBean.yaml.dump(this)
}

object CmdBean {
	val yaml = new Yaml
}

/** YAML bean representing an entire roboliq YAML file */
class RoboliqYamlBean {
	// Security-sensitive setup of classes used
	@BeanProperty var devices: java.util.List[DeviceBean] = null
	@BeanProperty var commandHandlers: java.util.List[CmdHandler] = null
	
	// Configuration of robot and systems
	@BeanProperty var systemProperties: java.util.LinkedHashMap[String, Object] = null
	@BeanProperty var tipModels: java.util.LinkedHashMap[String, TipModelBean] = null
	@BeanProperty var plateModels: java.util.LinkedHashMap[String, PlateModelBean] = null
	@BeanProperty var tubeModels: java.util.LinkedHashMap[String, TubeModelBean] = null
	@BeanProperty var tips: java.util.LinkedHashMap[String, TipBean] = null
	@BeanProperty var locations: java.util.LinkedHashMap[String, PlateLocationBean] = null
	@BeanProperty var tubeLocations: java.util.LinkedHashMap[String, TubeLocationBean] = null
	
	// Database
	@BeanProperty var substances: java.util.LinkedHashMap[String, SubstanceBean] = null
	@BeanProperty var plates: java.util.LinkedHashMap[String, PlateBean] = null
	@BeanProperty var tubes: java.util.LinkedHashMap[String, PlateBean] = null
	@BeanProperty var events: java.util.LinkedHashMap[String, java.util.List[EventBean]] = null
	
	// Protocol
	@BeanProperty var commands: java.util.List[CmdBean] = null
}

/**
 * Represents a change to the state of a well.
 */
sealed trait HistoryItem extends Bean

/** Represents the addition of a substance to a well's history in YAML */
class HistoryAddBean extends HistoryItem {
	/** ID of the added substance */
	@BeanProperty var substance: String = null
	/** Amount added in moles [mol] */
	@BeanProperty var mol: java.math.BigDecimal = null
	/** Amount added in liters [l] */
	@BeanProperty var liter: java.math.BigDecimal = null
}

/** Factory for [[roboliq.yaml.HistoryItemAddBean]] instances. */
object HistoryAddBean {
	/** Create a HistoryItemAddBean */
	def apply(
		substance: String, 
		mol: java.math.BigDecimal = null, 
		liter: java.math.BigDecimal = null
	): HistoryAddBean = {
		val bean = new HistoryAddBean
		bean.substance = substance
		bean.mol = mol
		bean.liter = liter
		bean
	}
}
