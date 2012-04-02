package roboliq.commands.pipette

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import roboliq.core._


class AspirateCmdBean extends CmdBean {
	@BeanProperty var description: String = null
	@BeanProperty var items: java.util.List[SpirateCmdItemBean] = null
}

class AspirateCmdHandler extends CmdHandlerA[AspirateCmdBean](isFinal = true) {
	def process(cmd: AspirateCmdBean, ctx: ProcessorContext, node: CmdNodeBean) {
		node.mustBeNonEmpty(cmd, "items")
		if (node.getErrorCount == 0) {
			val lItem = cmd.items.toList.map(_.toTokenItem(ctx.ob, node)).flatten
			if (node.getErrorCount == 0) {
				node.tokens = List(new AspirateToken(lItem))
			}
		}
	}
}

class SpirateCmdItemBean {
	@BeanProperty var tip: String = null
	@BeanProperty var well: String = null
	@BeanProperty var volume: java.math.BigDecimal = null
	@BeanProperty var policy: String = null
	
	
	def toTokenItem(ob: ObjBase, node: CmdNodeBean): Option[SpirateTokenItem] = {
		for {
			_ <- node.checkPropertyNonNull_?(this, "tip", "well", "volume", "policy")
			wellObj <- ob.findWell_?(well, node)
		} yield {
			val iTip = tip.drop(3).toInt
			new SpirateTokenItem(iTip, wellObj, LiquidVolume.l(volume), policy)
		}
	}
}

case class AspirateToken(
	val items: List[SpirateTokenItem]
) extends CmdToken

case class SpirateTokenItem(
	val tip: java.lang.Integer,
	val well: Well,
	val volume: LiquidVolume,
	val policy: String
)

class YamlTest {
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
		
		val node = handler.process(cmd, ctx)
		println(node)
	}
}

object Main extends App {
	new YamlTest().run
}
