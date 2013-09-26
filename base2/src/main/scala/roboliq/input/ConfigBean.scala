package roboliq.input

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import scala.collection.mutable.ListBuffer
import scala.beans.BeanProperty

class ConfigBean {
	@BeanProperty var evowareAgents: java.util.HashMap[String, EvowareAgentBean] = null
	@BeanProperty var aliases: java.util.HashMap[String, String] = null
	@BeanProperty var logic: java.util.ArrayList[String] = null
	@BeanProperty var specs: java.util.HashMap[String, String] = null
	@BeanProperty var deviceToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var deviceToModelToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var externalThermocyclers: java.util.ArrayList[String] = null
}

class EvowareAgentBean {
	/** Database ID */
	@BeanProperty var key: String = null
	/** Protocol ID */
	@BeanProperty var name: String = null
	/** Jshop ID */
	@BeanProperty var ident: String = null
	/** Evoware data directory */
	@BeanProperty var evowareDir: String = null
	/** Path to table file */
	@BeanProperty var tableFile: String = null
	/** Labware that this robot can use */
	@BeanProperty var labware: java.util.ArrayList[String] = null
	/** Tip models that this robot can use */
	@BeanProperty var tipModels: java.util.HashMap[String, TipModelBean] = null
	/** This robot's tips */
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