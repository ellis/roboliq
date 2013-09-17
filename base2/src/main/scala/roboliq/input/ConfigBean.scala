package roboliq.input

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import scala.collection.mutable.ListBuffer
import scala.beans.BeanProperty

class ConfigBean {
	@BeanProperty var aliases: java.util.HashMap[String, String] = null
	@BeanProperty var logic: java.util.ArrayList[String] = null
	@BeanProperty var specs: java.util.HashMap[String, String] = null
	@BeanProperty var deviceToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var deviceToModelToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var labware: java.util.ArrayList[String] = null
	@BeanProperty var tipModels: java.util.HashMap[String, TipModelBean] = null
	@BeanProperty var tips: java.util.ArrayList[TipBean] = null
}

class TipModelBean {
	//@BeanProperty var name: String = null
	@BeanProperty var min: java.lang.Double = null
	@BeanProperty var max: java.lang.Double = null
}

class TipBean {
	@BeanProperty var row: Integer = null
	@BeanProperty var permanentModel: String = null
	@BeanProperty var models: java.util.ArrayList[String] = null
}