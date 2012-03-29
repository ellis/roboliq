package roboliq.yaml

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.constructor.Constructor
import scala.collection.mutable.ListBuffer
import scala.reflect.BeanProperty
import scala.collection.JavaConversions
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.TypeDescription


/** YAML bean representing an entire roboliq YAML file */
class RoboliqYamlBean {
	@BeanProperty var plates: java.util.LinkedHashMap[String, PlateBean] = null
	@BeanProperty var substances: java.util.LinkedHashMap[String, java.util.List[SubstanceItem]] = null
	@BeanProperty var history: java.util.LinkedHashMap[String, java.util.List[HistoryItem]] = null
}

/** Represents a plate in YAML */
class PlateBean {
	@BeanProperty var model: String = null
	@BeanProperty var description: String = null
	@BeanProperty var barcode: String = null
}

sealed trait SubstanceItem

/** Represents a DNA-based substance in YAML */
class SubstanceItemDnaBean extends SubstanceItem {
	@BeanProperty var sequence: String = null
}

/*class HistoryBean {
	@BeanProperty var history: java.util.LinkedHashMap[String, java.util.List[HistoryItem]] = null
}*/

/**
 * Represents a change to the state of a well.
 */
sealed trait HistoryItem

/** Represents the addition of a substance to a well's history in YAML */
class HistoryItemAddBean extends HistoryItem {
	/** ID of the added substance */
	@BeanProperty var substance: String = null
	/** Amount added in moles [mol] */
	@BeanProperty var mol: java.math.BigDecimal = null
	/** Amount added in liters [l] */
	@BeanProperty var liter: java.math.BigDecimal = null
}

/** Factory for [[roboliq.yaml.HistoryItemAddBean]] instances. */
object HistoryItemAddBean {
	/** Create a HistoryItemAddBean */
	def apply(
		substance: String,
		mol: java.math.BigDecimal = null,
		liter: java.math.BigDecimal = null
	): HistoryItemAddBean = {
		val bean = new HistoryItemAddBean
		bean.substance = substance
		bean.mol = mol
		bean.liter = liter
		bean
	}
}
