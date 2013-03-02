package roboliq.yaml

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.constructor.Constructor
import scala.collection.mutable.ListBuffer
import scala.reflect.BeanProperty
import scala.collection.JavaConversions._
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.TypeDescription
import roboliq.core._
import roboliq.commands.pipette._
import org.yaml.snakeyaml.introspector.PropertyUtils
import org.yaml.snakeyaml.introspector.BeanAccess


class RoboliqRepresenter extends Representer {
	//addClassTag(classOf[HistoryBean], new Tag("!history"))
	addClassTag(classOf[SubstanceDnaBean], new Tag("!dna"))
	addClassTag(classOf[SubstanceLiquidBean], new Tag("!liquid"))
	addClassTag(classOf[HistoryAddBean], new Tag("!add"))
	
	setPropertyUtils(new RoboliqPropertyUtils)

	// Don't output null values
	protected override def representJavaBeanProperty(
		javaBean: Object,
		property: Property,
		propertyValue: Object,
		customTag: Tag
	): NodeTuple = {
		if (propertyValue == null) {
			return null;
		} else {
			return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag)
		}
	}
}

class RoboliqPropertyUtils extends PropertyUtils {
	private val l = List("id", "index", "doc", "command", "events")
	protected override def createPropertySet(clazz: Class[_], access: BeanAccess): java.util.Set[Property] = {
		val properties = new java.util.LinkedHashSet[Property]
		val p0 = super.createPropertySet(clazz, access)
		for (s <- l) {
			p0.find(_.getName == s) match {
				case Some(p) =>
					properties.add(p)
				case _ =>
			}
		}
		properties.addAll(p0)
		properties
	}
}

class RoboliqConstructor extends Constructor {
	val topDescription = new TypeDescription(classOf[RoboliqYamlBean])
	topDescription.putMapPropertyType("tipModels", classOf[String], classOf[TipModelBean])
	topDescription.putMapPropertyType("tips", classOf[String], classOf[TipBean])
	topDescription.putMapPropertyType("plateModels", classOf[String], classOf[PlateModelBean])
	topDescription.putMapPropertyType("tubeModels", classOf[String], classOf[TubeModelBean])
	topDescription.putMapPropertyType("plates", classOf[String], classOf[PlateBean])
	topDescription.putMapPropertyType("tubes", classOf[String], classOf[PlateBean])
	topDescription.putMapPropertyType("substances", classOf[String], classOf[SubstanceBean])
	topDescription.putMapPropertyType("locations", classOf[String], classOf[PlateLocationBean])
	topDescription.putMapPropertyType("tubeLocations", classOf[String], classOf[TubeLocationBean])
	topDescription.putListPropertyType("events", classOf[EventBean])
	//topDescription.putListPropertyType("commandHandlers", classOf[CmdHandler])
	topDescription.putListPropertyType("commands", classOf[CmdBean])
	addTypeDescription(topDescription)
	
	val pcr = new TypeDescription(classOf[roboliq.commands.pcr.PcrCmdBean])
	pcr.putListPropertyType("products", classOf[roboliq.commands.pcr.PcrProductBean])
	addTypeDescription(pcr)
	
	addTypeDescription(new TypeDescription(classOf[SubstanceDnaBean], "!dna"))
	addTypeDescription(new TypeDescription(classOf[SubstanceOtherBean], "!substance"))
	addTypeDescription(new TypeDescription(classOf[SubstanceLiquidBean], "!liquid"))
	addTypeDescription(new TypeDescription(classOf[WellAddEventBean], "!add"))
}

object RoboliqYaml {
	val representer = new RoboliqRepresenter
	val constructor = new RoboliqConstructor
	val yamlOut = new Yaml(representer, new DumperOptions)
	val yamlIn = new Yaml(constructor)
	
	def loadFile(filename: String): RoboliqYamlBean = {
		val s0 = scala.io.Source.fromFile(filename).mkString
		val o0 = yamlIn.loadAs(s0, classOf[RoboliqYamlBean])
		o0
	}
	
	def toString(bean: RoboliqYamlBean): String = {
		yamlOut.dump(bean)
	}
}
