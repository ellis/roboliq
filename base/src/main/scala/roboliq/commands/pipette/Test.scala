package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._

class YamlTest1 {
	import org.yaml.snakeyaml._
	
	roboliq.yaml.RoboliqYaml.constructor.addTypeDescription(new TypeDescription(classOf[AspirateCmdBean], "!_Aspirate"))
	val bean = roboliq.yaml.RoboliqYaml.loadFile("example1b.yaml")
	val text = roboliq.yaml.RoboliqYaml.toString(bean)
	
	def run {
		println(text)
		
		val bb = new BeanBase
		bb.addBean(bean)
		val ob = new ObjBase(bb)
		
		val processor = new Processor
		val builder = new StateBuilder(ob)
		val ctx = new ProcessorContext(processor, ob, Some(builder), builder.toImmutable)
		
		val handler = new AspirateCmdHandler

		val cmd = bean.commands.get(0).asInstanceOf[AspirateCmdBean]
		
		val node = handler.handle(cmd, ctx)
		println(roboliq.yaml.RoboliqYaml.yamlOut.dump(node))
	}
}

/*class YamlTest2 {
	import org.yaml.snakeyaml._
	
	roboliq.yaml.RoboliqYaml.constructor.addTypeDescription(new TypeDescription(classOf[AspirateCmdBean], "!_Aspirate"))
	val bean = roboliq.yaml.RoboliqYaml.loadFile("example2b.yaml")
	val text = roboliq.yaml.RoboliqYaml.toString(bean)
	
	def run {
		println(text)
		
		val bb = new BeanBase
		bb.addBean(bean)
		val ob = new ObjBase(bb)
		
		val processor = new Processor
		val builder = new StateBuilder(ob)
		val ctx = new ProcessorContext(processor, ob, Some(builder), builder.toImmutable)
		
		val handler = new PipetteCmdHandler

		val cmd = bean.commands.get(0).asInstanceOf[PipetteCmdBean]
		
		val node = handler.process(cmd, ctx)
		println(roboliq.yaml.RoboliqYaml.yamlOut.dump(node))
	}
}*/

object Test extends App {
	new YamlTest1().run
}