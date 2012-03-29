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


class RoboliqRepresenter extends Representer {
	//addClassTag(classOf[HistoryBean], new Tag("!history"));
	addClassTag(classOf[SubstanceItemDnaBean], new Tag("!dna"));
	addClassTag(classOf[HistoryItemAddBean], new Tag("!add"));

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
	//addTypeDescription(new TypeDescription(classOf[HistoryBean], "!history"))
	addTypeDescription(new TypeDescription(classOf[SubstanceItemDnaBean], "!dna"))
	addTypeDescription(new TypeDescription(classOf[HistoryItemAddBean], "!add"))
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
