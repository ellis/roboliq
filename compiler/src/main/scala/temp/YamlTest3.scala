package temp

//import java.util.BigInteger

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

class WellHistory2(
	@BeanProperty var addr: String,
	@BeanProperty var history: java.util.List[HistoryItem]
)

trait HistoryItem
/*class HistoryItem(
	@BeanProperty var action: String,
	@BeanProperty var substance: String
) {
	
}*/

class HistoryItemAddBean extends HistoryItem {
	@BeanProperty var substance: String = null
	@BeanProperty var mol: java.math.BigDecimal = null
	@BeanProperty var liter: java.math.BigDecimal = null
}

object HistoryItemAddBean {
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

class MyRepresenter extends Representer {
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

class MyConstructor extends Constructor {
	addTypeDescription(new TypeDescription(classOf[HistoryItemAddBean], "!add"))
}

class YamlTest3 {
	val representer = new MyRepresenter();
	val yaml = new Yaml(representer, new DumperOptions)
	
	val l = JavaConversions.seqAsJavaList[HistoryItem](Seq(
		HistoryItemAddBean("FRP128", mol = new java.math.BigDecimal(51e-9)),
		HistoryItemAddBean("FRO1259", mol = new java.math.BigDecimal(52e-9)),
		HistoryItemAddBean("FRO1360", mol = new java.math.BigDecimal(53e-9))
	))
	
	val wellHistories = new java.util.LinkedHashMap[String, java.util.List[HistoryItem]]
	wellHistories.put("E2215(A01)", l)
	val wellHistory = new WellHistory2("E2215(A01)", l)
	
	def run {
		println(yaml.dump(wellHistories))
	}
}

class YamlTest4 {
	val representer = new MyRepresenter
	val constructor = new MyConstructor
	val yamlOut = new Yaml(representer, new DumperOptions)
	val yamlIn = new Yaml(constructor)

	val s0 = scala.io.Source.fromFile("database3.yaml").mkString
	val o0 = yamlIn.loadAs(s0, classOf[java.util.LinkedHashMap[String, java.util.List[HistoryItem]]])
	
	def run {
		println(yamlOut.dump(o0))
	}
}