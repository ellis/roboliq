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

import roboliq.core._
import roboliq.commands.pipette._


class RoboliqRepresenter extends Representer {
	//addClassTag(classOf[HistoryBean], new Tag("!history"));
	addClassTag(classOf[SubstanceItemDnaBean], new Tag("!dna"));
	addClassTag(classOf[HistoryAddBean], new Tag("!add"));

	protected override def representJavaBeanProperty(
		javaBean: Object,
		property: Property,
		propertyValue: Object,
		customTag: Tag
	): NodeTuple = {
		if (propertyValue == null) {
			return null;
		} else {
			return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
		}
	}
}

class RoboliqConstructor extends Constructor {
	val topDescription = new TypeDescription(classOf[RoboliqYamlBean])
	topDescription.putMapPropertyType("plateModels", classOf[String], classOf[PlateModelBean])
	topDescription.putMapPropertyType("plates", classOf[String], classOf[PlateBean])
	topDescription.putMapPropertyType("substances", classOf[String], classOf[SubstanceItem])
	topDescription.putMapPropertyType("history", classOf[String], classOf[java.util.List[HistoryItem]])
	topDescription.putListPropertyType("commands", classOf[CmdBean])
	addTypeDescription(topDescription)
	
	//val aspirateDescription = new TypeDescription(classOf[AspirateCmdBean])
	
	//addTypeDescription(aspirateDescription)
	addTypeDescription(new TypeDescription(classOf[SubstanceItemDnaBean], "!dna"))
	addTypeDescription(new TypeDescription(classOf[HistoryAddBean], "!add"))
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
