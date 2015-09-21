package roboliq.evoware.translator

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import roboliq.core._
import java.io.FileInputStream
import java.io.File

/**
 * Hold names of grid sites for use in scripts.
 */
case class EvowareConfigData(
	siteIds: Map[String, String]
)

private class EvowareConfigYaml {
	@BeanProperty var siteId: java.util.Map[String, String] = null
}

object EvowareConfigData {
	def loadFile(filename: String): RqResult[EvowareConfigData] = {
		try {
			val fis = new FileInputStream(new File(filename))
			val yaml = new Yaml(new Constructor(classOf[EvowareConfigYaml]))
			val data0 = yaml.load(fis).asInstanceOf[EvowareConfigYaml]
			val config = EvowareConfigData(data0.siteId.toMap)
			RqSuccess(config)
		}
		catch {
			case ex: Throwable => RqError(ex.getMessage)
		}
	}
}
